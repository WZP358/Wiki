package com.wiki.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication(exclude = {
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class,
    ElasticsearchClientAutoConfiguration.class
})
public class WikiBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(WikiBackendApplication.class, args);
    }
}
