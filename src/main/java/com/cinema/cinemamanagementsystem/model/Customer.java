package com.cinema.cinemamanagementsystem.model;

import java.time.LocalDateTime;

public record Customer(int customerId, String name, String phone, String email, boolean registered, LocalDateTime createdAt) {
}
