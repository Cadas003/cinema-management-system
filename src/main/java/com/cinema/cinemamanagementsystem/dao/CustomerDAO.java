package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.models.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {
    private static final Logger logger = LoggerFactory.getLogger(CustomerDAO.class);

    // Создать или найти клиента
    public Customer findOrCreateCustomer(String name, String phone, String email) {
        Customer existing = findCustomerByPhone(phone);
        if (existing != null) {
            return existing;
        }

        String query = "INSERT INTO customer (name, phone, email, registered, created_at) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.setString(3, email);
            pstmt.setBoolean(4, true);
            pstmt.setTimestamp(5, Timestamp.valueOf(java.time.LocalDateTime.now()));

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    Customer newCustomer = new Customer();
                    newCustomer.setCustomerId(rs.getInt(1));
                    newCustomer.setName(name);
                    newCustomer.setPhone(phone);
                    newCustomer.setEmail(email);
                    newCustomer.setRegistered(true);
                    newCustomer.setCreatedAt(java.time.LocalDateTime.now());
                    return newCustomer;
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при создании клиента: {}", e.getMessage());
        }
        return null;
    }

    // Создать гостя (без регистрации)
    public Customer createGuestCustomer(String name, String phone, String email) {
        String query = "INSERT INTO customer (name, phone, email, registered, created_at) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.setString(3, email);
            pstmt.setBoolean(4, false);
            pstmt.setTimestamp(5, Timestamp.valueOf(java.time.LocalDateTime.now()));

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    Customer newCustomer = new Customer();
                    newCustomer.setCustomerId(rs.getInt(1));
                    newCustomer.setName(name);
                    newCustomer.setPhone(phone);
                    newCustomer.setEmail(email);
                    newCustomer.setRegistered(false);
                    newCustomer.setCreatedAt(java.time.LocalDateTime.now());
                    return newCustomer;
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при создании гостя: {}", e.getMessage());
        }
        return null;
    }

    public Customer getCustomerById(int customerId) {
        String query = "SELECT * FROM customer WHERE customer_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToCustomer(rs);
            }
        } catch (SQLException e) {
            logger.error("Ошибка при поиске клиента по ID: {}", e.getMessage());
        }
        return null;
    }

    public List<Customer> getRegisteredCustomers() {
        List<Customer> customers = new ArrayList<>();
        String query = "SELECT * FROM customer WHERE registered = 1 ORDER BY name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                customers.add(mapResultSetToCustomer(rs));
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении зарегистрированных клиентов: {}", e.getMessage());
        }

        return customers;
    }

    // Найти клиента по телефону
    public Customer findCustomerByPhone(String phone) {
        String query = "SELECT * FROM customer WHERE phone = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, phone);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToCustomer(rs);
            }
        } catch (SQLException e) {
            logger.error("Ошибка при поиске клиента: {}", e.getMessage());
        }
        return null;
    }

    // Получить всех клиентов (для таблицы)
    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();

        String query =
                "SELECT c.*, " +
                        "       (SELECT COUNT(*) FROM ticket WHERE customer_id = c.customer_id AND status_id = 1) AS visit_count, " +
                        "       (SELECT COALESCE(SUM(p.amount), 0) FROM payment p " +
                        "        JOIN ticket t ON p.ticket_id = t.ticket_id " +
                        "        WHERE t.customer_id = c.customer_id) AS total_spent " +
                        "FROM customer c " +
                        "ORDER BY c.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Customer customer = mapResultSetToCustomer(rs);
                customer.setVisitCount(rs.getInt("visit_count"));
                customer.setTotalSpent(rs.getDouble("total_spent"));
                customers.add(customer);
            }

        } catch (SQLException e) {
            logger.error("Ошибка при загрузке всех клиентов: {}", e.getMessage());
        }

        return customers;
    }

    // Поиск клиентов
    public List<Customer> searchCustomers(String searchText) {
        List<Customer> customers = new ArrayList<>();
        String query =
                "SELECT c.*, " +
                        "       (SELECT COUNT(*) FROM ticket WHERE customer_id = c.customer_id AND status_id = 1) AS visit_count, " +
                        "       (SELECT COALESCE(SUM(p.amount), 0) FROM payment p " +
                        "        JOIN ticket t ON p.ticket_id = t.ticket_id " +
                        "        WHERE t.customer_id = c.customer_id) AS total_spent " +
                        "FROM customer c " +
                        "WHERE c.name LIKE ? OR c.phone LIKE ? OR c.email LIKE ? " +
                        "ORDER BY c.created_at DESC " +
                        "LIMIT 50";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            String searchPattern = "%" + searchText + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Customer customer = mapResultSetToCustomer(rs);
                customer.setVisitCount(rs.getInt("visit_count"));
                customer.setTotalSpent(rs.getDouble("total_spent"));
                customers.add(customer);
            }
        } catch (SQLException e) {
            logger.error("Ошибка при поиске клиентов: {}", e.getMessage());
        }
        return customers;
    }

    // Обновить клиента
    public boolean updateCustomer(Customer customer) {
        String query = "UPDATE customer SET name = ?, phone = ?, email = ? WHERE customer_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, customer.getName());
            pstmt.setString(2, customer.getPhone());
            pstmt.setString(3, customer.getEmail());
            pstmt.setInt(4, customer.getCustomerId());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.error("Ошибка при обновлении клиента: {}", e.getMessage());
            return false;
        }
    }

    // Получить статистику по клиенту
    public Customer getCustomerStats(int customerId) {
        String query =
                "SELECT c.*, " +
                        "       (SELECT COUNT(*) FROM ticket WHERE customer_id = c.customer_id AND status_id = 1) AS visit_count, " +
                        "       (SELECT COALESCE(SUM(p.amount), 0) FROM payment p " +
                        "        JOIN ticket t ON p.ticket_id = t.ticket_id " +
                        "        WHERE t.customer_id = c.customer_id) AS total_spent " +
                        "FROM customer c " +
                        "WHERE c.customer_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Customer customer = mapResultSetToCustomer(rs);
                customer.setVisitCount(rs.getInt("visit_count"));
                customer.setTotalSpent(rs.getDouble("total_spent"));
                return customer;
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении статистики клиента: {}", e.getMessage());
        }
        return null;
    }

    // Маппер
    private Customer mapResultSetToCustomer(ResultSet rs) throws SQLException {
        Customer customer = new Customer();
        customer.setCustomerId(rs.getInt("customer_id"));
        customer.setName(rs.getString("name"));
        customer.setPhone(rs.getString("phone"));
        customer.setEmail(rs.getString("email"));
        customer.setRegistered(rs.getBoolean("registered"));
        customer.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        // Эти поля могут отсутствовать — не ошибка
        try {
            customer.setVisitCount(rs.getInt("visit_count"));
            customer.setTotalSpent(rs.getDouble("total_spent"));
        } catch (SQLException ignored) {}

        return customer;
    }
}
