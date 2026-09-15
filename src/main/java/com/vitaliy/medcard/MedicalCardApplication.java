package com.vitaliy.medcard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedicalCardApplication {
    public static void main(String[] args) {

        SpringApplication.run(MedicalCardApplication.class, args);
    }
}
