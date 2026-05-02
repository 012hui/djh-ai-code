package org.test.djhaicodeuser;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDubbo
@MapperScan("org.test.djhaicodeuser.mapper")
@ComponentScan("org.test")
public class DjhAiCodeUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(DjhAiCodeUserApplication.class, args);
    }
}

