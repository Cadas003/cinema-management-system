package com.cinema.cinemamanagementsystem.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OperationRecord {
    private final LocalDateTime timestamp;
    private final String description;
    private final double amount;
    private final String ticketIds;

    public OperationRecord(LocalDateTime timestamp, String description, double amount, String ticketIds) {
        this.timestamp = timestamp;
        this.description = description;
        this.amount = amount;
        this.ticketIds = ticketIds;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public String getTicketIds() {
        return ticketIds;
    }

    public String getFormattedTime() {
        return timestamp.format(DateTimeFormatter.ofPattern("dd.MM HH:mm"));
    }
}
