package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.db.DataSourceProvider;
import com.cinema.cinemamanagementsystem.model.Ticket;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TicketDao {
    private final DataSource dataSource = DataSourceProvider.getDataSource();

    public long create(Connection connection, Ticket ticket) throws SQLException {
        String sql = "INSERT INTO ticket(showtime_id, seat_id, customer_id, user_id, status_id, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, ticket.showtimeId());
            statement.setInt(2, ticket.seatId());
            if (ticket.customerId() == null) {
                statement.setNull(3, Types.INTEGER);
            } else {
                statement.setInt(3, ticket.customerId());
            }
            statement.setInt(4, ticket.userId());
            statement.setInt(5, ticket.statusId());
            statement.setTimestamp(6, Timestamp.valueOf(ticket.createdAt()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        return -1;
    }

    public void updateStatus(Connection connection, long ticketId, int statusId) throws SQLException {
        String sql = "UPDATE ticket SET status_id = ? WHERE ticket_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, statusId);
            statement.setLong(2, ticketId);
            statement.executeUpdate();
        }
    }

    public List<Ticket> findByShowtime(int showtimeId) throws SQLException {
        String sql = "SELECT ticket_id, showtime_id, seat_id, customer_id, user_id, status_id, created_at "
                + "FROM ticket WHERE showtime_id = ?";
        List<Ticket> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, showtimeId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(mapTicket(rs));
                }
            }
        }
        return result;
    }

    public Ticket findById(long ticketId) throws SQLException {
        String sql = "SELECT ticket_id, showtime_id, seat_id, customer_id, user_id, status_id, created_at "
                + "FROM ticket WHERE ticket_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ticketId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapTicket(rs);
                }
            }
        }
        return null;
    }

    public List<Integer> findOccupiedSeatIds(int showtimeId, List<Integer> excludedStatusIds) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT seat_id FROM ticket WHERE showtime_id = ?");
        if (!excludedStatusIds.isEmpty()) {
            sql.append(" AND status_id NOT IN (");
            for (int i = 0; i < excludedStatusIds.size(); i++) {
                sql.append("?");
                if (i < excludedStatusIds.size() - 1) {
                    sql.append(",");
                }
            }
            sql.append(")");
        }
        List<Integer> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setInt(1, showtimeId);
            int index = 2;
            for (Integer statusId : excludedStatusIds) {
                statement.setInt(index++, statusId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getInt("seat_id"));
                }
            }
        }
        return result;
    }

    public List<Ticket> findExpiredReservations(LocalDateTime expirationThreshold, int reservedStatusId) throws SQLException {
        String sql = "SELECT ticket_id, showtime_id, seat_id, customer_id, user_id, status_id, created_at "
                + "FROM ticket WHERE status_id = ? AND created_at < ?";
        List<Ticket> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reservedStatusId);
            statement.setTimestamp(2, Timestamp.valueOf(expirationThreshold));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(mapTicket(rs));
                }
            }
        }
        return result;
    }

    private Ticket mapTicket(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new Ticket(
                rs.getLong("ticket_id"),
                rs.getInt("showtime_id"),
                rs.getInt("seat_id"),
                rs.getObject("customer_id", Integer.class),
                rs.getInt("user_id"),
                rs.getInt("status_id"),
                createdAt != null ? createdAt.toLocalDateTime() : null
        );
    }
}
