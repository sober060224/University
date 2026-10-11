package com.gdou.marinebio.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.gdou.marinebio.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 模块五：阿里云百炼 DashScope 调用客户端。
 *
 * 走百炼的 OpenAI 兼容端点（/chat/completions），所以只用 Spring 自带的 RestClient，
 * 不引入官方 SDK。API Key 从 application.yml 读取，只存在于服务端，绝不下发到前端。
 *
 * 按非功能性需求「调用大模型服务需具备超时与重试机制」实现了：
 *   - 连接超时由 HttpClient 控制，读取超时由 JdkClientHttpRequestFactory 控制
 *   - 网络异常与 5xx 最多重试 maxRetry 次，退避 1 秒递增，且总耗时不超过 retryDeadline-ms
 *     （retryDeadline-ms 必须明显小于前端 request.js 的 60 秒 axios 超时）
 *   - 4xx（含鉴权失败、参数错误）不重试，直接抛出，避免无意义地打接口
 */
@Slf4j
@Component
public class DashScopeClient {

    /** 识别用的图片一律来自本系统的上传目录，这个前缀同时也是文件访问的 jail */
    private static final String UPLOAD_PREFIX = "/uploads/";

    private final RestClient restClient;
    private final String apiKey;
    private final String visionModel;
    private final String textModel;
    private final int maxRetry;
    private final int readTimeoutMs;
    private final int retryDeadlineMs;
    private final Path uploadDir;

