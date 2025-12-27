package com.cinema.cinemamanagementsystem.config;

public class DatabaseConfig {
    public static final String URL = "jdbc:mysql://localhost:3306/cinema_booking";
    public static final String USER = "root";
    public static final String PASSWORD = "1111";
    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    // Настройки пула соединений
    public static final int MAX_POOL_SIZE = 10;
    public static final int MIN_IDLE = 5;
    public static final int CONNECTION_TIMEOUT = 30000;

    // Константы бизнес-логики
    public static final int BOOKING_TIMEOUT_MINUTES = 30;
    public static final double BOOKING_SURCHARGE_RATE = 0.15;
    public static final double GUEST_PRICE_COEFFICIENT = 1.0;

    // Статусы билетов (должны соответствовать БД)
    public static class TicketStatus {
        public static final int PAID = 1;
        public static final int BOOKED = 2;
        public static final int REFUND = 3;
    }

    // Роли пользователей
    public static class UserRole {
        public static final int CASHIER = 1;
        public static final int ADMIN = 2;
    }
}
