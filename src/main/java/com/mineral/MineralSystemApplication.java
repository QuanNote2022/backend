package com.mineral;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mineral.mapper")
public class MineralSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(MineralSystemApplication.class, args);
    }
}
