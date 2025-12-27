package com.cinema.cinemamanagementsystem.services;

import com.cinema.cinemamanagementsystem.dao.UserDAO;
import com.cinema.cinemamanagementsystem.models.User;
import com.cinema.cinemamanagementsystem.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserDAO userDAO = new UserDAO();
    private User currentUser;
    private final Map<String, String> sessionTokens = new HashMap<>();

    /**
     * Аутентификация пользователя
     */
    public User authenticate(String login, String password) {
        try {
            User user = userDAO.authenticate(login, password);

            if (user != null) {
                this.currentUser = user;
                String sessionToken = generateSessionToken(user);
                sessionTokens.put(sessionToken, user.getLogin());

                logger.info("Пользователь {} успешно аутентифицирован", user.getFullName());
                return user;
            }

            logger.warn("Неудачная попытка входа с логином: {}", login);
            return null;

        } catch (Exception e) {
            logger.error("Ошибка при аутентификации: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Выход из системы
     */
    public void logout(String sessionToken) {
        if (sessionTokens.containsKey(sessionToken)) {
            String login = sessionTokens.remove(sessionToken);
            logger.info("Пользователь {} вышел из системы", login);
        }
        this.currentUser = null;
    }

    /**
     * Проверка сессии
     */
    public boolean validateSession(String sessionToken) {
        return sessionTokens.containsKey(sessionToken);
    }

    /**
     * Получение текущего пользователя
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Проверка прав доступа
     */
    public boolean hasPermission(String permission) {
        if (currentUser == null) return false;

        switch (permission) {
            case "manage_films":
            case "manage_showtimes":
            case "manage_users":
            case "view_reports":
                return currentUser.isAdmin();

            case "sell_tickets":
            case "manage_bookings":
            case "process_refunds":
                return currentUser.isCashier() || currentUser.isAdmin();

            case "view_customers":
                return true; // Все авторизованные пользователи

            default:
                return false;
        }
    }

    /**
     * Получение всех пользователей (только для админов)
     */
    public List<User> getAllUsers() {
        if (currentUser != null && currentUser.isAdmin()) {
            return userDAO.getAllUsers();
        }
        return List.of();
    }

    /**
     * Добавление нового пользователя
     */
    public boolean addUser(User user, String password) {
        if (currentUser != null && currentUser.isAdmin()) {
            // В реальной системе здесь должно быть хеширование пароля
            user.setPasswordHash(password); // TODO: Заменить на хеширование
            return userDAO.addUser(user);
        }
        return false;
    }

    /**
     * Обновление пользователя
     */
    public boolean updateUser(User user) {
        if (currentUser != null && currentUser.isAdmin()) {
            return userDAO.updateUser(user);
        }
        return false;
    }

    /**
     * Смена пароля
     */
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        // Проверяем старый пароль
        User user = userDAO.authenticate(currentUser.getLogin(), oldPassword);
        if (user == null && !currentUser.isAdmin()) {
            return false;
        }

        // В реальной системе здесь должно быть хеширование
        String newPasswordHash = newPassword; // TODO: Заменить на хеширование
        return userDAO.changePassword(userId, newPasswordHash);
    }

    /**
     * Генерация токена сессии
     */
    private String generateSessionToken(User user) {
        return user.getUserId() + "_" +
                System.currentTimeMillis() + "_" +
                java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Проверка необходимости смены пароля (например, при первом входе)
     */
    public boolean isPasswordChangeRequired() {
        // Здесь можно добавить логику, например:
        // - Проверка, что пароль не менялся более 90 дней
        // - Проверка, что это первый вход
        return false;
    }
}
