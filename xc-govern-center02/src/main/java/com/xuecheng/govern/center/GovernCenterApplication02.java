package com.xuecheng.govern.center;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class GovernCenterApplication02 {
    public static void main(String[] args) {
        SpringApplication.run(GovernCenterApplication02.class, args);
    }
}
