package com.assessment.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class FraudNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudNotificationApplication.class, args);
    }
}