    public DashScopeClient(@Value("${ai.api-key}") String apiKey,
                           @Value("${ai.base-url}") String baseUrl,
                           @Value("${ai.vision-model}") String visionModel,
                           @Value("${ai.text-model}") String textModel,
                           @Value("${ai.connect-timeout-ms:5000}") int connectTimeoutMs,
                           @Value("${ai.read-timeout-ms:30000}") int readTimeoutMs,
                           @Value("${ai.max-retry:2}") int maxRetry,
                           @Value("${ai.retry-deadline-ms:50000}") int retryDeadlineMs,
                           @Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.apiKey = apiKey;
        this.visionModel = visionModel;
        this.textModel = textModel;
        this.maxRetry = maxRetry;
        this.readTimeoutMs = readTimeoutMs;
        this.retryDeadlineMs = retryDeadlineMs;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();

        /*
         * 用 JdkClientHttpRequestFactory 而不是 SimpleClientHttpRequestFactory，因为后者读不到
         * 4xx 的响应正文。实测：同一个 401，裸 HttpURLConnection 能取到 241 字节的
         * {"error":{"message":"Invalid API-key provided…"}}，而 Simple 工厂在 onStatus 里
         * 和走默认异常路径时都只拿到 0 字节（bufferRequestBody 开或关都一样）。
         * 结果是所有鉴权/参数错误都被压缩成一句没有原因的「大模型接口返回 401：」，
         * 前端和用户看不出是 Key 错了、过期了还是模型名写错了，ai_records 里也只剩半句话。
         * JdkClientHttpRequestFactory 同属 spring-web，不引入新依赖，错误正文完整可读。
         */
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey) && !apiKey.startsWith("sk-请填写");
    }

    public String visionModel() {
        return visionModel;
    }

    public String textModel() {
        return textModel;
    }

    /** 纯文本对话，返回助手回复正文。 */
    public String chatText(String systemPrompt, String userPrompt) {
        return complete(textModel, List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)));
    }

    /**
     * 图像物种识别。imageUrl 既可以是公网地址，也可以是本系统上传后的相对路径
     * （如 /uploads/2026/xxx.jpg），后者会读成本地文件并转成 base64 data URI 发给模型。
     */
    public String chatVision(String userPrompt, String imageUrl) {
        return complete(visionModel, List.of(Map.of("role", "user", "content", List.of(
                Map.of("type", "text", "text", userPrompt),
                Map.of("type", "image_url", "image_url", Map.of("url", toModelImageUrl(imageUrl)))))));
    }

    private String complete(String model, List<Map<String, Object>> messages) {
        if (!isConfigured()) {
            throw new BizException("未配置大模型 API Key，请在 backend/src/main/resources/application.yml 的 ai.api-key 填写阿里云百炼的 Key");
        }
        RuntimeException last = null;
        long start = System.currentTimeMillis();
        for (int attempt = 0; attempt <= maxRetry; attempt++) {
            try {
                JsonNode response = restClient.post()
                        .uri("/chat/completions")
                        .body(Map.of("model", model, "messages", messages))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, res) -> {
                            byte[] body;
                            try (var in = res.getBody()) {
                                body = in.readAllBytes();
                            }
                            throw new BizException("大模型接口返回 " + res.getStatusCode().value() + "："
                                    + new String(body, StandardCharsets.UTF_8));
                        })
                        .body(JsonNode.class);
                return response.path("choices").path(0).path("message").path("content").asText("");
            } catch (ResourceAccessException e) {
                // 超时或网络抖动 —— 这正是需要重试的情况
                last = new BizException("调用大模型超时或网络异常：" + e.getMessage());
                log.warn("第 {} 次调用大模型失败，准备重试：{}", attempt + 1, e.getMessage());
                // 但只有「再来一次也还来得及」才重试。不看已花时间的重试会把用户拖死：
                // 实测一次「文本辅助分类」第一次读超时 30 秒、重试又跑 25.7 秒才成功，
                // 合计 56.7 秒，而前端 request.js 的 axios timeout 是 60 秒 —— 浏览器几乎
                // 同时放弃，用户看到「请求超时」，后端却替他把这次调用的钱花完、还写了一条
                // success=true 的日志。这里用总时限把「慢」和「网络断了」区分开：
                // 连不上（connect timeout 几秒）照旧重试，生成太慢就不再做第二次同样的等待。
                long elapsed = System.currentTimeMillis() - start;
                if (attempt >= maxRetry || elapsed + readTimeoutMs > retryDeadlineMs) {
                    break;
                }
                sleepBeforeRetry(attempt);
            } catch (RuntimeException e) {
                // 4xx / 解析失败属于确定性错误，重试没有意义，直接抛出
                throw e;
            }
        }
        throw last == null ? new BizException("调用大模型失败") : last;
    }

    private void sleepBeforeRetry(int attempt) {
        if (attempt >= maxRetry) {
            return;
        }
        try {
            Thread.sleep(1000L * (attempt + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 与 spring.servlet.multipart.max-file-size 一致，上传目录里不可能有更合法的大文件 */
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    /**
     * 把 imageUrl 转成模型能取到的形式。
     *
     * <p>只接受本系统上传目录里的相对路径（/uploads/yyyy/MM/xxx.jpg），原因有两个：
     *
     * <p>一是报告 7.3 写明的设计前提——「用户上传的图片仅存在于本机磁盘、不具备公网可达
     * 地址」，所以前端识别流程传过来的必然是以 /uploads/ 开头的路径，公网地址这条通路
     * 从来没有合法用途。留着它等于允许任何登录用户把任意 URL 交给百炼去抓取，
     * 包括指向内网地址的 URL：被请求的是阿里云那侧，日志和错误信息却回到我们这里，
     * 成了一条可以拿别人的机器探路的转发通道。
     *
     * <p>二是本地分支必须限制读取大小：不限制的话，往上传目录里塞一个几百 MB 的文件
     * 再让它被读成 Base64，光这一次请求就能把堆撑爆（Base64 还要再膨胀 4/3 倍）。
     */
    private String toModelImageUrl(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(UPLOAD_PREFIX)) {
            throw new BizException("请通过本页面的上传功能选择图片后再识别");
        }
        // 只允许读取上传目录内的文件，避免用户传 ../ 读到项目其它文件
        String relative = imageUrl.substring(UPLOAD_PREFIX.length());
        Path file = uploadDir.resolve(relative).normalize();
        if (!file.startsWith(uploadDir) || !Files.isRegularFile(file)) {
            throw new BizException("图片不存在或路径不合法");
        }
        long size;
        try {
            size = Files.size(file);
        } catch (IOException e) {
            throw new BizException("图片不存在或路径不合法");
        }
        if (size > MAX_IMAGE_BYTES) {
            throw new BizException("图片过大，请压缩到 5MB 以内后再识别");
        }
        try {
            String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(file));
            String mime = Files.probeContentType(file);
            return "data:" + (mime == null ? "image/jpeg" : mime) + ";base64," + base64;
        } catch (Exception e) {
            throw new BizException("读取图片失败：" + e.getMessage());
        }
    }
}
