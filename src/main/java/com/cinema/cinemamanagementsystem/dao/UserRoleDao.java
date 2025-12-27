package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.db.DataSourceProvider;
import com.cinema.cinemamanagementsystem.model.UserRole;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRoleDao {
    private final DataSource dataSource = DataSourceProvider.getDataSource();

    public List<UserRole> findAll() throws SQLException {
        String sql = "SELECT role_id, name, mysql_role FROM user_role";
        List<UserRole> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(mapRole(rs));
            }
        }
        return result;
    }

    public Optional<UserRole> findById(int id) throws SQLException {
        String sql = "SELECT role_id, name, mysql_role FROM user_role WHERE role_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRole(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<UserRole> findByName(String name) throws SQLException {
        String sql = "SELECT role_id, name, mysql_role FROM user_role WHERE name = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRole(rs));
                }
            }
        }
        return Optional.empty();
    }

    public int create(UserRole role) throws SQLException {
        String sql = "INSERT INTO user_role(name, mysql_role) VALUES (?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, role.name());
            statement.setString(2, role.mysqlRole());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void update(UserRole role) throws SQLException {
        String sql = "UPDATE user_role SET name = ?, mysql_role = ? WHERE role_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role.name());
            statement.setString(2, role.mysqlRole());
            statement.setInt(3, role.roleId());
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM user_role WHERE role_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private UserRole mapRole(ResultSet rs) throws SQLException {
        return new UserRole(
                rs.getInt("role_id"),
                rs.getString("name"),
                rs.getString("mysql_role")
        );
    }
}
