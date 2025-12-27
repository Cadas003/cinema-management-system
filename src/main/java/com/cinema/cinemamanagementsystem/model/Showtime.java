package com.cinema.cinemamanagementsystem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Showtime(int showtimeId, int filmId, int hallId, LocalDateTime dateTime, BigDecimal basePrice, Integer ruleId) {
}
