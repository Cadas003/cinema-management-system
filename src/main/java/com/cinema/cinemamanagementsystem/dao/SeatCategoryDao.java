package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.db.DataSourceProvider;
import com.cinema.cinemamanagementsystem.model.SeatCategory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SeatCategoryDao {
    private final DataSource dataSource = DataSourceProvider.getDataSource();

    public List<SeatCategory> findAll() throws SQLException {
        String sql = "SELECT category_id, name, color FROM seat_category";
        List<SeatCategory> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(mapCategory(rs));
            }
        }
        return result;
    }

    public Optional<SeatCategory> findById(int id) throws SQLException {
        String sql = "SELECT category_id, name, color FROM seat_category WHERE category_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapCategory(rs));
                }
            }
        }
        return Optional.empty();
    }

    public int create(SeatCategory category) throws SQLException {
        String sql = "INSERT INTO seat_category(name, color) VALUES (?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, category.name());
            statement.setString(2, category.color());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void update(SeatCategory category) throws SQLException {
        String sql = "UPDATE seat_category SET name = ?, color = ? WHERE category_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.name());
            statement.setString(2, category.color());
            statement.setInt(3, category.categoryId());
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM seat_category WHERE category_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private SeatCategory mapCategory(ResultSet rs) throws SQLException {
        return new SeatCategory(
                rs.getInt("category_id"),
                rs.getString("name"),
                rs.getString("color")
        );
    }
}
