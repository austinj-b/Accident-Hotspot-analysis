package com.example.hotspot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HotspotApplication {
    public static void main(String[] args) {
        SpringApplication.run(HotspotApplication.class, args);
    }
}
