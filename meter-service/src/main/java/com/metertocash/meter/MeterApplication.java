package com.metertocash.meter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeterApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeterApplication.class, args);
    }
}
