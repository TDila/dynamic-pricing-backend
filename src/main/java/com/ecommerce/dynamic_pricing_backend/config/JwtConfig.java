package com.ecommerce.dynamic_pricing_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class JwtConfig {
    private String jwtSecret;
    private int jwtExpiration;
}
