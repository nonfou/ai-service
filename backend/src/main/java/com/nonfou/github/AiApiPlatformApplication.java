package com.nonfou.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI API Platform 主类
 */
@SpringBootApplication
@MapperScan("com.nonfou.github.mapper")
@EnableScheduling
public class AiApiPlatformApplication {

    private static final Logger log = LoggerFactory.getLogger(AiApiPlatformApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AiApiPlatformApplication.class, args);
        log.info("========================================");
        log.info("AI API Platform 启动成功！");
        log.info("访问地址: http://localhost:8080");
        log.info("========================================");
    }
}
