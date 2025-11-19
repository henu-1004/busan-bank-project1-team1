package kr.co.api.flobankapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 📌 /pdf_ai/** 로 들어오는 URL을
        //     /app/uploads/pdf_ai/ 폴더와 매핑
        registry.addResourceHandler("/pdf_ai/**")
                .addResourceLocations("file:/app/uploads/pdf_ai/");
    }
}
