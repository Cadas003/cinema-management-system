package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

    // Аутентификация пользователя
    public User authenticate(String login, String password) {
        String query =
                "SELECT u.*, ur.name as role_name " +   // ← пробел в конце
                        "FROM users u " +                       // ← пробел в конце
                        "JOIN user_role ur ON u.role_id = ur.role_id " +
                        "WHERE u.login = ? AND u.password_hash = ?";


        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, login);
            pstmt.setString(2, password); // В реальной системе здесь должен быть хеш

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            logger.error("Ошибка при аутентификации: {}", e.getMessage());
        }
        return null;
    }

    // Получить всех пользователей
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String query =
                "SELECT u.*, ur.name as role_name " +
                        "FROM users u " +
                        "JOIN user_role ur ON u.role_id = ur.role_id " +
                        "ORDER BY u.username";


        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении пользователей: {}", e.getMessage());
        }
        return users;
    }

    // Добавить пользователя
    public boolean addUser(User user) {
        String query = "INSERT INTO users (username, password_hash, role_id, login) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setInt(3, user.getRoleId());
            pstmt.setString(4, user.getLogin());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при добавлении пользователя: {}", e.getMessage());
            return false;
        }
    }

    // Обновить пользователя
    public boolean updateUser(User user) {
        String query = "UPDATE users SET username = ?, role_id = ?, login = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setInt(2, user.getRoleId());
            pstmt.setString(3, user.getLogin());
            pstmt.setInt(4, user.getUserId());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении пользователя: {}", e.getMessage());
            return false;
        }
    }

    // Сменить пароль
    public boolean changePassword(int userId, String newPasswordHash) {
        String query = "UPDATE users SET password_hash = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, newPasswordHash);
            pstmt.setInt(2, userId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при смене пароля: {}", e.getMessage());
            return false;
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRoleId(rs.getInt("role_id"));
        user.setRoleName(rs.getString("role_name"));
        user.setLogin(rs.getString("login"));
        user.setFullName(rs.getString("username")); // В вашей БД username хранит ФИО
        return user;
    }
}
