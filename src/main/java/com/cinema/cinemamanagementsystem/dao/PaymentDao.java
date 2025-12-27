package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.db.DataSourceProvider;
import com.cinema.cinemamanagementsystem.model.Payment;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PaymentDao {
    private final DataSource dataSource = DataSourceProvider.getDataSource();

    public long create(Connection connection, Payment payment) throws SQLException {
        String sql = "INSERT INTO payment(ticket_id, user_id, amount, method_id, payment_time) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, payment.ticketId());
            statement.setInt(2, payment.userId());
            statement.setBigDecimal(3, payment.amount());
            statement.setInt(4, payment.methodId());
            statement.setTimestamp(5, Timestamp.valueOf(payment.paymentTime()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        return -1;
    }

    public List<Payment> findByPeriod(LocalDateTime from, LocalDateTime to) throws SQLException {
        String sql = "SELECT payment_id, ticket_id, user_id, amount, method_id, payment_time FROM payment "
                + "WHERE payment_time BETWEEN ? AND ?";
        List<Payment> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(from));
            statement.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(mapPayment(rs));
                }
            }
        }
        return result;
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Timestamp paymentTime = rs.getTimestamp("payment_time");
        return new Payment(
                rs.getLong("payment_id"),
                rs.getLong("ticket_id"),
                rs.getInt("user_id"),
                rs.getBigDecimal("amount"),
                rs.getInt("method_id"),
                paymentTime != null ? paymentTime.toLocalDateTime() : null
        );
    }
}
