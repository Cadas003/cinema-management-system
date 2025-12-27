package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.models.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {
    private static final Logger logger = LoggerFactory.getLogger(PaymentDAO.class);

    public boolean createPayment(Payment payment) {
        String query = "INSERT INTO payment (ticket_id, user_id, amount, method_id, payment_time) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, payment.getTicketId());
            pstmt.setInt(2, payment.getUserId());
            pstmt.setDouble(3, payment.getAmount());
            pstmt.setInt(4, payment.getMethodId());
            pstmt.setTimestamp(5, Timestamp.valueOf(payment.getPaymentTime()));

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при создании платежа: {}", e.getMessage());
            return false;
        }
    }

    public List<Payment> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Payment> payments = new ArrayList<>();
        String query =
                "SELECT p.*, pm.name AS method_name, u.username AS user_name, t.ticket_id " +
                        "FROM payment p " +
                        "JOIN payment_method pm ON p.method_id = pm.method_id " +
                        "JOIN users u ON p.user_id = u.user_id " +
                        "JOIN ticket t ON p.ticket_id = t.ticket_id " +
                        "WHERE p.payment_time BETWEEN ? AND ? " +
                        "ORDER BY p.payment_time DESC";


        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(startDate));
            pstmt.setTimestamp(2, Timestamp.valueOf(endDate));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Payment payment = new Payment();
                payment.setPaymentId(rs.getInt("payment_id"));
                payment.setTicketId(rs.getInt("ticket_id"));
                payment.setUserId(rs.getInt("user_id"));
                payment.setUserName(rs.getString("user_name"));
                payment.setAmount(rs.getDouble("amount"));
                payment.setMethodId(rs.getInt("method_id"));
                payment.setMethodName(rs.getString("method_name"));
                payment.setPaymentTime(rs.getTimestamp("payment_time").toLocalDateTime());
                payments.add(payment);
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении платежей: {}", e.getMessage());
        }
        return payments;
    }

    public double getTotalRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        String query = "SELECT COALESCE(SUM(amount), 0) as total FROM payment WHERE payment_time BETWEEN ? AND ? AND amount > 0";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(startDate));
            pstmt.setTimestamp(2, Timestamp.valueOf(endDate));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            logger.error("Ошибка при расчете выручки: {}", e.getMessage());
        }
        return 0.0;
    }
}
