package com.retrobolsa.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer{

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Apply to all paths
                .allowedOrigins(
                    "http://localhost:3000",  // React CRA / padrão
                    "http://localhost:5173",  // Vite dev server
                    "http://localhost:19006", // Expo Web (older)
                    "http://localhost:8081",  // Expo Web
                    "http://localhost:8082"   // Expo Web alternative port
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*") // Allow all headers
                .allowCredentials(true); // Allow cookies/auth headers
    }
}
