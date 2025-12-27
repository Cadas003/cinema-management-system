package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.db.DataSourceProvider;
import com.cinema.cinemamanagementsystem.model.TicketStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TicketStatusDao {
    private final DataSource dataSource = DataSourceProvider.getDataSource();

    public List<TicketStatus> findAll() throws SQLException {
        String sql = "SELECT status_id, name FROM ticket_status";
        List<TicketStatus> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new TicketStatus(rs.getInt("status_id"), rs.getString("name")));
            }
        }
        return result;
    }

    public Optional<TicketStatus> findById(int id) throws SQLException {
        String sql = "SELECT status_id, name FROM ticket_status WHERE status_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new TicketStatus(rs.getInt("status_id"), rs.getString("name")));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<TicketStatus> findByName(String name) throws SQLException {
        String sql = "SELECT status_id, name FROM ticket_status WHERE name = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new TicketStatus(rs.getInt("status_id"), rs.getString("name")));
                }
            }
        }
        return Optional.empty();
    }
}
