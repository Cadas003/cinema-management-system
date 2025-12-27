package com.cinema.cinemamanagementsystem.ui;

import com.cinema.cinemamanagementsystem.dao.DatabaseConnection;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.sql.Connection;

public class ConnectionStatusMonitor {
    public Timeline attach(Label label) {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0), event -> refresh(label)),
                new KeyFrame(Duration.seconds(20)));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        return timeline;
    }

    private void refresh(Label label) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                try (Connection connection = DatabaseConnection.getConnection()) {
                    return connection != null && !connection.isClosed();
                } catch (Exception e) {
                    return false;
                }
            }
        };

        task.setOnSucceeded(event -> updateLabel(label, task.getValue()));
        task.setOnFailed(event -> updateLabel(label, false));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void updateLabel(Label label, boolean online) {
        label.getStyleClass().removeAll("status-online", "status-offline");
        label.getStyleClass().add(online ? "status-online" : "status-offline");
        label.setText(online ? "Онлайн" : "Нет соединения");
    }
}
