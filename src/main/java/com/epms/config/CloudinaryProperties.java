package com.epms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "cloudinary")
@Data
public class CloudinaryProperties {

    private String cloudName;
    private String apiKey;
    private String apiSecret;
}