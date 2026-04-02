package com.example.back.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseMigrationConfig {

    @Bean
    public CommandLineRunner migrateDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Manually add status column if it doesn't exist (Safe for PostgreSQL)
                jdbcTemplate.execute("ALTER TABLE support_queries ADD COLUMN IF NOT EXISTS status VARCHAR(255) DEFAULT 'PENDING'");
                System.out.println(">>> Database migration: Added status column to support_queries");
            } catch (Exception e) {
                System.out.println(">>> Database migration skip: " + e.getMessage());
            }
        };
    }
}
