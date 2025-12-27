package com.cinema.cinemamanagementsystem.service;

import com.cinema.cinemamanagementsystem.dao.PaymentDao;
import com.cinema.cinemamanagementsystem.dao.PriceRuleDao;
import com.cinema.cinemamanagementsystem.dao.ShowtimeDao;
import com.cinema.cinemamanagementsystem.dao.TicketDao;
import com.cinema.cinemamanagementsystem.dao.TicketStatusDao;
import com.cinema.cinemamanagementsystem.db.DataSourceProvider;
import com.cinema.cinemamanagementsystem.model.Payment;
import com.cinema.cinemamanagementsystem.model.PriceRule;
import com.cinema.cinemamanagementsystem.model.Showtime;
import com.cinema.cinemamanagementsystem.model.Ticket;
import com.cinema.cinemamanagementsystem.util.MoneyUtil;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class TicketService {
    private static final BigDecimal RESERVATION_SURCHARGE = new BigDecimal("1.15");

    private final DataSource dataSource = DataSourceProvider.getDataSource();
    private final TicketDao ticketDao = new TicketDao();
    private final PaymentDao paymentDao = new PaymentDao();
    private final ShowtimeDao showtimeDao = new ShowtimeDao();
    private final PriceRuleDao priceRuleDao = new PriceRuleDao();
    private final TicketStatusDao ticketStatusDao = new TicketStatusDao();

    public void reserveTickets(int showtimeId, List<Integer> seatIds, Integer customerId, int userId) throws SQLException {
        int reservedStatusId = statusId("забронирован");
        ensureSeatsAvailable(showtimeId, seatIds);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for (Integer seatId : seatIds) {
                    Ticket ticket = new Ticket(0, showtimeId, seatId, customerId, userId, reservedStatusId,
                            LocalDateTime.now());
                    ticketDao.create(connection, ticket);
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void confirmReservation(long ticketId, int userId, int paymentMethodId) throws SQLException {
        int paidStatusId = statusId("оплачен");
        int reservedStatusId = statusId("забронирован");
        Ticket ticket = ticketDao.findById(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("Билет не найден");
        }
        if (ticket.statusId() != reservedStatusId) {
            throw new IllegalStateException("Билет не находится в статусе брони");
        }
        if (ticket.createdAt() != null && ticket.createdAt().isBefore(LocalDateTime.now().minusMinutes(30))) {
            throw new IllegalStateException("Бронь просрочена и должна быть отменена");
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BigDecimal amount = calculatePrice(ticketId, true);
                ticketDao.updateStatus(connection, ticketId, paidStatusId);
                Payment payment = new Payment(0, ticketId, userId, amount, paymentMethodId, LocalDateTime.now());
                paymentDao.create(connection, payment);
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void directPurchase(int showtimeId, int seatId, Integer customerId, int userId, int paymentMethodId)
            throws SQLException {
        int paidStatusId = statusId("оплачен");
        ensureSeatsAvailable(showtimeId, List.of(seatId));
        BigDecimal amount = calculatePrice(showtimeId, false);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Ticket ticket = new Ticket(0, showtimeId, seatId, customerId, userId, paidStatusId,
                        LocalDateTime.now());
                long ticketId = ticketDao.create(connection, ticket);
                Payment payment = new Payment(0, ticketId, userId, amount, paymentMethodId, LocalDateTime.now());
                paymentDao.create(connection, payment);
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void refundTicket(long ticketId) throws SQLException {
        int refundStatusId = statusId("возврат");
        try (Connection connection = dataSource.getConnection()) {
            ticketDao.updateStatus(connection, ticketId, refundStatusId);
        }
    }

    public int cancelExpiredReservations() throws SQLException {
        int reservedStatusId = statusId("забронирован");
        int cancelledStatusId = statusId("отменён");
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        List<Ticket> expired = ticketDao.findExpiredReservations(threshold, reservedStatusId);
        if (expired.isEmpty()) {
            return 0;
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for (Ticket ticket : expired) {
                    ticketDao.updateStatus(connection, ticket.ticketId(), cancelledStatusId);
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
        return expired.size();
    }

    public List<Integer> occupiedSeats(int showtimeId) throws SQLException {
        List<Integer> excluded = List.of(statusId("отменён"), statusId("возврат"));
        return ticketDao.findOccupiedSeatIds(showtimeId, excluded);
    }

    private BigDecimal calculatePrice(long ticketId, boolean withSurcharge) throws SQLException {
        String sql = "SELECT showtime_id FROM ticket WHERE ticket_id = ?";
        try (Connection connection = dataSource.getConnection();
             java.sql.PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ticketId);
            try (java.sql.ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    int showtimeId = rs.getInt("showtime_id");
                    return calculatePrice(showtimeId, withSurcharge);
                }
            }
        }
        throw new IllegalArgumentException("Билет не найден");
    }

    private BigDecimal calculatePrice(int showtimeId, boolean withSurcharge) throws SQLException {
        Showtime showtime = showtimeDao.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Сеанс не найден"));
        BigDecimal coefficient = BigDecimal.ONE;
        if (showtime.ruleId() != null) {
            PriceRule rule = priceRuleDao.findById(showtime.ruleId()).orElse(null);
            if (rule != null && rule.coefficient() != null) {
                coefficient = rule.coefficient();
            }
        }
        BigDecimal base = showtime.basePrice().multiply(coefficient);
        if (withSurcharge) {
            base = base.multiply(RESERVATION_SURCHARGE);
        }
        return MoneyUtil.round(base);
    }

    private int statusId(String name) throws SQLException {
        return ticketStatusDao.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Статус не найден: " + name))
                .statusId();
    }

    private void ensureSeatsAvailable(int showtimeId, List<Integer> seatIds) throws SQLException {
        List<Integer> excluded = List.of(statusId("отменён"), statusId("возврат"));
        List<Integer> occupied = ticketDao.findOccupiedSeatIds(showtimeId, excluded);
        for (Integer seatId : seatIds) {
            if (occupied.contains(seatId)) {
                throw new IllegalStateException("Место уже занято: " + seatId);
            }
        }
    }
}
