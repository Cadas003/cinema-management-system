package com.cinema.cinemamanagementsystem.models;

import java.time.LocalDateTime;

public class Ticket {
    private int ticketId;
    private int showtimeId;
    private int seatId;
    private String seatInfo;
    private int customerId;
    private String customerName;
    private int userId;
    private String userName;
    private int statusId;
    private String statusName;
    private LocalDateTime createdAt;
    private double finalPrice;
    private boolean isBooked;

    // Конструкторы
    public Ticket() {}

    // Геттеры и сеттеры
    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }

    public int getShowtimeId() { return showtimeId; }
    public void setShowtimeId(int showtimeId) { this.showtimeId = showtimeId; }

    public int getSeatId() { return seatId; }
    public void setSeatId(int seatId) { this.seatId = seatId; }

    public String getSeatInfo() { return seatInfo; }
    public void setSeatInfo(String seatInfo) { this.seatInfo = seatInfo; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public int getStatusId() { return statusId; }
    public void setStatusId(int statusId) { this.statusId = statusId; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(double finalPrice) { this.finalPrice = finalPrice; }

    public boolean isBooked() { return isBooked; }
    public void setBooked(boolean booked) { isBooked = booked; }

    @Override
    public String toString() {
        return "Билет #" + ticketId + " - " + statusName +
                (customerName != null ? " (" + customerName + ")" : "");
    }
}