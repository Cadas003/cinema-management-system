package com.cinema.cinemamanagementsystem.service;

import java.math.BigDecimal;

public record ReportSummary(int paidTickets, int reservations, int payments, BigDecimal revenue) {
}
