package com.cinema.cinemamanagementsystem;

import com.cinema.cinemamanagementsystem.service.TicketService;
import com.cinema.cinemamanagementsystem.ui.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CinemaApplication extends Application {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final TicketService ticketService = new TicketService();

    @Override
    public void start(Stage stage) {
        SceneNavigator navigator = new SceneNavigator(stage);
        navigator.showLogin();
        scheduler.scheduleAtFixedRate(this::cancelExpiredReservations, 1, 1, TimeUnit.MINUTES);
    }

    private void cancelExpiredReservations() {
        try {
            ticketService.cancelExpiredReservations();
        } catch (SQLException ignored) {
        }
    }

    @Override
    public void stop() {
        scheduler.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
