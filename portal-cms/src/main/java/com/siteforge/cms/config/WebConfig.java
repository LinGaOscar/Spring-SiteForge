package com.siteforge.cms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${storage.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 將 /uploads/** 對映至本機 uploadDir 目錄，讓前端可直接存取已上傳圖片
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + uploadDir + "/");
    }
}
