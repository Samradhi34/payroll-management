package com.epms.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CloudinaryConfig {

    private final CloudinaryProperties properties;

    @Bean
    Cloudinary cloudinary() {

        return new Cloudinary(
        		Map.of(
        				"cloud_name", properties.getCloudName(),
        				"api_key", properties.getApiKey(),
        				"api_secret", properties.getApiSecret()
        		)
        );
    }
}