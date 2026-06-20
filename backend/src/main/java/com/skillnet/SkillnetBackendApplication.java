package com.skillnet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SkillnetBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillnetBackendApplication.class, args);
    }
}
