package com.devflow;

import com.devflow.config.DevFlowProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties(DevFlowProperties.class)
public class DevFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevFlowApplication.class, args);
    }
}
