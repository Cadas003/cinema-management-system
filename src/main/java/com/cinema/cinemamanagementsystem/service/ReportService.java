package com.cinema.cinemamanagementsystem.service;

import com.cinema.cinemamanagementsystem.db.DataSourceProvider;
import com.cinema.cinemamanagementsystem.util.MoneyUtil;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReportService {
    private final DataSource dataSource = DataSourceProvider.getDataSource();

    public ReportSummary summary(LocalDateTime from, LocalDateTime to) throws SQLException {
        int paidStatus = statusId("оплачен");
        int reservedStatus = statusId("забронирован");
        String ticketSql = "SELECT "
                + "SUM(CASE WHEN status_id = ? THEN 1 ELSE 0 END) AS paid_count, "
                + "SUM(CASE WHEN status_id = ? THEN 1 ELSE 0 END) AS reserved_count "
                + "FROM ticket WHERE created_at BETWEEN ? AND ?";
        String paymentSql = "SELECT COUNT(1) AS payment_count, COALESCE(SUM(amount), 0) AS revenue "
                + "FROM payment WHERE payment_time BETWEEN ? AND ?";
        int paidCount = 0;
        int reservedCount = 0;
        int paymentCount = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ticketStatement = connection.prepareStatement(ticketSql);
             PreparedStatement paymentStatement = connection.prepareStatement(paymentSql)) {
            ticketStatement.setInt(1, paidStatus);
            ticketStatement.setInt(2, reservedStatus);
            ticketStatement.setTimestamp(3, Timestamp.valueOf(from));
            ticketStatement.setTimestamp(4, Timestamp.valueOf(to));
            try (ResultSet rs = ticketStatement.executeQuery()) {
                if (rs.next()) {
                    paidCount = rs.getInt("paid_count");
                    reservedCount = rs.getInt("reserved_count");
                }
            }
            paymentStatement.setTimestamp(1, Timestamp.valueOf(from));
            paymentStatement.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet rs = paymentStatement.executeQuery()) {
                if (rs.next()) {
                    paymentCount = rs.getInt("payment_count");
                    revenue = rs.getBigDecimal("revenue");
                }
            }
        }
        return new ReportSummary(paidCount, reservedCount, paymentCount, MoneyUtil.round(revenue));
    }

    public List<FilmPopularity> popularFilms(LocalDateTime from, LocalDateTime to) throws SQLException {
        int paidStatus = statusId("оплачен");
        String sql = "SELECT f.title, COUNT(t.ticket_id) AS sold "
                + "FROM ticket t "
                + "JOIN showtime s ON t.showtime_id = s.showtime_id "
                + "JOIN film f ON s.film_id = f.film_id "
                + "WHERE t.status_id = ? AND t.created_at BETWEEN ? AND ? "
                + "GROUP BY f.title ORDER BY sold DESC";
        List<FilmPopularity> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, paidStatus);
            statement.setTimestamp(2, Timestamp.valueOf(from));
            statement.setTimestamp(3, Timestamp.valueOf(to));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new FilmPopularity(rs.getString("title"), rs.getLong("sold")));
                }
            }
        }
        return result;
    }

    public List<HallLoad> hallLoad(LocalDateTime from, LocalDateTime to) throws SQLException {
        int paidStatus = statusId("оплачен");
        String sql = "SELECT h.name AS hall_name, COUNT(t.ticket_id) AS occupied, h.capacity AS total "
                + "FROM hall h "
                + "LEFT JOIN showtime s ON h.hall_id = s.hall_id "
                + "LEFT JOIN ticket t ON s.showtime_id = t.showtime_id "
                + "AND t.status_id = ? AND t.created_at BETWEEN ? AND ? "
                + "GROUP BY h.hall_id, h.name, h.capacity";
        List<HallLoad> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, paidStatus);
            statement.setTimestamp(2, Timestamp.valueOf(from));
            statement.setTimestamp(3, Timestamp.valueOf(to));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new HallLoad(
                            rs.getString("hall_name"),
                            rs.getLong("occupied"),
                            rs.getLong("total")
                    ));
                }
            }
        }
        return result;
    }

    private int statusId(String name) throws SQLException {
        String sql = "SELECT status_id FROM ticket_status WHERE name = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("status_id");
                }
            }
        }
        throw new IllegalStateException("Статус не найден: " + name);
    }
}
