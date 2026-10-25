package com.metertocash.field;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(FieldApplication.class, args);
    }
}
