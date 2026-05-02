package org.test.djhaicodeuser;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("org.test.djhaicodeuser.mapper")
@ComponentScan("org.test")
public class DjhAiCodeUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(DjhAiCodeUserApplication.class, args);
    }
}

