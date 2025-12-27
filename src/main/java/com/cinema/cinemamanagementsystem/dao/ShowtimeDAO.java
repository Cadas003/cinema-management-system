package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.models.Seat;
import com.cinema.cinemamanagementsystem.models.Showtime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ShowtimeDAO {
    private static final Logger logger = LoggerFactory.getLogger(ShowtimeDAO.class);

    // Получить все сеансы
    public List<Showtime> getAllShowtimes() {
        List<Showtime> showtimes = new ArrayList<>();
        String query =
                "SELECT s.*, f.title AS film_title, h.name AS hall_name, " +
                        "       pr.name AS rule_name, pr.coefficient " +
                        "FROM showtime s " +
                        "JOIN film f ON s.film_id = f.film_id " +
                        "JOIN hall h ON s.hall_id = h.hall_id " +
                        "LEFT JOIN price_rule pr ON s.rule_id = pr.rule_id " +
                        "ORDER BY s.date_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                showtimes.add(mapResultSetToShowtime(rs));
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении сеансов: {}", e.getMessage());
        }
        return showtimes;
    }

    // Получить будущие сеансы
    public List<Showtime> getFutureShowtimes() {
        List<Showtime> showtimes = new ArrayList<>();
        String query = " SELECT * FROM view_future_showtimes";


        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                showtimes.add(mapResultSetToShowtime(rs));
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении будущих сеансов: {}", e.getMessage());
        }
        return showtimes;
    }

    // Добавить сеанс
    public boolean addShowtime(Showtime showtime) {
        String query = "INSERT INTO showtime (film_id, hall_id, date_time, base_price, rule_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, showtime.getFilmId());
            pstmt.setInt(2, showtime.getHallId());
            pstmt.setTimestamp(3, Timestamp.valueOf(showtime.getDateTime()));
            pstmt.setDouble(4, showtime.getBasePrice());
            pstmt.setInt(5, showtime.getRuleId());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.error("Ошибка при добавлении сеанса: {}", e.getMessage());
            return false;
        }
    }

    // Проверить пересечение сеансов
    public boolean hasTimeConflict(int hallId, LocalDateTime startTime, LocalDateTime endTime, Integer excludeShowtimeId) {
        String query =
                "SELECT COUNT(*) AS conflict_count " +
                        "FROM showtime s " +
                        "JOIN film f ON s.film_id = f.film_id " +
                        "WHERE s.hall_id = ? " +
                        "AND s.date_time < ? " +
                        "AND DATE_ADD(s.date_time, INTERVAL (f.duration + 15) MINUTE) > ? " +
                        "AND s.showtime_id != ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, hallId);
            pstmt.setTimestamp(2, Timestamp.valueOf(endTime));
            pstmt.setTimestamp(3, Timestamp.valueOf(startTime));
            pstmt.setInt(4, excludeShowtimeId != null ? excludeShowtimeId : 0);

            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt("conflict_count") > 0;

        } catch (SQLException e) {
            logger.error("Ошибка при проверке пересечения времени: {}", e.getMessage());
            return false;
        }
    }

    // Получить ВСЕ места зала для сеанса
    public List<Seat> getSeatsForShowtime(int showtimeId) {
        List<Seat> seats = new ArrayList<>();

        String query =
                "SELECT s.seat_id, s.seat_row, s.seat_number, s.category_id " +
                        "FROM seat s " +
                        "WHERE s.hall_id = (SELECT hall_id FROM showtime WHERE showtime_id = ?) " +
                        "ORDER BY s.seat_row, s.seat_number";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, showtimeId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Seat seat = new Seat();
                seat.setSeatId(rs.getInt("seat_id"));
                seat.setRow(rs.getInt("seat_row"));
                seat.setNumber(rs.getInt("seat_number"));
                seat.setCategoryId(rs.getInt("category_id"));
                seats.add(seat);
            }

        } catch (SQLException e) {
            logger.error("Ошибка при получении мест: {}", e.getMessage());
        }

        return seats;
    }

    // Получить занятые места
    public List<Integer> getTakenSeats(int showtimeId) {
        List<Integer> taken = new ArrayList<>();

        String query =
                "SELECT seat_id FROM ticket " +
                        "WHERE showtime_id = ? AND status_id IN (1, 2)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, showtimeId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                taken.add(rs.getInt("seat_id"));
            }

        } catch (SQLException e) {
            logger.error("Ошибка при получении занятых мест: {}", e.getMessage());
        }

        return taken;
    }

    private Showtime mapResultSetToShowtime(ResultSet rs) throws SQLException {
        Showtime showtime = new Showtime();
        showtime.setShowtimeId(rs.getInt("showtime_id"));
        showtime.setFilmId(rs.getInt("film_id"));
        showtime.setFilmTitle(rs.getString("film_title"));
        showtime.setHallId(rs.getInt("hall_id"));
        showtime.setHallName(rs.getString("hall_name"));
        showtime.setDateTime(rs.getTimestamp("date_time").toLocalDateTime());
        showtime.setBasePrice(rs.getDouble("base_price"));
        showtime.setRuleId(rs.getInt("rule_id"));
        showtime.setRuleName(rs.getString("rule_name"));
        showtime.setCoefficient(rs.getDouble("coefficient"));
        return showtime;
    }
}
