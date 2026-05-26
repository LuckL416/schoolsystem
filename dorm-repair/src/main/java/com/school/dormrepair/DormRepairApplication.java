package com.school.dormrepair;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@EnableScheduling
@MapperScan("com.school.dormrepair.mapper")
public class DormRepairApplication {

    public static void main(String[] args) {
        SpringApplication.run(DormRepairApplication.class, args);
    }

}
