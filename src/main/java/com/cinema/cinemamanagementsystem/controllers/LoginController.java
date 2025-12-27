package com.cinema.cinemamanagementsystem.controllers;

import com.cinema.cinemamanagementsystem.dao.UserDAO;
import com.cinema.cinemamanagementsystem.models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private Stage primaryStage;
    private final UserDAO userDAO = new UserDAO();

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    @FXML
    private void initialize() {
        // Устанавливаем обработчик нажатия Enter
        passwordField.setOnAction(event -> handleLogin());
        loginButton.setOnAction(event -> handleLogin());

        // Тестовые данные (можно удалить в продакшене)
        loginField.setText("SAV");
        passwordField.setText("hash12345");
    }

    @FXML
    private void handleLogin() {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();

        if (login.isEmpty() || password.isEmpty()) {
            showError("Введите логин и пароль");
            return;
        }

        try {
            User user = userDAO.authenticate(login, password);

            if (user == null) {
                showError("Неверный логин или пароль");
                return;
            }

            logger.info("Пользователь {} успешно авторизован", user.getFullName());

            // Загружаем соответствующую панель в зависимости от роли
            if (user.isAdmin()) {
                loadAdminPanel(user);
            } else if (user.isCashier()) {
                loadCashierPanel(user);
            } else {
                showError("Неизвестная роль пользователя");
            }

        } catch (Exception e) {
            logger.error("Ошибка при авторизации: {}", e.getMessage());
            showError("Ошибка при подключении к базе данных");
        }
    }

    private void loadAdminPanel(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cinema/fxml/admin_dashboard.fxml"));
            Parent root = loader.load();

            AdminController controller = loader.getController();
            controller.setCurrentUser(user);
            controller.setPrimaryStage(primaryStage);
            controller.initializeData();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/com/cinema/styles/styles.css")).toExternalForm());

            primaryStage.setTitle("Панель администратора - Кинотеатр 'КиноСфера'");
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();

        } catch (Exception e) {
            logger.error("Ошибка при загрузке панели администратора: {}", e.getMessage(), e);
            showError("Не удалось загрузить интерфейс администратора");
        }
    }

    private void loadCashierPanel(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cinema/cinemamanagementsystem/cashier_dashboard.fxml"));
            Parent root = loader.load();

            CashierController controller = loader.getController();
            controller.setCurrentUser(user);
            controller.setPrimaryStage(primaryStage);
            controller.initializeData();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/com/cinema/styles/styles.css")).toExternalForm());

            primaryStage.setTitle("Панель кассира - Кинотеатр 'КиноСфера'");
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();

        } catch (Exception e) {
            logger.error("Ошибка при загрузке панели кассира: {}", e.getMessage(), e);
            showError("Не удалось загрузить интерфейс кассира");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    @FXML
    private void clearError() {
        errorLabel.setVisible(false);
    }
}
