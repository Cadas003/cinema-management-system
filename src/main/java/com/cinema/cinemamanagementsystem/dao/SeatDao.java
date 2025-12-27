package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.db.DataSourceProvider;
import com.cinema.cinemamanagementsystem.model.Seat;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SeatDao {
    private final DataSource dataSource = DataSourceProvider.getDataSource();

    public List<Seat> findByHall(int hallId) throws SQLException {
        String sql = "SELECT seat_id, hall_id, seat_row, seat_number, category_id FROM seat "
                + "WHERE hall_id = ? ORDER BY seat_row, seat_number";
        List<Seat> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, hallId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(mapSeat(rs));
                }
            }
        }
        return result;
    }

    public Optional<Seat> findById(int id) throws SQLException {
        String sql = "SELECT seat_id, hall_id, seat_row, seat_number, category_id FROM seat WHERE seat_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapSeat(rs));
                }
            }
        }
        return Optional.empty();
    }

    public int create(Seat seat) throws SQLException {
        String sql = "INSERT INTO seat(hall_id, seat_row, seat_number, category_id) VALUES (?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, seat.hallId());
            statement.setInt(2, seat.seatRow());
            statement.setInt(3, seat.seatNumber());
            statement.setInt(4, seat.categoryId());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void update(Seat seat) throws SQLException {
        String sql = "UPDATE seat SET hall_id = ?, seat_row = ?, seat_number = ?, category_id = ? WHERE seat_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, seat.hallId());
            statement.setInt(2, seat.seatRow());
            statement.setInt(3, seat.seatNumber());
            statement.setInt(4, seat.categoryId());
            statement.setInt(5, seat.seatId());
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM seat WHERE seat_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private Seat mapSeat(ResultSet rs) throws SQLException {
        return new Seat(
                rs.getInt("seat_id"),
                rs.getInt("hall_id"),
                rs.getInt("seat_row"),
                rs.getInt("seat_number"),
                rs.getInt("category_id")
        );
    }
}
