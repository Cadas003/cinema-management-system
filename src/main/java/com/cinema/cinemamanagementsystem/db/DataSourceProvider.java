package com.cinema.cinemamanagementsystem.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public final class DataSourceProvider {
    private static final HikariDataSource DATA_SOURCE = createDataSource();

    private DataSourceProvider() {
    }

    private static HikariDataSource createDataSource() {
        DatabaseConfig config = DatabaseConfig.load();
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.jdbcUrl());
        hikariConfig.setUsername(config.username());
        hikariConfig.setPassword(config.password());
        hikariConfig.setMaximumPoolSize(config.poolSize());
        hikariConfig.setPoolName("cinema-pool");
        hikariConfig.setAutoCommit(true);
        return new HikariDataSource(hikariConfig);
    }

    public static DataSource getDataSource() {
        return DATA_SOURCE;
    }
}
