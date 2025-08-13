package org.likelionhsu.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.likelionhsu.backend.common.config.KmaApiConfig;
import org.likelionhsu.backend.common.config.NaverSearchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableConfigurationProperties({NaverSearchProperties.class, KmaApiConfig.class})
public class BackendApplication {

    public static void main(String[] args) {
        // .env 파일 로드
        Dotenv.load();
        SpringApplication.run(BackendApplication.class, args);
    }
}


