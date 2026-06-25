package com.epms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.epms.config.CloudinaryProperties;

@EnableJpaAuditing
@SpringBootApplication
@EnableConfigurationProperties(CloudinaryProperties.class)
public class PayrollManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(PayrollManagementSystemApplication.class, args);
	}

}
