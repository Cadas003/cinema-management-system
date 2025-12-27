package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.db.DataSourceProvider;
import com.cinema.cinemamanagementsystem.model.HallType;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HallTypeDao {
    private final DataSource dataSource = DataSourceProvider.getDataSource();

    public List<HallType> findAll() throws SQLException {
        String sql = "SELECT type_id, name FROM hall_type";
        List<HallType> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new HallType(rs.getInt("type_id"), rs.getString("name")));
            }
        }
        return result;
    }

    public Optional<HallType> findById(int id) throws SQLException {
        String sql = "SELECT type_id, name FROM hall_type WHERE type_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new HallType(rs.getInt("type_id"), rs.getString("name")));
                }
            }
        }
        return Optional.empty();
    }

    public int create(HallType type) throws SQLException {
        String sql = "INSERT INTO hall_type(name) VALUES (?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, type.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void update(HallType type) throws SQLException {
        String sql = "UPDATE hall_type SET name = ? WHERE type_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type.name());
            statement.setInt(2, type.typeId());
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM hall_type WHERE type_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }
}
