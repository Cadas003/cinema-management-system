package com.cinema.cinemamanagementsystem.ui.controllers;

import com.cinema.cinemamanagementsystem.service.AuthResult;
import com.cinema.cinemamanagementsystem.service.AuthService;
import com.cinema.cinemamanagementsystem.ui.SceneNavigator;
import com.cinema.cinemamanagementsystem.util.ValidationUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

public class LoginController {
    @FXML
    private TextField loginField;

    @FXML
    private PasswordField passwordField;

    private final AuthService authService = new AuthService();

    @FXML
    public void handleLogin(ActionEvent event) {
        String login = loginField.getText();
        String password = passwordField.getText();
        if (ValidationUtil.isBlank(login) || ValidationUtil.isBlank(password)) {
            showAlert(Alert.AlertType.WARNING, "Введите логин и пароль");
            return;
        }
        try {
            Optional<AuthResult> result = authService.authenticate(login, password);
            if (result.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Неверный логин или пароль");
                return;
            }
            SceneNavigator navigator = new SceneNavigator((Stage) loginField.getScene().getWindow());
            String roleName = result.get().role() != null ? result.get().role().name() : "";
            String normalized = roleName.toLowerCase(Locale.ROOT);
            if (normalized.contains("админ")) {
                navigator.showAdminDashboard();
            } else {
                navigator.showCashierDashboard();
            }
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Ошибка подключения к БД: " + ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
