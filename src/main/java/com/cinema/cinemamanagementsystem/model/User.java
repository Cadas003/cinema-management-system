package com.cinema.cinemamanagementsystem.model;

public record User(int userId, String username, String passwordHash, int roleId, String login) {
}
