package com.cinema.cinemamanagementsystem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Payment(long paymentId, long ticketId, int userId, BigDecimal amount, int methodId,
                      LocalDateTime paymentTime) {
}
