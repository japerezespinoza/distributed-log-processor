package com.example.logprocessor.coordinator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableScheduling
public class CoordinatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoordinatorApplication.class, args);
    }

    @RestController
    static class StatusController {
        @GetMapping("/status")
        String status() { return "coordinator-up"; }
    }
}
