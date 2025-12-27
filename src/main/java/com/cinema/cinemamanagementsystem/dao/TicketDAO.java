package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.models.Ticket;
import com.cinema.cinemamanagementsystem.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TicketDAO {
    private static final Logger logger = LoggerFactory.getLogger(TicketDAO.class);

    // Создать новый билет
    public int createTicket(Ticket ticket) {
        String query = " INSERT INTO ticket (showtime_id, seat_id, customer_id, user_id, status_id, created_at)"+
            "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, ticket.getShowtimeId());
            pstmt.setInt(2, ticket.getSeatId());

            if (ticket.getCustomerId() > 0) {
                pstmt.setInt(3, ticket.getCustomerId());
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }

            pstmt.setInt(4, ticket.getUserId());
            pstmt.setInt(5, ticket.getStatusId());
            pstmt.setTimestamp(6, Timestamp.valueOf(ticket.getCreatedAt()));

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при создании билета: {}", e.getMessage());
        }
        return -1;
    }

    // Получить билет по ID
    public Ticket getTicketById(int ticketId) {
        String query =
                "SELECT t.*, s.seat_row, s.seat_number, sc.name AS seat_category, " +
                        "       c.name AS customer_name, u.username AS user_name, ts.name AS status_name, " +
                        "       sh.date_time, sh.base_price, pr.coefficient, " +
                        "       f.title AS film_title, h.name AS hall_name " +
                        "FROM ticket t " +
                        "JOIN showtime sh ON t.showtime_id = sh.showtime_id " +
                        "JOIN film f ON sh.film_id = f.film_id " +
                        "JOIN hall h ON sh.hall_id = h.hall_id " +
                        "JOIN seat s ON t.seat_id = s.seat_id " +
                        "JOIN seat_category sc ON s.category_id = sc.category_id " +
                        "LEFT JOIN customer c ON t.customer_id = c.customer_id " +
                        "JOIN users u ON t.user_id = u.user_id " +
                        "JOIN ticket_status ts ON t.status_id = ts.status_id " +
                        "LEFT JOIN price_rule pr ON sh.rule_id = pr.rule_id " +
                        "WHERE t.ticket_id = ?";


        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, ticketId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToTicket(rs);
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении билета: {}", e.getMessage());
        }
        return null;
    }

    // Обновить статус билета
    public boolean updateTicketStatus(int ticketId, int statusId) {
        String query = "UPDATE ticket SET status_id = ? WHERE ticket_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, statusId);
            pstmt.setInt(2, ticketId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении статуса билета: {}", e.getMessage());
            return false;
        }
    }

    // Получить активные бронирования (истекшие и неистекшие)
    public List<Ticket> getActiveBookings(boolean expiredOnly) {
        List<Ticket> tickets = new ArrayList<>();
        String query =
                "SELECT t.*, s.seat_row, s.seat_number, sc.name AS seat_category, " +
                        "       c.name AS customer_name, u.username AS user_name, ts.name AS status_name, " +
                        "       sh.date_time, sh.base_price, pr.coefficient, " +
                        "       f.title AS film_title, h.name AS hall_name " +
                        "FROM ticket t " +
                        "JOIN showtime sh ON t.showtime_id = sh.showtime_id " +
                        "JOIN film f ON sh.film_id = f.film_id " +
                        "JOIN hall h ON sh.hall_id = h.hall_id " +
                        "JOIN seat s ON t.seat_id = s.seat_id " +
                        "JOIN seat_category sc ON s.category_id = sc.category_id " +
                        "LEFT JOIN customer c ON t.customer_id = c.customer_id " +
                        "JOIN users u ON t.user_id = u.user_id " +
                        "JOIN ticket_status ts ON t.status_id = ts.status_id " +
                        "LEFT JOIN price_rule pr ON sh.rule_id = pr.rule_id " +
                        "WHERE t.status_id = 2";


        if (expiredOnly) {
            query += " AND t.created_at < DATE_SUB(NOW(), INTERVAL ? MINUTE)";
        } else {
            query += " AND t.created_at >= DATE_SUB(NOW(), INTERVAL ? MINUTE)";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, DatabaseConfig.BOOKING_TIMEOUT_MINUTES);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                tickets.add(mapResultSetToTicket(rs));
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении бронирований: {}", e.getMessage());
        }
        return tickets;
    }

    // Отменить истекшие бронирования
    public int cancelExpiredBookings() {
        String query =
                "UPDATE ticket " +
                        "SET status_id = ? " +        // статус отмены
                        "WHERE status_id = 2 " +      // забронирован
                        "AND created_at < DATE_SUB(NOW(), INTERVAL ? MINUTE)";


        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, DatabaseConfig.TicketStatus.REFUND);
            pstmt.setInt(2, DatabaseConfig.BOOKING_TIMEOUT_MINUTES);

            return pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка при отмене истекших бронирований: {}", e.getMessage());
            return 0;
        }
    }

    // Получить билеты клиента
    public List<Ticket> getCustomerTickets(int customerId) {
        List<Ticket> tickets = new ArrayList<>();
        String query =
                "SELECT t.*, s.seat_row, s.seat_number, sc.name AS seat_category, " +
                        "       c.name AS customer_name, u.username AS user_name, ts.name AS status_name, " +
                        "       sh.date_time, sh.base_price, pr.coefficient, " +
                        "       f.title AS film_title, h.name AS hall_name " +
                        "FROM ticket t " +
                        "JOIN showtime sh ON t.showtime_id = sh.showtime_id " +
                        "JOIN film f ON sh.film_id = f.film_id " +
                        "JOIN hall h ON sh.hall_id = h.hall_id " +
                        "JOIN seat s ON t.seat_id = s.seat_id " +
                        "JOIN seat_category sc ON s.category_id = sc.category_id " +
                        "JOIN customer c ON t.customer_id = c.customer_id " +
                        "JOIN users u ON t.user_id = u.user_id " +
                        "JOIN ticket_status ts ON t.status_id = ts.status_id " +
                        "LEFT JOIN price_rule pr ON sh.rule_id = pr.rule_id " +
                        "WHERE t.customer_id = ? " +
                        "ORDER BY t.created_at DESC";


        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                tickets.add(mapResultSetToTicket(rs));
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении билетов клиента: {}", e.getMessage());
        }
        return tickets;
    }

    private Ticket mapResultSetToTicket(ResultSet rs) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setTicketId(rs.getInt("ticket_id"));
        ticket.setShowtimeId(rs.getInt("showtime_id"));
        ticket.setSeatId(rs.getInt("seat_id"));

        // Формируем информацию о месте
        String seatInfo = "Ряд " + rs.getInt("seat_row") +
                ", Место " + rs.getInt("seat_number") +
                " (" + rs.getString("seat_category") + ")";
        ticket.setSeatInfo(seatInfo);

        int customerId = rs.getInt("customer_id");
        boolean hasCustomer = !rs.wasNull();
        ticket.setCustomerId(hasCustomer ? customerId : 0);
        ticket.setCustomerName(rs.getString("customer_name"));
        ticket.setUserId(rs.getInt("user_id"));
        ticket.setUserName(rs.getString("user_name"));
        ticket.setStatusId(rs.getInt("status_id"));
        ticket.setStatusName(rs.getString("status_name"));
        ticket.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        // Рассчитываем финальную цену
        double basePrice = rs.getDouble("base_price");
        double coefficient = hasCustomer
                ? rs.getDouble("coefficient")
                : DatabaseConfig.GUEST_PRICE_COEFFICIENT;
        if (coefficient == 0) {
            coefficient = 1.0;
        }

        double finalPrice = basePrice * coefficient;
        // Если билет был забронирован, добавляем доплату 15%
        if (ticket.getStatusId() == DatabaseConfig.TicketStatus.BOOKED) {
            finalPrice *= (1 + DatabaseConfig.BOOKING_SURCHARGE_RATE);
        }
        ticket.setFinalPrice(finalPrice);

        return ticket;
    }
}
