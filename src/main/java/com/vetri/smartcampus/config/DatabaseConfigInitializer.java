package com.vetri.smartcampus.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfigInitializer {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPass;

    @Value("${spring.datasource.driver-class-name}")
    private String dbDriver;

    @PostConstruct
    public void init() {
        System.setProperty("smartcampus.db.url", dbUrl);
        System.setProperty("smartcampus.db.user", dbUser);
        System.setProperty("smartcampus.db.password", dbPass);
        System.setProperty("smartcampus.db.driver", dbDriver);
    }
}
