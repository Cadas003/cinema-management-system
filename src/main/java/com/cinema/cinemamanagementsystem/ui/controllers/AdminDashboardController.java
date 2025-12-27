package com.cinema.cinemamanagementsystem.ui.controllers;

import com.cinema.cinemamanagementsystem.service.ReportService;
import com.cinema.cinemamanagementsystem.service.ReportSummary;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminDashboardController {
    @FXML
    private TextField reportDateField;

    @FXML
    private Label paidLabel;

    @FXML
    private Label reservedLabel;

    @FXML
    private Label paymentsLabel;

    @FXML
    private Label revenueLabel;

    private final ReportService reportService = new ReportService();

    @FXML
    public void initialize() {
        reportDateField.setText(LocalDate.now().toString());
    }

    @FXML
    public void handleLoadReport() {
        try {
            LocalDate date = LocalDate.parse(reportDateField.getText());
            LocalDateTime from = date.atStartOfDay();
            LocalDateTime to = date.plusDays(1).atStartOfDay().minusSeconds(1);
            ReportSummary summary = reportService.summary(from, to);
            paidLabel.setText(String.valueOf(summary.paidTickets()));
            reservedLabel.setText(String.valueOf(summary.reservations()));
            paymentsLabel.setText(String.valueOf(summary.payments()));
            revenueLabel.setText(summary.revenue().toString());
        } catch (SQLException ex) {
            showAlert("Ошибка отчёта: " + ex.getMessage());
        } catch (Exception ex) {
            showAlert("Неверная дата. Используйте формат YYYY-MM-DD");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
