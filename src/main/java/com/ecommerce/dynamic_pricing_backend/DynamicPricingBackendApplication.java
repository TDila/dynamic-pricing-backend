package com.ecommerce.dynamic_pricing_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableCaching
@EnableRedisHttpSession
@EnableTransactionManagement
public class DynamicPricingBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DynamicPricingBackendApplication.class, args);

	}

}
