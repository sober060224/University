package com.gdou.marinebio.controller;

import com.gdou.marinebio.common.BizException;
import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.common.Result;
import com.gdou.marinebio.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传（模块二物种图片、模块五物种识别共用）。
 *
 * 落盘目录由 application.yml 的 app.upload-dir 配置，WebConfig 已把该目录映射为 /uploads/**，
 * 所以返回的相对路径可以直接给 <img src> 用。文件名用 UUID 重写，避免用户文件名里的
 * 路径分隔符、中文和特殊字符造成的问题。
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    /** 只放行常见图片格式，其它一律拒绝 */
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /**
     * 扩展名可以随手伪造，光看后缀挡不住把任意文件改名为 .png 上传。
     * 这里再比对文件头魔数，确认内容确实是图片。WebP 的 RIFF 容器魔数与 WAV/AVI 相同，
     * 因此额外要求第 12 字节是 'WEBP'；各家写法的偏移略有差异，故取 12-15 字节比较。
     */
    private static final Map<String, byte[]> MAGIC = Map.of(
            "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A},
            "gif", new byte[]{'G', 'I', 'F', '8'});

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final LogService logService;
    private final Path uploadDir;

    public UploadController(LogService logService,
                            @Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.logService = logService;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostMapping
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                              @AuthenticationPrincipal LoginUser loginUser) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的图片");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException("只支持 " + String.join("、", ALLOWED_EXT) + " 格式的图片");
        }
        checkMagic(file, ext);

        // 按日期分目录，避免 uploads 目录平铺成千上万张图
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        Path dir = uploadDir.resolve(dateDir).normalize();
        if (!dir.startsWith(uploadDir)) {
            throw new BizException("上传路径不合法");
        }
        String stored = dateDir + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(stored.substring(dateDir.length() + 1)));
        } catch (IOException e) {
            // 异常信息里带的是服务器绝对路径，不能原样回给前端
            log.warn("保存上传文件失败: {}", dir, e);
            throw new BizException("图片保存失败，请稍后重试");
        }

        String url = "/uploads/" + stored;
        logService.record(loginUser, "模块二", "上传图片", "文件", null, url);
        return Result.ok("上传成功", Map.of("url", url));
    }

    /** 读文件头若干字节与魔数比对，确认内容确实是该扩展名对应的图片 */
    private void checkMagic(MultipartFile file, String ext) {
        byte[] head = new byte[16];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.read(head);
        } catch (IOException e) {
            log.warn("读取上传文件内容失败", e);
            throw new BizException("图片内容无法读取，请重新选择文件");
        }
        if (read < 4) {
            throw new BizException("文件内容为空或已损坏");
        }
        if ("webp".equals(ext)) {
            if (read < 16 || head[0] != 'R' || head[1] != 'I' || head[2] != 'F' || head[3] != 'F'
                    || head[8] != 'W' || head[9] != 'E' || head[10] != 'B' || head[11] != 'P') {
                throw new BizException("文件内容不是有效的 WebP 图片");
            }
            return;
        }
        byte[] magic = MAGIC.get(ext);
        if (magic == null || read < magic.length) {
            throw new BizException("文件内容与扩展名不符");
        }
        for (int i = 0; i < magic.length; i++) {
            if (head[i] != magic[i]) {
                throw new BizException("文件内容与扩展名不符");
            }
        }
    }
}
