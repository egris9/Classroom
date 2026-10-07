package com.Classroom_ai.Classroom.material;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/** Makes the servlet's per-file limit the same {@code upload.max-size} that {@link Materials} enforces. */
@Configuration
class MultipartLimits {

    @Bean
    MultipartConfigElement multipartConfigElement(@Value("${upload.max-size}") DataSize maxFileSize) {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(maxFileSize);
        // Every endpoint takes one file plus short text fields, so the per-file limit is the bound that matters.
        factory.setMaxRequestSize(DataSize.ofBytes(-1));
        return factory.createMultipartConfig();
    }
}
