package com.cinema.cinemamanagementsystem.models;

import java.time.LocalDateTime;

public class Payment {
    private int paymentId;
    private int ticketId;
    private int userId;
    private String userName;
    private double amount;
    private int methodId;
    private String methodName;
    private LocalDateTime paymentTime;

    // Конструкторы
    public Payment() {}

    public Payment(int ticketId, int userId, double amount, int methodId, LocalDateTime paymentTime) {
        this.ticketId = ticketId;
        this.userId = userId;
        this.amount = amount;
        this.methodId = methodId;
        this.paymentTime = paymentTime;
    }

    // Геттеры и сеттеры
    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }

    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public int getMethodId() { return methodId; }
    public void setMethodId(int methodId) { this.methodId = methodId; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public LocalDateTime getPaymentTime() { return paymentTime; }
    public void setPaymentTime(LocalDateTime paymentTime) { this.paymentTime = paymentTime; }

    @Override
    public String toString() {
        return String.format("Платеж #%d: %.2f руб. (%s)",
                paymentId, amount, methodName);
    }
}