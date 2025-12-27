package com.cinema.cinemamanagementsystem.dao;
import com.cinema.cinemamanagementsystem.config.DatabaseConfig;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DatabaseConfig.URL);
        config.setUsername(DatabaseConfig.USER);
        config.setPassword(DatabaseConfig.PASSWORD);
        config.setDriverClassName(DatabaseConfig.DRIVER);

        config.setMaximumPoolSize(DatabaseConfig.MAX_POOL_SIZE);
        config.setMinimumIdle(DatabaseConfig.MIN_IDLE);
        config.setConnectionTimeout(DatabaseConfig.CONNECTION_TIMEOUT);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
        logger.info("Пул соединений с БД инициализирован");
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Пул соединений закрыт");
        }
    }
}