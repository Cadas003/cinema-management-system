package com.cinema.cinemamanagementsystem.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class DatabaseConfig {
    private static final String PROPERTIES_FILE = "/application.properties";

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int poolSize;

    private DatabaseConfig(String jdbcUrl, String username, String password, int poolSize) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
    }

    public static DatabaseConfig load() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConfig.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
        }
        String jdbcUrl = getProperty(properties, "app.db.url", "JDBC_URL", "jdbc:mysql://localhost:3306/cinema");
        String username = getProperty(properties, "app.db.username", "JDBC_USERNAME", "cinema_user");
        String password = getProperty(properties, "app.db.password", "JDBC_PASSWORD", "cinema_password");
        int poolSize = Integer.parseInt(getProperty(properties, "app.db.poolSize", "JDBC_POOL_SIZE", "10"));
        return new DatabaseConfig(jdbcUrl, username, password, poolSize);
    }

    private static String getProperty(Properties properties, String key, String envKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return properties.getProperty(key, defaultValue);
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public int poolSize() {
        return poolSize;
    }
}
