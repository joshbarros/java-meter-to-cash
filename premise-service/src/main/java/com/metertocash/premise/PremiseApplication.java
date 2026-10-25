package com.metertocash.premise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PremiseApplication {

    public static void main(String[] args) {
        SpringApplication.run(PremiseApplication.class, args);
    }
}
