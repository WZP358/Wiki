package com.wiki.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class WikiBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(WikiBackendApplication.class, args);
    }
}
