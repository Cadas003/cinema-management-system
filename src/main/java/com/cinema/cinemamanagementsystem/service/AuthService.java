package com.cinema.cinemamanagementsystem.service;

import com.cinema.cinemamanagementsystem.dao.UserDao;
import com.cinema.cinemamanagementsystem.dao.UserRoleDao;
import com.cinema.cinemamanagementsystem.model.User;
import com.cinema.cinemamanagementsystem.model.UserRole;
import com.cinema.cinemamanagementsystem.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Optional;

public class AuthService {
    private final UserDao userDao = new UserDao();
    private final UserRoleDao userRoleDao = new UserRoleDao();

    public Optional<AuthResult> authenticate(String login, String password) throws SQLException {
        Optional<User> userOptional = userDao.findByLogin(login);
        if (userOptional.isEmpty()) {
            return Optional.empty();
        }
        User user = userOptional.get();
        if (!PasswordUtil.verify(password, user.passwordHash())) {
            return Optional.empty();
        }
        UserRole role = userRoleDao.findById(user.roleId()).orElse(null);
        return Optional.of(new AuthResult(user, role));
    }
}
