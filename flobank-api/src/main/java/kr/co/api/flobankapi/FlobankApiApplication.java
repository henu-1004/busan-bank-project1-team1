package kr.co.api.flobankapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FlobankApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlobankApiApplication.class, args);
    }
}
