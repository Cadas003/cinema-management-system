package com.cinema.cinemamanagementsystem.model;

import java.time.LocalDateTime;

public record Ticket(long ticketId, int showtimeId, int seatId, Integer customerId, int userId, int statusId,
                     LocalDateTime createdAt) {
}
