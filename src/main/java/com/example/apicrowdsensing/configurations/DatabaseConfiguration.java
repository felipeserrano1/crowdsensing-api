package com.example.apicrowdsensing.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
public class DatabaseConfiguration {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/my_database");
        dataSource.setUsername("user");
        dataSource.setPassword("password");

        try (Connection connection = dataSource.getConnection()) {
            System.out.println("Connected to the database");
        } catch (SQLException e) {
            System.err.println("Failed to connect to the database: " + e.getMessage());
            // Aquí puedes lanzar una excepción personalizada o realizar otra acción
        }

        return dataSource;
    }
}