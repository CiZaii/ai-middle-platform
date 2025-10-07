package com.ai.middle.platform;

import com.dtflys.forest.springboot.annotation.ForestScan;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication
@MapperScan("com.ai.middle.platform.repository.mapper")
@EnableNeo4jRepositories(basePackages = "com.ai.middle.platform.repository.neo4j")
@ForestScan(basePackages = "com.ai.middle.platform.client")
public class MiddlePlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiddlePlatformApplication.class, args);
    }
}
