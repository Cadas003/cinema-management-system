package com.cinema.cinemamanagementsystem.services;

import com.cinema.cinemamanagementsystem.dao.PaymentDAO;
import com.cinema.cinemamanagementsystem.models.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentDAO paymentDAO = new PaymentDAO();

    /**
     * Создание платежа
     */
    public boolean createPayment(int ticketId, int userId, double amount, int methodId) {
        try {
            Payment payment = new Payment();
            payment.setTicketId(ticketId);
            payment.setUserId(userId);
            payment.setAmount(amount);
            payment.setMethodId(methodId);
            payment.setPaymentTime(LocalDateTime.now());

            boolean success = paymentDAO.createPayment(payment);

            if (success) {
                logger.info("Платеж создан: билет #{}, сумма: {} руб., метод: {}",
                        ticketId, amount, methodId);
            }

            return success;

        } catch (Exception e) {
            logger.error("Ошибка при создании платежа: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Создание возврата платежа
     */
    public boolean createRefund(int ticketId, int userId, double amount, int methodId) {
        try {
            // Возврат - отрицательная сумма
            Payment refund = new Payment();
            refund.setTicketId(ticketId);
            refund.setUserId(userId);
            refund.setAmount(-Math.abs(amount));
            refund.setMethodId(methodId);
            refund.setPaymentTime(LocalDateTime.now());

            boolean success = paymentDAO.createPayment(refund);

            if (success) {
                logger.info("Возврат создан: билет #{}, сумма: {} руб.", ticketId, amount);
            }

            return success;

        } catch (Exception e) {
            logger.error("Ошибка при создании возврата: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Получение платежей за период
     */
    public List<Payment> getPaymentsByPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            return paymentDAO.getPaymentsByDateRange(startDate, endDate);
        } catch (Exception e) {
            logger.error("Ошибка при получении платежей: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Расчет выручки за период
     */
    public double calculateRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            return paymentDAO.getTotalRevenue(startDate, endDate);
        } catch (Exception e) {
            logger.error("Ошибка при расчете выручки: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Получение статистики по методам оплаты
     */
    public String getPaymentMethodsStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<Payment> payments = paymentDAO.getPaymentsByDateRange(startDate, endDate);

            long cardCount = payments.stream()
                    .filter(p -> p.getMethodId() == 1 && p.getAmount() > 0)
                    .count();

            long cashCount = payments.stream()
                    .filter(p -> p.getMethodId() == 2 && p.getAmount() > 0)
                    .count();

            long sbpCount = payments.stream()
                    .filter(p -> p.getMethodId() == 3 && p.getAmount() > 0)
                    .count();

            double cardAmount = payments.stream()
                    .filter(p -> p.getMethodId() == 1 && p.getAmount() > 0)
                    .mapToDouble(Payment::getAmount)
                    .sum();

            double cashAmount = payments.stream()
                    .filter(p -> p.getMethodId() == 2 && p.getAmount() > 0)
                    .mapToDouble(Payment::getAmount)
                    .sum();

            double sbpAmount = payments.stream()
                    .filter(p -> p.getMethodId() == 3 && p.getAmount() > 0)
                    .mapToDouble(Payment::getAmount)
                    .sum();

            return String.format(
                    "Банковская карта: %d операций (%.2f руб.)\n" +
                            "Наличные: %d операций (%.2f руб.)\n" +
                            "СБП: %d операций (%.2f руб.)",
                    cardCount, cardAmount, cashCount, cashAmount, sbpCount, sbpAmount
            );

        } catch (Exception e) {
            logger.error("Ошибка при получении статистики методов оплаты: {}", e.getMessage());
            return "Не удалось получить статистику";
        }
    }

    /**
     * Проверка, был ли платеж возвращен
     */
    public boolean isTicketRefunded(int ticketId) {
        try {
            // Получаем все платежи по билету
            List<Payment> payments = getPaymentsByPeriod(
                    LocalDateTime.now().minusYears(1),
                    LocalDateTime.now()
            );

            return payments.stream()
                    .anyMatch(p -> p.getTicketId() == ticketId && p.getAmount() < 0);

        } catch (Exception e) {
            logger.error("Ошибка при проверке возврата: {}", e.getMessage());
            return false;
        }
    }
}
