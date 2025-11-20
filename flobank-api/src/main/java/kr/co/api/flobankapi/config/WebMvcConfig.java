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

        registry.addResourceHandler("/pdf_ai/**")
                .addResourceLocations("file:/app/uploads/pdf_ai/");



        // 약관 PDF
        String termsPath = filePathConfig.getPdfTermsPath();

        String resourceLocation = Paths.get(termsPath).toUri().toString();

        registry.addResourceHandler("/uploads/terms/**")
                .addResourceLocations(resourceLocation);



        // 상품설명서 pdf
        String productPath = filePathConfig.getPdfProductsPath(); // /app/uploads/pdf_products
        String productLocation = Paths.get(productPath).toUri().toString();

        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations(productLocation);
    }


    }


