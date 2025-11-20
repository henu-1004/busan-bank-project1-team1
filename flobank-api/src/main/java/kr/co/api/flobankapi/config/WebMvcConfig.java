package kr.co.api.flobankapi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@RequiredArgsConstructor
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final FilePathConfig filePathConfig;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 📌 /pdf_ai/** 로 들어오는 URL을
        //     /app/uploads/pdf_ai/ 폴더와 매핑
        registry.addResourceHandler("/pdf_ai/**")
                .addResourceLocations("file:/app/uploads/pdf_ai/");



        // 약관 PDF
        String termsPath = filePathConfig.getPdfTermsPath();

        String resourceLocation = Paths.get(termsPath).toUri().toString();

        registry.addResourceHandler("/uploads/terms/**")
                .addResourceLocations(resourceLocation);





    }

}
