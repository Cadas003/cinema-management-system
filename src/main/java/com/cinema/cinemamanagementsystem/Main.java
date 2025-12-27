package com.cinema.cinemamanagementsystem;

import com.cinema.cinemamanagementsystem.controllers.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Запуск системы управления кинотеатром");

            // Загружаем окно авторизации
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cinema/cinemamanagementsystem/login.fxml"));
            Parent root = loader.load();

            // Получаем контроллер и передаем stage
            LoginController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);

            // Настраиваем главное окно
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/com/cinema/styles/styles.css")).toExternalForm());

            primaryStage.setTitle("Кинотеатр 'КиноСфера' - Система управления");
           // primaryStage.getIcons().add(new Image(
                  //  Objects.requireNonNull(getClass().getResourceAsStream("/com/cinema/styles/icon.png"))));
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(600);
            primaryStage.show();

            logger.info("Приложение успешно запущено");

        } catch (Exception e) {
            logger.error("Ошибка при запуске приложения: {}", e.getMessage(), e);
            showErrorAlert("Критическая ошибка", "Не удалось запустить приложение: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        logger.info("Завершение работы приложения");
        // Закрываем соединения с БД
        com.cinema.cinemamanagementsystem.dao.DatabaseConnection.closeDataSource();
    }

    private void showErrorAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        // Инициализируем логгер
        System.setProperty("logback.configurationFile", "logback.xml");

        // Запускаем JavaFX приложение
        launch(args);
    }
}
