package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.db.DataSourceProvider;
import com.cinema.cinemamanagementsystem.model.Customer;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerDao {
    private final DataSource dataSource = DataSourceProvider.getDataSource();

    public List<Customer> findAll() throws SQLException {
        String sql = "SELECT customer_id, name, phone, email, registered, created_at FROM customer";
        List<Customer> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(mapCustomer(rs));
            }
        }
        return result;
    }

    public Optional<Customer> findById(int id) throws SQLException {
        String sql = "SELECT customer_id, name, phone, email, registered, created_at FROM customer WHERE customer_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapCustomer(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Customer> search(String query) throws SQLException {
        String sql = "SELECT customer_id, name, phone, email, registered, created_at FROM customer "
                + "WHERE name LIKE ? OR phone LIKE ? OR email LIKE ?";
        String like = "%" + query + "%";
        List<Customer> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, like);
            statement.setString(2, like);
            statement.setString(3, like);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(mapCustomer(rs));
                }
            }
        }
        return result;
    }

    public int create(Customer customer) throws SQLException {
        String sql = "INSERT INTO customer(name, phone, email, registered, created_at) VALUES (?, ?, ?, ?, ?)";
        LocalDateTime createdAt = customer.createdAt() == null ? LocalDateTime.now() : customer.createdAt();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, customer.name());
            statement.setString(2, customer.phone());
            statement.setString(3, customer.email());
            statement.setBoolean(4, customer.registered());
            statement.setTimestamp(5, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void update(Customer customer) throws SQLException {
        String sql = "UPDATE customer SET name = ?, phone = ?, email = ?, registered = ? WHERE customer_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customer.name());
            statement.setString(2, customer.phone());
            statement.setString(3, customer.email());
            statement.setBoolean(4, customer.registered());
            statement.setInt(5, customer.customerId());
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM customer WHERE customer_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private Customer mapCustomer(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        return new Customer(
                rs.getInt("customer_id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getBoolean("registered"),
                created != null ? created.toLocalDateTime() : null
        );
    }
}
