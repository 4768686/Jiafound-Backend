package org.example.campususer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.example.campususer.mapper")
public class CampusUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusUserApplication.class, args);
    }

}
