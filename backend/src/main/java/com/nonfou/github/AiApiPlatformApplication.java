package com.nonfou.github;

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

    public static void main(String[] args) {
        SpringApplication.run(AiApiPlatformApplication.class, args);
        System.out.println("========================================");
        System.out.println("AI API Platform 启动成功！");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("========================================");
    }
}
