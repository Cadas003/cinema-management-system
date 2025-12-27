package com.cinema.cinemamanagementsystem.service;

import com.cinema.cinemamanagementsystem.model.User;
import com.cinema.cinemamanagementsystem.model.UserRole;

public record AuthResult(User user, UserRole role) {
}
