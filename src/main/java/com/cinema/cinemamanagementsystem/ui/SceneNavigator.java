package com.cinema.cinemamanagementsystem.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneNavigator {
    private final Stage stage;

    public SceneNavigator(Stage stage) {
        this.stage = stage;
    }

    public void showLogin() {
        loadScene("/com/cinema/cinemamanagementsystem/ui/LoginView.fxml", "КиноСфера - Вход", 480, 320);
    }

    public void showAdminDashboard() {
        loadScene("/com/cinema/cinemamanagementsystem/ui/AdminDashboard.fxml", "КиноСфера - Администратор", 1200, 800);
    }

    public void showCashierDashboard() {
        loadScene("/com/cinema/cinemamanagementsystem/ui/CashierDashboard.fxml", "КиноСфера - Кассир", 1200, 800);
    }

    private void loadScene(String resource, String title, int width, int height) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(resource));
            stage.setTitle(title);
            stage.setScene(new Scene(root, width, height));
            stage.show();
        } catch (IOException ex) {
            throw new IllegalStateException("Не удалось загрузить сцену: " + resource, ex);
        }
    }
}
