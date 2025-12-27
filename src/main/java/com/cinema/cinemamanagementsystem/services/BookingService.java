package com.cinema.cinemamanagementsystem.services;

import com.cinema.cinemamanagementsystem.dao.*;
import com.cinema.cinemamanagementsystem.models.*;
import com.cinema.cinemamanagementsystem.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class BookingService {
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final TicketDAO ticketDAO = new TicketDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    // Продажа билета (без бронирования)
    public Ticket sellTicket(int showtimeId, int seatId, int userId,
                             String customerName, String customerPhone, String customerEmail) {
        try {
            // 1. Проверяем доступность места
            List<Integer> takenSeats = showtimeDAO.getTakenSeats(showtimeId);
            if (takenSeats.contains(seatId)) {
                throw new IllegalStateException("Место уже занято или не существует");
            }


            // 2. Находим или создаем клиента (если клиент регистрируется)
            Customer customer = null;
            if (hasCustomerData(customerName, customerPhone)) {
                customer = customerDAO.findOrCreateCustomer(customerName, customerPhone, customerEmail);
                if (customer == null) {
                    throw new IllegalStateException("Не удалось создать клиента");
                }
            }

            // 3. Создаем билет со статусом "оплачен"
            Ticket ticket = new Ticket();
            ticket.setShowtimeId(showtimeId);
            ticket.setSeatId(seatId);
            ticket.setCustomerId(customer != null ? customer.getCustomerId() : 0);
            ticket.setUserId(userId);
            ticket.setStatusId(DatabaseConfig.TicketStatus.PAID);
            ticket.setCreatedAt(LocalDateTime.now());

            int ticketId = ticketDAO.createTicket(ticket);
            if (ticketId == -1) {
                throw new IllegalStateException("Не удалось создать билет");
            }

            ticket.setTicketId(ticketId);

            // 4. Создаем запись о платеже
            Payment payment = new Payment();
            payment.setTicketId(ticketId);
            payment.setUserId(userId);
            payment.setAmount(calculateFinalPrice(showtimeId, false, customer != null));
            payment.setPaymentTime(LocalDateTime.now());
            payment.setMethodId(1); // Банковская карта по умолчанию

            if (!paymentDAO.createPayment(payment)) {
                throw new IllegalStateException("Не удалось зарегистрировать платеж");
            }

            logger.info("Билет #{} успешно продан клиенту {}", ticketId,
                    customer != null ? customer.getName() : "без регистрации");
            return ticket;

        } catch (Exception e) {
            logger.error("Ошибка при продаже билета: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // Бронирование билета
    public Ticket bookTicket(int showtimeId, int seatId, int userId,
                             String customerName, String customerPhone, String customerEmail) {
        try {
            // 1. Проверяем доступность места
            List<Integer> takenSeats = showtimeDAO.getTakenSeats(showtimeId);
            if (takenSeats.contains(seatId)) {
                throw new IllegalStateException("Место уже занято или не существует");
            }


            // 2. Находим или создаем клиента (если клиент регистрируется)
            Customer customer = null;
            if (hasCustomerData(customerName, customerPhone)) {
                customer = customerDAO.findOrCreateCustomer(customerName, customerPhone, customerEmail);
                if (customer == null) {
                    throw new IllegalStateException("Не удалось создать клиента");
                }
            }

            // 3. Создаем билет со статусом "забронирован"
            Ticket ticket = new Ticket();
            ticket.setShowtimeId(showtimeId);
            ticket.setSeatId(seatId);
            ticket.setCustomerId(customer != null ? customer.getCustomerId() : 0);
            ticket.setUserId(userId);
            ticket.setStatusId(DatabaseConfig.TicketStatus.BOOKED);
            ticket.setCreatedAt(LocalDateTime.now());
            ticket.setBooked(true);

            int ticketId = ticketDAO.createTicket(ticket);
            if (ticketId == -1) {
                throw new IllegalStateException("Не удалось создать бронирование");
            }

            ticket.setTicketId(ticketId);

            logger.info("Бронирование #{} создано для клиента {}", ticketId,
                    customer != null ? customer.getName() : "без регистрации");
            return ticket;

        } catch (Exception e) {
            logger.error("Ошибка при бронировании: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // Подтверждение бронирования (оплата)
    public boolean confirmBooking(int ticketId, int userId, int paymentMethodId) {
        try {
            // 1. Получаем бронирование
            Ticket ticket = ticketDAO.getTicketById(ticketId);
            if (ticket == null || ticket.getStatusId() != DatabaseConfig.TicketStatus.BOOKED) {
                throw new IllegalStateException("Бронирование не найдено или уже обработано");
            }

            // 2. Проверяем, не истекло ли время брони
            LocalDateTime bookingExpiry = ticket.getCreatedAt()
                    .plusMinutes(DatabaseConfig.BOOKING_TIMEOUT_MINUTES);

            if (LocalDateTime.now().isAfter(bookingExpiry)) {
                ticketDAO.updateTicketStatus(ticketId, DatabaseConfig.TicketStatus.REFUND);
                throw new IllegalStateException("Время бронирования истекло");
            }

            // 3. Обновляем статус на "оплачен"
            if (!ticketDAO.updateTicketStatus(ticketId, DatabaseConfig.TicketStatus.PAID)) {
                throw new IllegalStateException("Не удалось обновить статус билета");
            }

            // 4. Создаем платеж с доплатой 15%
            double finalPrice = calculateFinalPrice(ticket.getShowtimeId(), true, ticket.getCustomerId() > 0);

            Payment payment = new Payment();
            payment.setTicketId(ticketId);
            payment.setUserId(userId);
            payment.setAmount(finalPrice);
            payment.setMethodId(paymentMethodId);
            payment.setPaymentTime(LocalDateTime.now());

            if (!paymentDAO.createPayment(payment)) {
                // Откатываем статус, если платеж не прошел
                ticketDAO.updateTicketStatus(ticketId, DatabaseConfig.TicketStatus.BOOKED);
                throw new IllegalStateException("Не удалось зарегистрировать платеж");
            }

            logger.info("Бронирование #{} подтверждено, сумма оплаты: {}", ticketId, finalPrice);
            return true;

        } catch (Exception e) {
            logger.error("Ошибка при подтверждении бронирования: {}", e.getMessage());
            return false;
        }
    }

    // Возврат билета
    public boolean refundTicket(int ticketId, int userId) {
        try {
            Ticket ticket = ticketDAO.getTicketById(ticketId);
            if (ticket == null || ticket.getStatusId() != DatabaseConfig.TicketStatus.PAID) {
                throw new IllegalStateException("Билет не найден или не может быть возвращен");
            }

            // Проверяем, не начался ли сеанс
            Showtime showtime = showtimeDAO.getAllShowtimes().stream()
                    .filter(s -> s.getShowtimeId() == ticket.getShowtimeId())
                    .findFirst()
                    .orElse(null);

            if (showtime != null && LocalDateTime.now().isAfter(showtime.getDateTime())) {
                throw new IllegalStateException("Возврат невозможен: сеанс уже начался");
            }

            // Обновляем статус на "возврат"
            if (!ticketDAO.updateTicketStatus(ticketId, DatabaseConfig.TicketStatus.REFUND)) {
                throw new IllegalStateException("Не удалось обновить статус билета");
            }

            // Создаем запись о возврате платежа (отрицательный платеж)
            Payment refund = new Payment();
            refund.setTicketId(ticketId);
            refund.setUserId(userId);
            refund.setAmount(-ticket.getFinalPrice()); // Отрицательная сумма для возврата
            refund.setMethodId(1); // Метод возврата
            refund.setPaymentTime(LocalDateTime.now());

            paymentDAO.createPayment(refund);

            logger.info("Билет #{} возвращен", ticketId);
            return true;

        } catch (Exception e) {
            logger.error("Ошибка при возврате билета: {}", e.getMessage());
            return false;
        }
    }

    // Расчет итоговой цены
    public double calculateFinalPrice(int showtimeId, boolean isBooking, boolean isRegisteredCustomer) {
        Showtime showtime = showtimeDAO.getShowtimeById(showtimeId);
        if (showtime == null) {
            throw new IllegalStateException("Сеанс не найден для расчета цены");
        }

        double coefficient = isRegisteredCustomer
                ? showtime.getCoefficient()
                : DatabaseConfig.GUEST_PRICE_COEFFICIENT;

        if (coefficient == 0) {
            coefficient = 1.0;
        }

        double finalPrice = showtime.getBasePrice() * coefficient;

        if (isBooking) {
            finalPrice *= (1 + DatabaseConfig.BOOKING_SURCHARGE_RATE);
        }

        return finalPrice;
    }

    private boolean hasCustomerData(String customerName, String customerPhone) {
        return customerName != null && !customerName.trim().isEmpty()
                && customerPhone != null && !customerPhone.trim().isEmpty();
    }

    // Автоматическая отмена истекших бронирований
    public void cancelExpiredBookings() {
        int canceledCount = ticketDAO.cancelExpiredBookings();
        if (canceledCount > 0) {
            logger.info("Автоматически отменено {} истекших бронирований", canceledCount);
        }
    }
}
