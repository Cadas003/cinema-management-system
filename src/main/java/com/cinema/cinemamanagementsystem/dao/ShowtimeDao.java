package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.db.DataSourceProvider;
import com.cinema.cinemamanagementsystem.model.Showtime;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShowtimeDao {
    private final DataSource dataSource = DataSourceProvider.getDataSource();

    public List<Showtime> findAll() throws SQLException {
        String sql = "SELECT showtime_id, film_id, hall_id, date_time, base_price, rule_id FROM showtime";
        List<Showtime> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(mapShowtime(rs));
            }
        }
        return result;
    }

    public Optional<Showtime> findById(int id) throws SQLException {
        String sql = "SELECT showtime_id, film_id, hall_id, date_time, base_price, rule_id FROM showtime WHERE showtime_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapShowtime(rs));
                }
            }
        }
        return Optional.empty();
    }

    public int create(Showtime showtime) throws SQLException {
        String sql = "INSERT INTO showtime(film_id, hall_id, date_time, base_price, rule_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, showtime.filmId());
            statement.setInt(2, showtime.hallId());
            statement.setTimestamp(3, Timestamp.valueOf(showtime.dateTime()));
            statement.setBigDecimal(4, showtime.basePrice());
            if (showtime.ruleId() == null) {
                statement.setNull(5, Types.INTEGER);
            } else {
                statement.setInt(5, showtime.ruleId());
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void update(Showtime showtime) throws SQLException {
        String sql = "UPDATE showtime SET film_id = ?, hall_id = ?, date_time = ?, base_price = ?, rule_id = ? WHERE showtime_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, showtime.filmId());
            statement.setInt(2, showtime.hallId());
            statement.setTimestamp(3, Timestamp.valueOf(showtime.dateTime()));
            statement.setBigDecimal(4, showtime.basePrice());
            if (showtime.ruleId() == null) {
                statement.setNull(5, Types.INTEGER);
            } else {
                statement.setInt(5, showtime.ruleId());
            }
            statement.setInt(6, showtime.showtimeId());
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM showtime WHERE showtime_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public boolean hasOverlap(int hallId, LocalDateTime start, LocalDateTime end, Integer excludeShowtimeId) throws SQLException {
        String sql = "SELECT COUNT(1) FROM showtime s "
                + "JOIN film f ON s.film_id = f.film_id "
                + "WHERE s.hall_id = ? "
                + "AND s.date_time < ? "
                + "AND DATE_ADD(s.date_time, INTERVAL f.duration MINUTE) > ? "
                + "AND (? IS NULL OR s.showtime_id <> ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, hallId);
            statement.setTimestamp(2, Timestamp.valueOf(end));
            statement.setTimestamp(3, Timestamp.valueOf(start));
            if (excludeShowtimeId == null) {
                statement.setNull(4, Types.INTEGER);
                statement.setNull(5, Types.INTEGER);
            } else {
                statement.setInt(4, excludeShowtimeId);
                statement.setInt(5, excludeShowtimeId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private Showtime mapShowtime(ResultSet rs) throws SQLException {
        Timestamp timestamp = rs.getTimestamp("date_time");
        return new Showtime(
                rs.getInt("showtime_id"),
                rs.getInt("film_id"),
                rs.getInt("hall_id"),
                timestamp != null ? timestamp.toLocalDateTime() : null,
                rs.getBigDecimal("base_price"),
                rs.getObject("rule_id", Integer.class)
        );
    }
}
