package com.cinema.cinemamanagementsystem.models;

import java.time.LocalDateTime;

public class Customer {

    // Поля из таблицы customer
    private int customerId;        // customer_id
    private String name;           // name
    private String phone;          // phone
    private String email;          // email
    private boolean registered;    // registered (TINYINT 0/1)
    private LocalDateTime createdAt; // created_at

    // Дополнительные поля (оставляем!)
    private int visitCount;        // количество посещений
    private double totalSpent;     // сумма покупок

    // Конструктор по умолчанию
    public Customer() {}

    // Конструктор для создания нового клиента вручную
    public Customer(String name, String phone, String email) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.registered = true;
        this.createdAt = LocalDateTime.now();
        this.visitCount = 0;
        this.totalSpent = 0.0;
    }

    // Геттеры и сеттеры
    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(int visitCount) {
        this.visitCount = visitCount;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    @Override
    public String toString() {
        return name + (phone != null ? " (" + phone + ")" : "");
    }
}
