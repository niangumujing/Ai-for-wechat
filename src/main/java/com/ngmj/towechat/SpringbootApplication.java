package com.ngmj.towechat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@MapperScan("com.ngmj.towechat.mapper")
public class SpringbootApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(SpringbootApplication.class, args);
    }
}
