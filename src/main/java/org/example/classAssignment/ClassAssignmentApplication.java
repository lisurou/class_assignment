package org.example.classAssignment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.example.classAssignment.mapper")
public class ClassAssignmentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClassAssignmentApplication.class, args);
    }

}
