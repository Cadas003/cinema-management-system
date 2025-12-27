package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.db.DataSourceProvider;
import com.cinema.cinemamanagementsystem.model.PaymentMethod;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentMethodDao {
    private final DataSource dataSource = DataSourceProvider.getDataSource();

    public List<PaymentMethod> findAll() throws SQLException {
        String sql = "SELECT method_id, name FROM payment_method";
        List<PaymentMethod> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new PaymentMethod(rs.getInt("method_id"), rs.getString("name")));
            }
        }
        return result;
    }

    public Optional<PaymentMethod> findById(int id) throws SQLException {
        String sql = "SELECT method_id, name FROM payment_method WHERE method_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new PaymentMethod(rs.getInt("method_id"), rs.getString("name")));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<PaymentMethod> findByName(String name) throws SQLException {
        String sql = "SELECT method_id, name FROM payment_method WHERE name = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new PaymentMethod(rs.getInt("method_id"), rs.getString("name")));
                }
            }
        }
        return Optional.empty();
    }
}
