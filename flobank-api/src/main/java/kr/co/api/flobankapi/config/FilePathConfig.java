package kr.co.api.flobankapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "")
@Data
public class FilePathConfig {

    private String pdfTermsPath; // application.yml 의 key와 동일 (pdf-terms-path)
}
