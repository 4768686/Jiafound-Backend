package org.example.campusclaim;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.example.campusclaim.mapper")
public class CampusClaimApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusClaimApplication.class, args);
    }
}