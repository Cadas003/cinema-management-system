package com.cinema.cinemamanagementsystem.ui.controllers;

import com.cinema.cinemamanagementsystem.dao.SeatCategoryDao;
import com.cinema.cinemamanagementsystem.dao.SeatDao;
import com.cinema.cinemamanagementsystem.dao.ShowtimeDao;
import com.cinema.cinemamanagementsystem.model.Seat;
import com.cinema.cinemamanagementsystem.model.SeatCategory;
import com.cinema.cinemamanagementsystem.model.Showtime;
import com.cinema.cinemamanagementsystem.service.TicketService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @FXML
    private GridPane seatMapGrid;

    @FXML
    private TextArea selectedSeatsArea;

    private final TicketService ticketService = new TicketService();
    private final ShowtimeDao showtimeDao = new ShowtimeDao();
    private final SeatDao seatDao = new SeatDao();
    private final SeatCategoryDao seatCategoryDao = new SeatCategoryDao();
    private final Set<Integer> selectedSeatIds = new HashSet<>();

    @FXML
    public void handleLoadOccupiedSeats() {
        try {
            int showtimeId = parseRequiredInt(showtimeIdField.getText(), "ID сеанса");
            List<Integer> occupied = ticketService.occupiedSeats(showtimeId);
            occupiedSeatsArea.setText(occupied.toString());
        } catch (Exception ex) {
            showAlert("Ошибка загрузки мест: " + ex.getMessage());
        }
    }

    @FXML
    public void handleReserveTicket() {
        try {
            int showtimeId = parseRequiredInt(showtimeIdField.getText(), "ID сеанса");
            int seatId = parseRequiredInt(seatIdField.getText(), "ID места");
            Integer customerId = parseNullableInt(customerIdField.getText());
            int userId = parseRequiredInt(userIdField.getText(), "ID кассира");
            ticketService.reserveTickets(showtimeId, List.of(seatId), customerId, userId);
            showInfo("Бронь создана");
        } catch (SQLException | IllegalArgumentException ex) {
            showAlert("Ошибка брони: " + ex.getMessage());
        }
    }

    @FXML
    public void handleDirectPurchase() {
        try {
            int showtimeId = parseRequiredInt(showtimeIdField.getText(), "ID сеанса");
            int seatId = parseRequiredInt(seatIdField.getText(), "ID места");
            Integer customerId = parseNullableInt(customerIdField.getText());
            int userId = parseRequiredInt(userIdField.getText(), "ID кассира");
            int methodId = parseRequiredInt(paymentMethodField.getText(), "Метод оплаты");
            ticketService.directPurchase(showtimeId, seatId, customerId, userId, methodId);
            showInfo("Оплата прошла успешно");
        } catch (SQLException | IllegalArgumentException ex) {
            showAlert("Ошибка оплаты: " + ex.getMessage());
        }
    }

    @FXML
    public void handleBuildSeatMap() {
        selectedSeatIds.clear();
        selectedSeatsArea.clear();
        seatMapGrid.getChildren().clear();
        try {
            int showtimeId = parseRequiredInt(showtimeIdField.getText(), "ID сеанса");
            Showtime showtime = showtimeDao.findById(showtimeId).orElse(null);
            if (showtime == null) {
                showAlert("Сеанс не найден");
                return;
            }
            List<Seat> seats = seatDao.findByHall(showtime.hallId());
            List<Integer> occupied = ticketService.occupiedSeats(showtimeId);
            Map<Integer, SeatCategory> categories = new HashMap<>();
            for (SeatCategory category : seatCategoryDao.findAll()) {
                categories.put(category.categoryId(), category);
            }
            for (Seat seat : seats) {
                Button seatButton = new Button(seat.seatRow() + "-" + seat.seatNumber());
                seatButton.setMinWidth(60);
                seatButton.setUserData(seat.seatId());
                SeatCategory category = categories.get(seat.categoryId());
                if (category != null && category.color() != null && !category.color().isBlank()) {
                    seatButton.setStyle("-fx-background-color: " + category.color() + "; -fx-text-fill: white;");
                }
                if (occupied.contains(seat.seatId())) {
                    seatButton.setDisable(true);
                    seatButton.setStyle("-fx-background-color: #808080; -fx-text-fill: white;");
                } else {
                    seatButton.setOnAction(event -> toggleSeatSelection(seat.seatId(), seatButton));
                }
                seatMapGrid.add(seatButton, seat.seatNumber() - 1, seat.seatRow() - 1);
            }
        } catch (Exception ex) {
            showAlert("Ошибка построения схемы: " + ex.getMessage());
        }
    }

    @FXML
    public void handleReserveSelectedSeats() {
        try {
            int showtimeId = parseRequiredInt(showtimeIdField.getText(), "ID сеанса");
            Integer customerId = parseNullableInt(customerIdField.getText());
            int userId = parseRequiredInt(userIdField.getText(), "ID кассира");
            if (selectedSeatIds.isEmpty()) {
                showAlert("Выберите места для брони");
                return;
            }
            ticketService.reserveTickets(showtimeId, List.copyOf(selectedSeatIds), customerId, userId);
            showInfo("Бронь создана");
            handleBuildSeatMap();
        } catch (SQLException | IllegalArgumentException ex) {
            showAlert("Ошибка брони: " + ex.getMessage());
        }
    }

    @FXML
    public void handlePurchaseSelectedSeats() {
        try {
            int showtimeId = parseRequiredInt(showtimeIdField.getText(), "ID сеанса");
            Integer customerId = parseNullableInt(customerIdField.getText());
            int userId = parseRequiredInt(userIdField.getText(), "ID кассира");
            int methodId = parseRequiredInt(paymentMethodField.getText(), "Метод оплаты");
            if (selectedSeatIds.isEmpty()) {
                showAlert("Выберите места для покупки");
                return;
            }
            for (Integer seatId : selectedSeatIds) {
                ticketService.directPurchase(showtimeId, seatId, customerId, userId, methodId);
            }
            showInfo("Покупка завершена");
            handleBuildSeatMap();
        } catch (SQLException | IllegalArgumentException ex) {
            showAlert("Ошибка оплаты: " + ex.getMessage());
        }
    }

    @FXML
    public void handleConfirmReservation() {
        try {
            long ticketId = parseRequiredLong(ticketIdField.getText(), "ID билета");
            int userId = parseRequiredInt(userIdField.getText(), "ID кассира");
            int methodId = parseRequiredInt(paymentMethodField.getText(), "Метод оплаты");
            ticketService.confirmReservation(ticketId, userId, methodId);
            showInfo("Бронь оплачена");
        } catch (SQLException | IllegalArgumentException ex) {
            showAlert("Ошибка оплаты брони: " + ex.getMessage());
        }
    }

    @FXML
    public void handleRefund() {
        try {
            long ticketId = parseRequiredLong(ticketIdField.getText(), "ID билета");
            ticketService.refundTicket(ticketId);
            showInfo("Возврат выполнен");
        } catch (SQLException | IllegalArgumentException ex) {
            showAlert("Ошибка возврата: " + ex.getMessage());
        }
    }

    private Integer parseNullableInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private int parseRequiredInt(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " не заполнен");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " имеет неверный формат");
        }
    }

    private long parseRequiredLong(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " не заполнен");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " имеет неверный формат");
        }
    }

    private void toggleSeatSelection(int seatId, Button seatButton) {
        if (selectedSeatIds.contains(seatId)) {
            selectedSeatIds.remove(seatId);
            seatButton.setOpacity(1.0);
        } else {
            selectedSeatIds.add(seatId);
            seatButton.setOpacity(0.6);
        }
        selectedSeatsArea.setText(selectedSeatIds.toString());
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
