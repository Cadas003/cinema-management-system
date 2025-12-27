package com.cinema.cinemamanagementsystem.service;

import com.cinema.cinemamanagementsystem.dao.UserDao;
import com.cinema.cinemamanagementsystem.model.User;
import com.cinema.cinemamanagementsystem.util.PasswordUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UserService {
    private final UserDao userDao = new UserDao();

    public List<User> findAll() throws SQLException {
        return userDao.findAll();
    }

    public Optional<User> findByLogin(String login) throws SQLException {
        return userDao.findByLogin(login);
    }

    public int createUser(String username, String login, String plainPassword, int roleId) throws SQLException {
        String hash = PasswordUtil.hash(plainPassword);
        User user = new User(0, username, hash, roleId, login);
        return userDao.create(user);
    }

    public void updateUser(User user, String plainPassword) throws SQLException {
        String hash = plainPassword == null ? user.passwordHash() : PasswordUtil.hash(plainPassword);
        User updated = new User(user.userId(), user.username(), hash, user.roleId(), user.login());
        userDao.update(updated);
    }

    public void deleteUser(int userId) throws SQLException {
        userDao.delete(userId);
    }
}
