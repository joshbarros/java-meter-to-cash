package com.metertocash.adjustment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AdjustmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdjustmentApplication.class, args);
    }
}
