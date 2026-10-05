package com.gdou.marinebio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * 静态资源与单页应用路由。
 *
 * 开发时前端由 Vite 提供（Vite 已把 /api 代理到本服务），构建后把 dist 拷进
 * resources/static 即可由本服务一并提供，前后端同源，因此不需要配置 CORS。
 * 上传目录单独映射：/uploads/** 的 URL 前缀要去掉，剩下的部分才对得上磁盘目录，
 * 所以不能和 /** 的 classpath:/static/ 放进同一个 location 列表
 * （那样 "uploads/xxx" 会被解析成 &lt;uploadDir&gt;/uploads/xxx，永远找不到）。
 * 静态资源与图片刻意不要求登录：物种图片是公开信息的一部分，权限矩阵里「浏览公开物种
 * 信息」对公众开放，公开的物种详情页也要能显示图片，因此图片按可公开资源对待。
 * UploadController 已用扩展名白名单加文件头魔数双重校验，杜绝非图片文件被改名上传。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadDir;

    public WebConfig(@Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /uploads/2026/09/xxx.jpg → <uploadDir>/2026/09/xxx.jpg
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        // checkResource 校验资源确实位于 location 之下；不调用它，
                        // 带 ../ 的路径就能读到 classpath 里别的文件（例如 application.yml）
                        if (requested.exists() && requested.isReadable() && checkResource(requested, location)) {
                            return requested;
                        }
                        // 接口路径写错时要如实 404，不能拿 SPA 首页冒充成功响应
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("/api/")) {
                            return null;
                        }
                        // 前端是单页应用，未知路径一律交回 index.html 由 vue-router 处理
                        ClassPathResource index = new ClassPathResource("static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
