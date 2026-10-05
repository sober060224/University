package com.gdou.marinebio.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.gdou.marinebio.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

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
 *   - 连接/读取超时由 SimpleClientHttpRequestFactory 控制
 *   - 网络异常与 5xx 最多重试 maxRetry 次，退避 1 秒递增
 *   - 4xx（含鉴权失败、参数错误）不重试，直接抛出，避免无意义地打接口
 */
@Slf4j
@Component
public class DashScopeClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String visionModel;
    private final String textModel;
    private final int maxRetry;
    private final Path uploadDir;

    public DashScopeClient(@Value("${ai.api-key}") String apiKey,
                           @Value("${ai.base-url}") String baseUrl,
                           @Value("${ai.vision-model}") String visionModel,
                           @Value("${ai.text-model}") String textModel,
                           @Value("${ai.connect-timeout-ms:5000}") int connectTimeoutMs,
                           @Value("${ai.read-timeout-ms:30000}") int readTimeoutMs,
                           @Value("${ai.max-retry:2}") int maxRetry,
                           @Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.apiKey = apiKey;
        this.visionModel = visionModel;
        this.textModel = textModel;
        this.maxRetry = maxRetry;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
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
            } catch (RuntimeException e) {
                // 4xx / 解析失败属于确定性错误，重试没有意义，直接抛出
                throw e;
            }
            sleepBeforeRetry(attempt);
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

    /** 本地上传的图片转 base64 data URI，公网地址原样透传。 */
    private String toModelImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        // 只允许读取上传目录内的文件，避免用户传 ../ 读到项目其它文件
        String relative = imageUrl.startsWith("/uploads/") ? imageUrl.substring("/uploads/".length()) : imageUrl;
        Path file = uploadDir.resolve(relative).normalize();
        if (!file.startsWith(uploadDir) || !Files.isRegularFile(file)) {
            throw new BizException("图片不存在或路径不合法：" + imageUrl);
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
