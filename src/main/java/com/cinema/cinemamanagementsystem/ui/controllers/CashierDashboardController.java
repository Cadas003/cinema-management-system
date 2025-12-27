package com.cinema.cinemamanagementsystem.ui.controllers;

import com.cinema.cinemamanagementsystem.service.TicketService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.util.List;

public class CashierDashboardController {
    @FXML
    private TextField showtimeIdField;

    @FXML
    private TextField seatIdField;

    @FXML
    private TextField customerIdField;

    @FXML
    private TextField userIdField;

    @FXML
    private TextField paymentMethodField;

    @FXML
    private TextField ticketIdField;

    @FXML
    private TextArea occupiedSeatsArea;

    private final TicketService ticketService = new TicketService();

    @FXML
    public void handleLoadOccupiedSeats() {
        try {
            int showtimeId = Integer.parseInt(showtimeIdField.getText());
            List<Integer> occupied = ticketService.occupiedSeats(showtimeId);
            occupiedSeatsArea.setText(occupied.toString());
        } catch (Exception ex) {
            showAlert("Ошибка загрузки мест: " + ex.getMessage());
        }
    }

    @FXML
    public void handleReserveTicket() {
        try {
            int showtimeId = Integer.parseInt(showtimeIdField.getText());
            int seatId = Integer.parseInt(seatIdField.getText());
            Integer customerId = parseNullableInt(customerIdField.getText());
            int userId = Integer.parseInt(userIdField.getText());
            ticketService.reserveTickets(showtimeId, List.of(seatId), customerId, userId);
            showInfo("Бронь создана");
        } catch (SQLException ex) {
            showAlert("Ошибка брони: " + ex.getMessage());
        }
    }

    @FXML
    public void handleDirectPurchase() {
        try {
            int showtimeId = Integer.parseInt(showtimeIdField.getText());
            int seatId = Integer.parseInt(seatIdField.getText());
            Integer customerId = parseNullableInt(customerIdField.getText());
            int userId = Integer.parseInt(userIdField.getText());
            int methodId = Integer.parseInt(paymentMethodField.getText());
            ticketService.directPurchase(showtimeId, seatId, customerId, userId, methodId);
            showInfo("Оплата прошла успешно");
        } catch (SQLException ex) {
            showAlert("Ошибка оплаты: " + ex.getMessage());
        }
    }

    @FXML
    public void handleConfirmReservation() {
        try {
            long ticketId = Long.parseLong(ticketIdField.getText());
            int userId = Integer.parseInt(userIdField.getText());
            int methodId = Integer.parseInt(paymentMethodField.getText());
            ticketService.confirmReservation(ticketId, userId, methodId);
            showInfo("Бронь оплачена");
        } catch (SQLException ex) {
            showAlert("Ошибка оплаты брони: " + ex.getMessage());
        }
    }

    @FXML
    public void handleRefund() {
        try {
            long ticketId = Long.parseLong(ticketIdField.getText());
            ticketService.refundTicket(ticketId);
            showInfo("Возврат выполнен");
        } catch (SQLException ex) {
            showAlert("Ошибка возврата: " + ex.getMessage());
        }
    }

    private Integer parseNullableInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
