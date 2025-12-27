package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.db.DataSourceProvider;
import com.cinema.cinemamanagementsystem.model.Hall;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HallDao {
    private final DataSource dataSource = DataSourceProvider.getDataSource();

    public List<Hall> findAll() throws SQLException {
        String sql = "SELECT hall_id, name, capacity, type_id FROM hall";
        List<Hall> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(mapHall(rs));
            }
        }
        return result;
    }

    public Optional<Hall> findById(int id) throws SQLException {
        String sql = "SELECT hall_id, name, capacity, type_id FROM hall WHERE hall_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapHall(rs));
                }
            }
        }
        return Optional.empty();
    }

    public int create(Hall hall) throws SQLException {
        String sql = "INSERT INTO hall(name, capacity, type_id) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, hall.name());
            statement.setInt(2, hall.capacity());
            statement.setInt(3, hall.typeId());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void update(Hall hall) throws SQLException {
        String sql = "UPDATE hall SET name = ?, capacity = ?, type_id = ? WHERE hall_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hall.name());
            statement.setInt(2, hall.capacity());
            statement.setInt(3, hall.typeId());
            statement.setInt(4, hall.hallId());
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM hall WHERE hall_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private Hall mapHall(ResultSet rs) throws SQLException {
        return new Hall(
                rs.getInt("hall_id"),
                rs.getString("name"),
                rs.getInt("capacity"),
                rs.getInt("type_id")
        );
    }
}
