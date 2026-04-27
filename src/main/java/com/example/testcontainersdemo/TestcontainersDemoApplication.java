package com.example.testcontainersdemo;

import com.example.testcontainersdemo.config.TestcontainersInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestcontainersDemoApplication {
    
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(TestcontainersDemoApplication.class);
        application.addInitializers(new TestcontainersInitializer());
        application.run(args);
    }
}
