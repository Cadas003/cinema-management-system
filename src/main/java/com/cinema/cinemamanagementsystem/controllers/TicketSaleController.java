package com.cinema.cinemamanagementsystem.controllers;

import com.cinema.cinemamanagementsystem.config.DatabaseConfig;
import com.cinema.cinemamanagementsystem.dao.CustomerDAO;
import com.cinema.cinemamanagementsystem.dao.SeatCategoryDAO;
import com.cinema.cinemamanagementsystem.dao.ShowtimeDAO;
import com.cinema.cinemamanagementsystem.models.Customer;
import com.cinema.cinemamanagementsystem.models.Seat;
import com.cinema.cinemamanagementsystem.models.SeatCategory;
import com.cinema.cinemamanagementsystem.models.Showtime;
import com.cinema.cinemamanagementsystem.models.Ticket;
import com.cinema.cinemamanagementsystem.models.User;
import com.cinema.cinemamanagementsystem.services.BookingService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class TicketSaleController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(TicketSaleController.class);

    @FXML private Label totalSelectedLabel;
    @FXML private Label selectedFilmLabel;
    @FXML private Label selectedDateTimeLabel;
    @FXML private Label selectedHallLabel;
    @FXML private Label selectedPriceLabel;
    @FXML private GridPane seatGrid;
    @FXML private RadioButton guestCustomerRadio;
    @FXML private RadioButton registeredCustomerRadio;
    @FXML private TextField registeredSearchField;
    @FXML private ComboBox<Customer> registeredCustomerCombo;
    @FXML private Button sellTicketButton;
    @FXML private Button bookTicketButton;

    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final SeatCategoryDAO seatCategoryDAO = new SeatCategoryDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final BookingService bookingService = new BookingService();

    private ObservableList<Customer> registeredCustomersList = FXCollections.observableArrayList();
    private List<Integer> selectedSeatIds = new ArrayList<>();
    private Showtime selectedShowtime;
    private User currentUser;
    private java.util.Map<Integer, SeatCategory> categoryMap;

    public void setShowtime(Showtime showtime) {
        this.selectedShowtime = showtime;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        guestCustomerRadio.setSelected(true);
        guestCustomerRadio.selectedProperty().addListener((obs, oldVal, newVal) -> updateCustomerMode());
        registeredCustomerRadio.selectedProperty().addListener((obs, oldVal, newVal) -> updateCustomerMode());
        registeredSearchField.textProperty().addListener((obs, oldVal, newVal) -> filterRegisteredCustomers(newVal));
        registeredCustomerCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateTotalSelected());

        sellTicketButton.setOnAction(e -> sellTickets());
        bookTicketButton.setOnAction(e -> bookTickets());
    }

    public void initializeData() {
        categoryMap = seatCategoryDAO.getAllCategories();
        loadRegisteredCustomers();
        updateCustomerMode();
        updateShowtimeInfo();
        renderSeats();
        updateTotalSelected();
    }

    private void loadRegisteredCustomers() {
        registeredCustomersList.setAll(customerDAO.getRegisteredCustomers());
        registeredCustomerCombo.setItems(registeredCustomersList);
        if (!registeredCustomersList.isEmpty()) {
            registeredCustomerCombo.getSelectionModel().selectFirst();
        }
    }

    private void updateCustomerMode() {
        boolean isGuest = guestCustomerRadio.isSelected();
        registeredSearchField.setDisable(isGuest);
        registeredCustomerCombo.setDisable(isGuest);
        updateTotalSelected();
    }

    private void updateShowtimeInfo() {
        if (selectedShowtime == null) {
            return;
        }

        selectedFilmLabel.setText("Фильм: " + selectedShowtime.getFilmTitle());
        selectedDateTimeLabel.setText("Время: " +
                selectedShowtime.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        selectedHallLabel.setText("Зал: " + selectedShowtime.getHallName());

        int totalSeats = showtimeDAO.getSeatsForShowtime(selectedShowtime.getShowtimeId()).size();
        int takenSeats = showtimeDAO.getTakenSeats(selectedShowtime.getShowtimeId()).size();
        int freeSeats = totalSeats - takenSeats;

        selectedPriceLabel.setText(String.format("Цена: %.2f руб. | Свободно: %d/%d",
                calculateTicketPrice(false), freeSeats, totalSeats));
    }

    private void renderSeats() {
        if (selectedShowtime == null) {
            return;
        }

        seatGrid.getChildren().clear();
        selectedSeatIds.clear();

        seatGrid.setGridLinesVisible(false);
        seatGrid.getRowConstraints().clear();
        seatGrid.getColumnConstraints().clear();

        List<Seat> seats = showtimeDAO.getSeatsForShowtime(selectedShowtime.getShowtimeId());
        List<Integer> takenSeats = showtimeDAO.getTakenSeats(selectedShowtime.getShowtimeId());

        if (seats.isEmpty()) {
            return;
        }

        List<Seat> availableSeats = seats.stream()
                .filter(seat -> !takenSeats.contains(seat.getSeatId()))
                .collect(Collectors.toList());

        List<Integer> availableRows = availableSeats.stream()
                .map(Seat::getRow)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<Integer> availableColumns = availableSeats.stream()
                .map(Seat::getNumber)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        if (availableSeats.isEmpty()) {
            Label noSeatsLabel = new Label("Все места заняты");
            noSeatsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            seatGrid.add(noSeatsLabel, 0, 0, 2, 1);
            return;
        }

        int maxRow = availableRows.size();
        int maxCol = availableColumns.size();

        for (int i = 0; i <= maxCol + 1; i++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setHalignment(HPos.CENTER);
            colConst.setHgrow(Priority.SOMETIMES);
            colConst.setMinWidth(30);
            colConst.setPrefWidth(34);
            colConst.setMaxWidth(38);
            seatGrid.getColumnConstraints().add(colConst);
        }

        for (int i = 0; i <= maxRow + 2; i++) {
            RowConstraints rowConst = new RowConstraints();
            rowConst.setValignment(VPos.CENTER);
            rowConst.setVgrow(Priority.SOMETIMES);
            rowConst.setMinHeight(30);
            rowConst.setPrefHeight(34);
            rowConst.setMaxHeight(38);
            seatGrid.getRowConstraints().add(rowConst);
        }

        for (int i = 0; i < availableColumns.size(); i++) {
            Label colLabel = new Label(String.valueOf(availableColumns.get(i)));
            colLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-font-size: 11px; -fx-padding: 2px;");
            colLabel.setAlignment(Pos.CENTER);
            seatGrid.add(colLabel, i + 1, 1);
        }

        for (int i = 0; i < availableRows.size(); i++) {
            Label rowLabel = new Label(String.valueOf(availableRows.get(i)));
            rowLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-font-size: 11px; -fx-padding: 2px;");
            rowLabel.setAlignment(Pos.CENTER);
            seatGrid.add(rowLabel, 0, i + 2);
        }

        java.util.Map<String, Seat> seatMap = new java.util.HashMap<>();
        for (Seat seat : availableSeats) {
            seatMap.put(seat.getRow() + "-" + seat.getNumber(), seat);
        }

        for (int rowIdx = 0; rowIdx < availableRows.size(); rowIdx++) {
            final int rowNum = availableRows.get(rowIdx);

            for (int colIdx = 0; colIdx < availableColumns.size(); colIdx++) {
                final int colNum = availableColumns.get(colIdx);

                Seat seat = seatMap.get(rowNum + "-" + colNum);
                if (seat == null) {
                    continue;
                }

                Button seatButton = new Button(String.valueOf(colNum));
                seatButton.setPrefSize(32, 32);
                seatButton.setMinSize(30, 30);
                seatButton.setMaxSize(36, 36);

                final boolean isSelected = selectedSeatIds.contains(seat.getSeatId());

                final SeatCategory category = categoryMap.get(seat.getCategoryId());
                final String color = (category != null && category.getColor() != null)
                        ? category.getColor() : "#7f8c8d";
                final String categoryName = (category != null) ? category.getName() : "Стандарт";

                updateSeatButtonStyle(seatButton, isSelected, color);

                seatButton.setOnAction(e -> toggleSeatSelection(seat.getSeatId(), seatButton, color));
                seatButton.setUserData(seat.getSeatId());

                StringBuilder tooltipText = new StringBuilder();
                tooltipText.append("Ряд: ").append(rowNum).append("\n");
                tooltipText.append("Место: ").append(colNum).append("\n");
                tooltipText.append("Категория: ").append(categoryName).append("\n");
                tooltipText.append("Цена: ").append(String.format("%.2f руб.", calculateTicketPrice(false)));

                Tooltip tooltip = new Tooltip(tooltipText.toString());
                tooltip.setStyle("-fx-font-size: 11px; -fx-font-weight: normal;");
                tooltip.setShowDelay(Duration.millis(300));
                tooltip.setShowDuration(Duration.seconds(10));
                Tooltip.install(seatButton, tooltip);

                seatButton.setOnMouseEntered(e -> {
                    String currentStyle = seatButton.getStyle();
                    if (!currentStyle.contains("dropshadow")) {
                        seatButton.setStyle(currentStyle +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 0);");
                    }
                });

                seatButton.setOnMouseExited(e -> {
                    boolean currentlySelected = selectedSeatIds.contains(seat.getSeatId());
                    updateSeatButtonStyle(seatButton, currentlySelected, color);
                });

                GridPane.setHalignment(seatButton, HPos.CENTER);
                GridPane.setValignment(seatButton, VPos.CENTER);
                seatGrid.add(seatButton, colIdx + 1, rowIdx + 2);
            }
        }

        seatGrid.setPadding(new javafx.geometry.Insets(10));
        seatGrid.setHgap(5);
        seatGrid.setVgap(5);

        int preferredWidth = (maxCol + 1) * 34 + 24;
        int preferredHeight = (maxRow + 2) * 34 + 24;
        seatGrid.setPrefSize(preferredWidth, preferredHeight);
    }

    private void updateSeatButtonStyle(Button seatButton, boolean isSelected, String color) {
        if (isSelected) {
            seatButton.setStyle("-fx-background-color: #ff66b2; -fx-text-fill: white; " +
                    "-fx-background-radius: 6; -fx-font-weight: bold; " +
                    "-fx-border-color: #ff3385; -fx-border-width: 2;");
        } else {
            seatButton.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                    "-fx-background-radius: 6; -fx-font-weight: bold;");
        }
    }

    private void toggleSeatSelection(int seatId, Button seatButton, String seatColor) {
        if (selectedSeatIds.contains(seatId)) {
            selectedSeatIds.remove(Integer.valueOf(seatId));
        } else {
            selectedSeatIds.add(seatId);
        }

        boolean isSelected = selectedSeatIds.contains(seatId);
        updateSeatButtonStyle(seatButton, isSelected, seatColor);
        updateTotalSelected();
    }

    private void updateTotalSelected() {
        int count = selectedSeatIds.size();
        double pricePerTicket = selectedShowtime != null ? calculateTicketPrice(false) : 0;
        double total = count * pricePerTicket;

        totalSelectedLabel.setText(String.format("Выбрано: %d мест | Итого: %.2f руб.", count, total));

        boolean canSell = selectedShowtime != null && !selectedSeatIds.isEmpty() && hasValidCustomerSelection();
        sellTicketButton.setDisable(!canSell);
        bookTicketButton.setDisable(!canSell);

        updateShowtimeInfo();
    }

    private boolean hasValidCustomerSelection() {
        return guestCustomerRadio.isSelected() || registeredCustomerCombo.getValue() != null;
    }

    private void sellTickets() {
        try {
            validateSaleForm();
            CustomerSelection selection = resolveCustomerSelection();
            List<Ticket> soldTickets = new ArrayList<>();

            for (Integer seatId : selectedSeatIds) {
                Ticket ticket = bookingService.sellTicket(
                        selectedShowtime.getShowtimeId(),
                        seatId,
                        currentUser.getUserId(),
                        selection.customer(),
                        selection.isGuest()
                );

                if (ticket != null) {
                    soldTickets.add(ticket);
                }
            }

            if (!soldTickets.isEmpty()) {
                showAlert("Успех",
                        String.format("Продано %d билетов. Номера: %s",
                                soldTickets.size(),
                                soldTickets.stream()
                                        .map(t -> "#" + t.getTicketId())
                                        .collect(Collectors.joining(", "))),
                        Alert.AlertType.INFORMATION);
                clearSaleForm();
            }
        } catch (Exception e) {
            logger.error("Ошибка при продаже билетов: {}", e.getMessage());
            showAlert("Ошибка", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void bookTickets() {
        try {
            validateSaleForm();
            CustomerSelection selection = resolveCustomerSelection();
            List<Ticket> bookedTickets = new ArrayList<>();

            for (Integer seatId : selectedSeatIds) {
                Ticket ticket = bookingService.bookTicket(
                        selectedShowtime.getShowtimeId(),
                        seatId,
                        currentUser.getUserId(),
                        selection.customer(),
                        selection.isGuest()
                );

                if (ticket != null) {
                    bookedTickets.add(ticket);
                }
            }

            if (!bookedTickets.isEmpty()) {
                showAlert("Успех",
                        String.format("Создано %d бронирований. Номера: %s",
                                bookedTickets.size(),
                                bookedTickets.stream()
                                        .map(t -> "#" + t.getTicketId())
                                        .collect(Collectors.joining(", "))),
                        Alert.AlertType.INFORMATION);
                clearSaleForm();
            }
        } catch (Exception e) {
            logger.error("Ошибка при бронировании: {}", e.getMessage());
            showAlert("Ошибка", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void validateSaleForm() {
        if (selectedShowtime == null) {
            throw new IllegalArgumentException("Сеанс не выбран");
        }
        if (selectedSeatIds.isEmpty()) {
            throw new IllegalArgumentException("Выберите места");
        }
        if (!hasValidCustomerSelection()) {
            throw new IllegalArgumentException("Выберите клиента");
        }

        List<Integer> takenSeats = showtimeDAO.getTakenSeats(selectedShowtime.getShowtimeId());
        for (Integer seatId : selectedSeatIds) {
            if (takenSeats.contains(seatId)) {
                throw new IllegalArgumentException("Место #" + seatId + " уже занято");
            }
        }
    }

    private void clearSaleForm() {
        registeredCustomerCombo.setValue(null);
        selectedSeatIds.clear();
        renderSeats();
        updateTotalSelected();
    }

    private CustomerSelection resolveCustomerSelection() {
        if (guestCustomerRadio.isSelected()) {
            String guestName = "Гость";
            String guestPhone = "GUEST-" + System.currentTimeMillis();
            Customer guest = customerDAO.createGuestCustomer(guestName, guestPhone, null);
            if (guest == null) {
                throw new IllegalStateException("Не удалось создать гостя");
            }
            return new CustomerSelection(guest, true);
        }

        return new CustomerSelection(registeredCustomerCombo.getValue(), false);
    }

    private void filterRegisteredCustomers(String query) {
        if (query == null || query.trim().isEmpty()) {
            registeredCustomerCombo.setItems(registeredCustomersList);
            return;
        }

        String searchText = query.trim().toLowerCase(Locale.ROOT);
        ObservableList<Customer> filtered = FXCollections.observableArrayList(
                registeredCustomersList.stream()
                        .filter(customer -> matchesCustomer(customer, searchText))
                        .collect(Collectors.toList())
        );
        registeredCustomerCombo.setItems(filtered);
        if (!filtered.isEmpty()) {
            registeredCustomerCombo.getSelectionModel().selectFirst();
        }
    }

    private boolean matchesCustomer(Customer customer, String searchText) {
        String name = customer.getName() != null ? customer.getName().toLowerCase(Locale.ROOT) : "";
        String phone = customer.getPhone() != null ? customer.getPhone().toLowerCase(Locale.ROOT) : "";
        String email = customer.getEmail() != null ? customer.getEmail().toLowerCase(Locale.ROOT) : "";
        return name.contains(searchText) || phone.contains(searchText) || email.contains(searchText);
    }

    private double calculateTicketPrice(boolean isBooking) {
        if (selectedShowtime == null) {
            return 0;
        }

        double price = selectedShowtime.getFinalPrice();
        if (isBooking) {
            price *= (1 + DatabaseConfig.BOOKING_SURCHARGE_RATE);
        }

        if (guestCustomerRadio.isSelected()) {
            price *= (1 + DatabaseConfig.GUEST_SURCHARGE_RATE);
        } else {
            price *= (1 - DatabaseConfig.REGISTERED_DISCOUNT_RATE);
        }

        return price;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class CustomerSelection {
        private final Customer customer;
        private final boolean isGuest;

        private CustomerSelection(Customer customer, boolean isGuest) {
            this.customer = customer;
            this.isGuest = isGuest;
        }

        private Customer customer() {
            return customer;
        }

        private boolean isGuest() {
            return isGuest;
        }
    }
}
