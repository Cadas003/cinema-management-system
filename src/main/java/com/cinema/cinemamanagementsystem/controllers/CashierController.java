package com.cinema.cinemamanagementsystem.controllers;

import com.cinema.cinemamanagementsystem.dao.*;
import com.cinema.cinemamanagementsystem.models.*;
import com.cinema.cinemamanagementsystem.services.BookingService;
import com.cinema.cinemamanagementsystem.config.DatabaseConfig;
import javafx.geometry.Pos;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.ColumnConstraints;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.Priority;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CashierController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(CashierController.class);

    // UI элементы
    @FXML private TabPane mainTabPane;
    @FXML private Label welcomeLabel;
    @FXML private Label currentTimeLabel;

    // Вкладка ПРОДАЖА
    @FXML private DatePicker datePicker;
    @FXML private TilePane filmsTilePane;
    @FXML private Label selectedFilmLabel;
    @FXML private Label selectedDateTimeLabel;
    @FXML private Label selectedHallLabel;
    @FXML private Label selectedPriceLabel;
    @FXML private GridPane seatGrid;
    @FXML private TextField customerNameField;
    @FXML private TextField customerPhoneField;
    @FXML private TextField customerEmailField;
    @FXML private RadioButton immediateSaleRadio;
    @FXML private RadioButton bookingRadio;
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private Button sellTicketButton;
    @FXML private Button bookTicketButton;
    @FXML private Label totalSelectedLabel;

    // Вкладка БРОНИРОВАНИЯ
    @FXML private TableView<Ticket> bookingsTable;
    @FXML private TableColumn<Ticket, Integer> bookingIdColumn;
    @FXML private TableColumn<Ticket, String> bookingFilmColumn;
    @FXML private TableColumn<Ticket, String> bookingTimeColumn;
    @FXML private TableColumn<Ticket, String> bookingSeatColumn;
    @FXML private TableColumn<Ticket, String> bookingCustomerColumn;
    @FXML private TableColumn<Ticket, String> bookingStatusColumn;
    @FXML private Button confirmBookingButton;
    @FXML private Button cancelBookingButton;

    // Вкладка КЛИЕНТЫ
    @FXML private TableView<Customer> customersTable;
    @FXML private TableColumn<Customer, Integer> customerIdColumn;
    @FXML private TableColumn<Customer, String> customerNameColumn;
    @FXML private TableColumn<Customer, String> customerPhoneColumn;
    @FXML private TableColumn<Customer, Integer> customerVisitsColumn;
    @FXML private TableColumn<Customer, Double> customerTotalColumn;
    @FXML private TextField customerSearchField;
    @FXML private Button searchCustomerButton;

    // Вкладка ВОЗВРАТЫ
    @FXML private TextField refundTicketIdField;
    @FXML private TextField refundReasonField;
    @FXML private Button refundButton;

    // DAO
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final TicketDAO ticketDAO = new TicketDAO();
    private final SeatCategoryDAO seatCategoryDAO = new SeatCategoryDAO();
    private final BookingService bookingService = new BookingService();

    // Данные
    private Map<Integer, SeatCategory> categoryMap;
    private ObservableList<Showtime> allShowtimesList = FXCollections.observableArrayList();
    private ObservableList<Ticket> bookingsList = FXCollections.observableArrayList();
    private ObservableList<Customer> customersList = FXCollections.observableArrayList();

    // Текущие выборы
    private LocalDate selectedDate;
    private Film selectedFilm;
    private Showtime selectedShowtime;
    private List<Integer> selectedSeatIds = new ArrayList<>();
    private User currentUser;
    private Stage primaryStage;

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSellingTab();
        setupBookingsTab();
        setupCustomersTab();
        setupRefundsTab();
        startClock();
        startBookingTimer();
    }

    public void initializeData() {
        if (currentUser != null) {
            welcomeLabel.setText("Кассир: " + currentUser.getFullName());
        }

        categoryMap = seatCategoryDAO.getAllCategories();

        // Загружаем данные
        loadAllShowtimes();
        loadActiveBookings();
        loadCustomers();
        loadPaymentMethods();

        // Устанавливаем сегодняшнюю дату
        selectedDate = LocalDate.now();
        datePicker.setValue(selectedDate);

        // Загружаем фильмы на выбранную дату
        loadFilmsForDate(selectedDate);
    }

    // ===================== НАСТРОЙКА ВКЛАДОК =====================

    private void setupSellingTab() {
        // Группа радиокнопок
        ToggleGroup saleTypeGroup = new ToggleGroup();
        immediateSaleRadio.setToggleGroup(saleTypeGroup);
        bookingRadio.setToggleGroup(saleTypeGroup);
        immediateSaleRadio.setSelected(true);

        // Настройка DatePicker
        setupDatePicker();

        // Кнопки продажи/бронирования
        sellTicketButton.setOnAction(e -> sellTickets());
        bookTicketButton.setOnAction(e -> bookTickets());

        // Валидация телефона
        customerPhoneField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\+?\\d*")) customerPhoneField.setText(oldV);
        });

        // Обновление суммы
        updateTotalSelected();
    }

    private void setupDatePicker() {
        datePicker.setValue(LocalDate.now());
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                // Отключаем прошедшие даты
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffcccc;");
                }

                // Подсвечиваем даты с сеансами
                if (hasShowtimesOnDate(date)) {
                    setStyle("-fx-background-color: #e8f5e9; -fx-border-color: #4caf50;");
                }
            }
        });

        datePicker.setOnAction(e -> {
            selectedDate = datePicker.getValue();
            loadFilmsForDate(selectedDate);
            clearFilmSelection();
            clearShowtimeSelection();
        });
    }

    private void setupBookingsTab() {
        bookingIdColumn.setCellValueFactory(new PropertyValueFactory<>("ticketId"));
        bookingFilmColumn.setCellValueFactory(new PropertyValueFactory<>("filmTitle"));
        bookingTimeColumn.setCellValueFactory(cell -> {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("dd.MM HH:mm");
            return new SimpleStringProperty(cell.getValue().getCreatedAt().format(f));
        });
        bookingSeatColumn.setCellValueFactory(new PropertyValueFactory<>("seatInfo"));
        bookingCustomerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        bookingStatusColumn.setCellValueFactory(cell -> {
            Ticket t = cell.getValue();
            LocalDateTime expiry = t.getCreatedAt().plusMinutes(DatabaseConfig.BOOKING_TIMEOUT_MINUTES);
            long left = java.time.Duration.between(LocalDateTime.now(), expiry).toMinutes();
            return new SimpleStringProperty(
                    left > 0 ? "Действует (" + left + " мин)" : "Истекло"
            );
        });

        bookingsTable.setItems(bookingsList);
        confirmBookingButton.setOnAction(e -> confirmBooking());
        cancelBookingButton.setOnAction(e -> cancelBooking());
    }

    private void setupCustomersTab() {
        customerIdColumn.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        customerPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        customerVisitsColumn.setCellValueFactory(new PropertyValueFactory<>("visitCount"));
        customerTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalSpent"));

        customersTable.setItems(customersList);
        searchCustomerButton.setOnAction(e -> searchCustomers());
        customerSearchField.setOnAction(e -> searchCustomers());
    }

    private void setupRefundsTab() {
        refundButton.setOnAction(e -> processRefund());
    }

    // ===================== ЗАГРУЗКА ДАННЫХ =====================

    private void loadAllShowtimes() {
        allShowtimesList.setAll(showtimeDAO.getFutureShowtimes());
    }

    private void loadFilmsForDate(LocalDate date) {
        filmsTilePane.getChildren().clear();

        // Используем Map для группировки фильмов по ID (чтобы избежать дублирования)
        Map<Integer, Film> uniqueFilms = new HashMap<>();
        Map<Integer, List<Showtime>> filmShowtimesMap = new HashMap<>();

        // Собираем все сеансы на эту дату и группируем по фильмам
        for (Showtime showtime : allShowtimesList) {
            if (showtime.getDateTime().toLocalDate().equals(date)) {
                int filmId = showtime.getFilmId();

                // Создаем объект фильма только если его еще нет
                if (!uniqueFilms.containsKey(filmId)) {
                    Film film = new Film();
                    film.setFilmId(filmId);
                    film.setTitle(showtime.getFilmTitle());
                    uniqueFilms.put(filmId, film);
                }

                // Добавляем сеанс в список сеансов этого фильма
                filmShowtimesMap.computeIfAbsent(filmId, k -> new ArrayList<>())
                        .add(showtime);
            }
        }

        // Создаем карточки для уникальных фильмов
        List<Film> films = uniqueFilms.values().stream()
                .sorted(Comparator.comparing(Film::getTitle))
                .collect(Collectors.toList());

        for (Film film : films) {
            List<Showtime> filmShowtimes = filmShowtimesMap.get(film.getFilmId());
            VBox filmCard = createFilmCard(film, date, filmShowtimes);
            filmsTilePane.getChildren().add(filmCard);
        }

        // Если фильмов нет
        if (films.isEmpty()) {
            Label noFilmsLabel = new Label("На " + date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) +
                    " сеансов нет");
            noFilmsLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 14;");
            filmsTilePane.getChildren().add(noFilmsLabel);
        }
    }

    private VBox createFilmCard(Film film, LocalDate date, List<Showtime> filmShowtimesParam) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setPrefWidth(200);
        card.setStyle("-fx-background-color: #34495e; -fx-background-radius: 10; " +
                "-fx-border-color: #2c3e50; -fx-border-radius: 10;");
        card.setUserData(film);

        // Заголовок фильма
        Label titleLabel = new Label(film.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: white;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(180);

        // Создаем effectively final копию списка
        final List<Showtime> showtimesList;
        if (filmShowtimesParam == null || filmShowtimesParam.isEmpty()) {
            showtimesList = new ArrayList<>();
        } else {
            // Создаем новый список, чтобы не модифицировать оригинал
            showtimesList = new ArrayList<>(filmShowtimesParam);
            // Сортируем сеансы по времени
            showtimesList.sort(Comparator.comparing(Showtime::getDateTime));
        }

        VBox timesBox = new VBox(5);

        for (Showtime showtime : showtimesList) {
            HBox timeRow = new HBox(10);
            timeRow.setAlignment(Pos.CENTER_LEFT);

            // Кнопка времени
            Button timeBtn = new Button(showtime.getDateTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm")));
            timeBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-min-width: 60;");
            timeBtn.setUserData(showtime);
            timeBtn.setOnAction(e -> selectShowtime(showtime));

            // Информация о зале и свободных местах
            Label hallLabel = new Label("Зал " + showtime.getHallName());
            hallLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 11;");

            int totalSeats = showtimeDAO.getSeatsForShowtime(showtime.getShowtimeId()).size();
            int takenSeats = showtimeDAO.getTakenSeats(showtime.getShowtimeId()).size();
            int freeSeats = totalSeats - takenSeats;

            Label seatsLabel = new Label(freeSeats + " мест");
            String seatsStyle = freeSeats == 0 ? "-fx-text-fill: #e74c3c;" :
                    freeSeats < 10 ? "-fx-text-fill: #f39c12;" :
                            "-fx-text-fill: #27ae60;";
            seatsLabel.setStyle(seatsStyle + " -fx-font-size: 11;");

            timeRow.getChildren().addAll(timeBtn, hallLabel, seatsLabel);
            timesBox.getChildren().add(timeRow);
        }

        // Если у фильма нет сеансов на эту дату
        if (showtimesList.isEmpty()) {
            Label noShowtimesLabel = new Label("Нет сеансов");
            noShowtimesLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 12;");
            timesBox.getChildren().add(noShowtimesLabel);
        }

        // Обработчик клика по карточке - используем effectively final переменную
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) { // Двойной клик
                if (!showtimesList.isEmpty()) {
                    selectShowtime(showtimesList.get(0));
                }
            }
        });

        card.getChildren().addAll(titleLabel, timesBox);
        return card;
    }

    private void selectShowtime(Showtime showtime) {
        selectedShowtime = showtime;
        selectedFilm = new Film();
        selectedFilm.setFilmId(showtime.getFilmId());
        selectedFilm.setTitle(showtime.getFilmTitle());

        // Обновляем информацию
        updateShowtimeInfo();

        // Подсвечиваем выбранную карточку
        highlightSelectedFilmCard();

        // Рисуем места
        renderSeats();
    }

    private void highlightSelectedFilmCard() {
        for (Node node : filmsTilePane.getChildren()) {
            if (node instanceof VBox) {
                VBox card = (VBox) node;
                Film cardFilm = (Film) card.getUserData();

                if (cardFilm != null && cardFilm.getFilmId() == selectedFilm.getFilmId()) {
                    card.setStyle("-fx-background-color: #2c3e50; -fx-background-radius: 10; " +
                            "-fx-border-color: #3498db; -fx-border-width: 2; " +
                            "-fx-border-radius: 10;");
                } else {
                    card.setStyle("-fx-background-color: #34495e; -fx-background-radius: 10; " +
                            "-fx-border-color: #2c3e50; -fx-border-radius: 10;");
                }
            }
        }
    }

    private void loadActiveBookings() {
        bookingsList.setAll(ticketDAO.getActiveBookings(false));
    }

    private void loadCustomers() {
        customersList.setAll(customerDAO.getAllCustomers());
    }

    private void loadPaymentMethods() {
        paymentMethodCombo.getItems().addAll("Банковская карта", "Наличные", "СБП");
        paymentMethodCombo.setValue("Банковская карта");
    }

    private boolean hasShowtimesOnDate(LocalDate date) {
        return allShowtimesList.stream()
                .anyMatch(st -> st.getDateTime().toLocalDate().equals(date));
    }

    // ===================== ОТРИСОВКА МЕСТ =====================

    private void renderSeats() {
        if (selectedShowtime == null) return;

        seatGrid.getChildren().clear();
        selectedSeatIds.clear();
        updateTotalSelected();

        seatGrid.setGridLinesVisible(false);
        seatGrid.getRowConstraints().clear();
        seatGrid.getColumnConstraints().clear();

        List<Seat> seats = showtimeDAO.getSeatsForShowtime(selectedShowtime.getShowtimeId());
        List<Integer> takenSeats = showtimeDAO.getTakenSeats(selectedShowtime.getShowtimeId());

        if (seats.isEmpty()) return;

        // Фильтруем места: оставляем только свободные
        List<Seat> availableSeats = seats.stream()
                .filter(seat -> !takenSeats.contains(seat.getSeatId()))
                .collect(Collectors.toList());

        // Получаем уникальные ряды и колонки из доступных мест
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
            noSeatsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            seatGrid.add(noSeatsLabel, 0, 0, 2, 1);
            return;
        }

        int maxRow = availableRows.size();
        int maxCol = availableColumns.size();

        // Создаем констрейнты для колонок
        for (int i = 0; i <= maxCol + 1; i++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setHalignment(HPos.CENTER);
            colConst.setHgrow(Priority.SOMETIMES);
            colConst.setMinWidth(35);
            colConst.setPrefWidth(40);
            colConst.setMaxWidth(45);
            seatGrid.getColumnConstraints().add(colConst);
        }

        // Создаем констрейнты для строк
        for (int i = 0; i <= maxRow + 2; i++) {
            RowConstraints rowConst = new RowConstraints();
            rowConst.setValignment(VPos.CENTER);
            rowConst.setVgrow(Priority.SOMETIMES);
            rowConst.setMinHeight(35);
            rowConst.setPrefHeight(40);
            rowConst.setMaxHeight(45);
            seatGrid.getRowConstraints().add(rowConst);
        }

        // ЭКРАН
        Label screenLabel = new Label("Э К Р А Н");
        screenLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white; " +
                "-fx-padding: 8px; -fx-background-color: #2c3e50; " +
                "-fx-background-radius: 5;");
        screenLabel.setMaxWidth(Double.MAX_VALUE);
        screenLabel.setAlignment(Pos.CENTER);
        seatGrid.add(screenLabel, 1, 0, maxCol, 1);

        // Подписи колонок (номера мест) - только для свободных мест
        for (int i = 0; i < availableColumns.size(); i++) {
            Label colLabel = new Label(String.valueOf(availableColumns.get(i)));
            colLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black; " +
                    "-fx-font-size: 12px; -fx-padding: 2px;");
            colLabel.setAlignment(Pos.CENTER);
            seatGrid.add(colLabel, i + 1, 1);
        }

        // Подписи рядов - только для рядов со свободными местами
        for (int i = 0; i < availableRows.size(); i++) {
            Label rowLabel = new Label(String.valueOf(availableRows.get(i)));
            rowLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black; " +
                    "-fx-font-size: 12px; -fx-padding: 2px;");
            rowLabel.setAlignment(Pos.CENTER);
            seatGrid.add(rowLabel, 0, i + 2);
        }

        // Создаем карту мест для быстрого доступа
        Map<String, Seat> seatMap = new HashMap<>();
        for (Seat seat : availableSeats) {
            seatMap.put(seat.getRow() + "-" + seat.getNumber(), seat);
        }

        // Кнопки мест - ТОЛЬКО СВОБОДНЫЕ МЕСТА
        for (int rowIdx = 0; rowIdx < availableRows.size(); rowIdx++) {
            final int rowNum = availableRows.get(rowIdx);  // Добавить final

            for (int colIdx = 0; colIdx < availableColumns.size(); colIdx++) {
                final int colNum = availableColumns.get(colIdx);  // Добавить final

                Seat seat = seatMap.get(rowNum + "-" + colNum);
                if (seat == null) {
                    // Если в этом ряду нет места с таким номером, оставляем пустую ячейку
                    continue;
                }

                Button seatButton = new Button(String.valueOf(colNum));
                seatButton.setPrefSize(38, 38);
                seatButton.setMinSize(35, 35);
                seatButton.setMaxSize(42, 42);

                final boolean isSelected = selectedSeatIds.contains(seat.getSeatId());  // Добавить final

                final SeatCategory category = categoryMap.get(seat.getCategoryId());  // Добавить final
                final String color = (category != null && category.getColor() != null)  // Добавить final
                        ? category.getColor() : "#7f8c8d";
                final String categoryName = (category != null) ? category.getName() : "Стандарт";  // Добавить final

                // Устанавливаем начальный стиль
                updateSeatButtonStyle(seatButton, isSelected, color);

                seatButton.setOnAction(e -> toggleSeatSelection(seat.getSeatId(), seatButton, color));
                seatButton.setUserData(seat.getSeatId());

                // Создаем Tooltip с информацией о месте
                StringBuilder tooltipText = new StringBuilder();
                tooltipText.append("Ряд: ").append(rowNum).append("\n");
                tooltipText.append("Место: ").append(colNum).append("\n");
                tooltipText.append("Категория: ").append(categoryName).append("\n");
                tooltipText.append("Цена: ").append(String.format("%.2f руб.", selectedShowtime.getFinalPrice()));

                Tooltip tooltip = new Tooltip(tooltipText.toString());
                tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: normal;");
                tooltip.setShowDelay(Duration.millis(300));
                tooltip.setShowDuration(Duration.seconds(10));
                Tooltip.install(seatButton, tooltip);

                // Добавляем эффект при наведении
                seatButton.setOnMouseEntered(e -> {
                    String currentStyle = seatButton.getStyle();
                    // Добавляем тень только если еще нет эффекта
                    if (!currentStyle.contains("dropshadow")) {
                        seatButton.setStyle(currentStyle +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 0);");
                    }
                });

                seatButton.setOnMouseExited(e -> {
                    // Восстанавливаем оригинальный стиль в зависимости от состояния выбора
                    boolean currentlySelected = selectedSeatIds.contains(seat.getSeatId());
                    updateSeatButtonStyle(seatButton, currentlySelected, color);
                });

                GridPane.setHalignment(seatButton, HPos.CENTER);
                GridPane.setValignment(seatButton, VPos.CENTER);
                seatGrid.add(seatButton, colIdx + 1, rowIdx + 2);
            }
        }

        seatGrid.setPadding(new Insets(15));
        seatGrid.setHgap(5);
        seatGrid.setVgap(5);

        int preferredWidth = (maxCol + 1) * 40 + 30;
        int preferredHeight = (maxRow + 2) * 40 + 30;
        seatGrid.setPrefSize(preferredWidth, preferredHeight);
    }

    // Вспомогательный метод для обновления стиля кнопки места
    private void updateSeatButtonStyle(Button seatButton, boolean isSelected, String color) {
        if (isSelected) {
            // ВЫБРАНО - РОЗОВЫЙ
            seatButton.setStyle("-fx-background-color: #ff66b2; -fx-text-fill: white; " +
                    "-fx-background-radius: 6; -fx-font-weight: bold; " +
                    "-fx-border-color: #ff3385; -fx-border-width: 2;");
        } else {
            // СВОБОДНО - обычный цвет категории
            seatButton.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                    "-fx-background-radius: 6; -fx-font-weight: bold;");
        }
    }

    private void toggleSeatSelection(int seatId, Button seatButton, String seatColor) {
        Seat seat = getSeatById(seatId);
        if (seat == null) return;

        if (selectedSeatIds.contains(seatId)) {
            // Снимаем выделение
            selectedSeatIds.remove(Integer.valueOf(seatId));
        } else {
            // Добавляем выделение
            selectedSeatIds.add(seatId);
        }

        // Обновляем стиль кнопки
        boolean isSelected = selectedSeatIds.contains(seatId);
        updateSeatButtonStyle(seatButton, isSelected, seatColor);

        updateTotalSelected();
    }

    private Seat getSeatById(int seatId) {
        List<Seat> seats = showtimeDAO.getSeatsForShowtime(selectedShowtime.getShowtimeId());
        return seats.stream()
                .filter(s -> s.getSeatId() == seatId)
                .findFirst()
                .orElse(null);
    }

    private void updateTotalSelected() {
        int count = selectedSeatIds.size();
        double pricePerTicket = selectedShowtime != null ? selectedShowtime.getFinalPrice() : 0;
        double total = count * pricePerTicket;

        totalSelectedLabel.setText(String.format("Выбрано: %d мест | Итого: %.2f руб.", count, total));

        // Активируем/деактивируем кнопки продажи
        boolean canSell = selectedShowtime != null && !selectedSeatIds.isEmpty() &&
                !customerNameField.getText().trim().isEmpty() &&
                !customerPhoneField.getText().trim().isEmpty();

        sellTicketButton.setDisable(!canSell);
        bookTicketButton.setDisable(!canSell);
    }

    // ===================== ПРОДАЖА И БРОНИРОВАНИЕ =====================

    private void sellTickets() {
        try {
            validateSaleForm();

            List<Ticket> soldTickets = new ArrayList<>();

            for (Integer seatId : selectedSeatIds) {
                Ticket ticket = bookingService.sellTicket(
                        selectedShowtime.getShowtimeId(),
                        seatId,
                        currentUser.getUserId(),
                        customerNameField.getText().trim(),
                        customerPhoneField.getText().trim(),
                        customerEmailField.getText().trim()
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
                reloadData();
            }

        } catch (Exception e) {
            logger.error("Ошибка при продаже билетов: {}", e.getMessage());
            showAlert("Ошибка", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void bookTickets() {
        try {
            validateSaleForm();

            List<Ticket> bookedTickets = new ArrayList<>();

            for (Integer seatId : selectedSeatIds) {
                Ticket ticket = bookingService.bookTicket(
                        selectedShowtime.getShowtimeId(),
                        seatId,
                        currentUser.getUserId(),
                        customerNameField.getText().trim(),
                        customerPhoneField.getText().trim(),
                        customerEmailField.getText().trim()
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
                reloadData();
            }

        } catch (Exception e) {
            logger.error("Ошибка при бронировании: {}", e.getMessage());
            showAlert("Ошибка", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void reloadData() {
        loadAllShowtimes();
        loadFilmsForDate(selectedDate);
        loadActiveBookings();
        if (selectedShowtime != null) {
            renderSeats();
        }
    }

    private void validateSaleForm() {
        if (selectedShowtime == null) throw new IllegalArgumentException("Выберите сеанс");
        if (selectedSeatIds.isEmpty()) throw new IllegalArgumentException("Выберите места");
        if (customerNameField.getText().trim().isEmpty()) throw new IllegalArgumentException("Введите имя клиента");
        if (customerPhoneField.getText().trim().isEmpty()) throw new IllegalArgumentException("Введите телефон");

        // Проверяем, что выбранные места еще свободны
        List<Integer> takenSeats = showtimeDAO.getTakenSeats(selectedShowtime.getShowtimeId());
        for (Integer seatId : selectedSeatIds) {
            if (takenSeats.contains(seatId)) {
                throw new IllegalArgumentException("Место #" + seatId + " уже занято");
            }
        }
    }

    // ===================== ОБНОВЛЕНИЕ ИНФОРМАЦИИ =====================

    private void updateShowtimeInfo() {
        if (selectedShowtime == null) return;

        selectedFilmLabel.setText("Фильм: " + selectedShowtime.getFilmTitle());
        selectedDateTimeLabel.setText("Время: " +
                selectedShowtime.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        selectedHallLabel.setText("Зал: " + selectedShowtime.getHallName());

        int totalSeats = showtimeDAO.getSeatsForShowtime(selectedShowtime.getShowtimeId()).size();
        int takenSeats = showtimeDAO.getTakenSeats(selectedShowtime.getShowtimeId()).size();
        int freeSeats = totalSeats - takenSeats;

        selectedPriceLabel.setText(String.format("Цена: %.2f руб. | Свободно: %d/%d",
                selectedShowtime.getFinalPrice(), freeSeats, totalSeats));
    }

    private void clearFilmSelection() {
        selectedFilm = null;
        selectedShowtime = null;
        selectedSeatIds.clear();

        selectedFilmLabel.setText("Фильм: ");
        selectedDateTimeLabel.setText("Время: ");
        selectedHallLabel.setText("Зал: ");
        selectedPriceLabel.setText("Цена: ");

        seatGrid.getChildren().clear();
        updateTotalSelected();
    }

    private void clearShowtimeSelection() {
        selectedShowtime = null;
        selectedSeatIds.clear();

        selectedFilmLabel.setText("Фильм: ");
        selectedDateTimeLabel.setText("Время: ");
        selectedHallLabel.setText("Зал: ");
        selectedPriceLabel.setText("Цена: ");

        seatGrid.getChildren().clear();
        updateTotalSelected();
    }

    private void clearSaleForm() {
        customerNameField.clear();
        customerPhoneField.clear();
        customerEmailField.clear();
        selectedSeatIds.clear();
        updateTotalSelected();

        if (selectedShowtime != null) {
            renderSeats();
        }
    }

    // ===================== ОСТАЛЬНЫЕ МЕТОДЫ =====================

    private void confirmBooking() {
        Ticket selectedBooking = bookingsTable.getSelectionModel().getSelectedItem();
        if (selectedBooking == null) {
            showAlert("Предупреждение", "Выберите бронирование", Alert.AlertType.WARNING);
            return;
        }

        try {
            int paymentMethodId = getPaymentMethodId(paymentMethodCombo.getValue());

            if (bookingService.confirmBooking(
                    selectedBooking.getTicketId(),
                    currentUser.getUserId(),
                    paymentMethodId)) {

                showAlert("Успех", "Бронирование подтверждено", Alert.AlertType.INFORMATION);
                reloadData();
            } else {
                showAlert("Ошибка", "Не удалось подтвердить", Alert.AlertType.ERROR);
            }

        } catch (Exception e) {
            logger.error("Ошибка подтверждения: {}", e.getMessage());
            showAlert("Ошибка", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void cancelBooking() {
        Ticket selectedBooking = bookingsTable.getSelectionModel().getSelectedItem();
        if (selectedBooking == null) {
            showAlert("Предупреждение", "Выберите бронирование", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Отмена бронирования");
        confirm.setHeaderText("Отменить #" + selectedBooking.getTicketId() + "?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (ticketDAO.updateTicketStatus(
                        selectedBooking.getTicketId(),
                        DatabaseConfig.TicketStatus.REFUND)) {

                    showAlert("Успех", "Бронирование отменено", Alert.AlertType.INFORMATION);
                    reloadData();
                } else {
                    showAlert("Ошибка", "Не удалось отменить", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void searchCustomers() {
        String text = customerSearchField.getText().trim();
        if (text.isEmpty()) {
            loadCustomers();
        } else {
            customersList.setAll(customerDAO.searchCustomers(text));
        }
    }

    private void processRefund() {
        try {
            int ticketId = Integer.parseInt(refundTicketIdField.getText().trim());
            String reason = refundReasonField.getText().trim();

            if (reason.isEmpty()) {
                showAlert("Ошибка", "Укажите причину", Alert.AlertType.WARNING);
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Возврат билета");
            confirm.setHeaderText("Вернуть билет #" + ticketId + "?");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    if (bookingService.refundTicket(ticketId, currentUser.getUserId())) {
                        showAlert("Успех", "Возврат оформлен", Alert.AlertType.INFORMATION);
                        refundTicketIdField.clear();
                        refundReasonField.clear();
                        reloadData();
                    } else {
                        showAlert("Ошибка", "Не удалось оформить возврат", Alert.AlertType.ERROR);
                    }
                }
            });

        } catch (NumberFormatException e) {
            showAlert("Ошибка", "Некорректный номер билета", Alert.AlertType.ERROR);
        }
    }

    private int getPaymentMethodId(String methodName) {
        switch (methodName) {
            case "Банковская карта": return 1;
            case "Наличные": return 2;
            case "СБП": return 3;
            default: return 1;
        }
    }

    private void startClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            currentTimeLabel.setText(LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
        }), new KeyFrame(Duration.seconds(1)));

        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void startBookingTimer() {
        Timeline timer = new Timeline(new KeyFrame(Duration.minutes(1), e -> {
            loadActiveBookings();
            bookingService.cancelExpiredBookings();
        }));

        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/cinema/cinemamanagementsystem/login.fxml"));
            Parent root = loader.load();

            LoginController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/com/cinema/cinemamanagementsystem/style.css")).toExternalForm());

            primaryStage.setTitle("Кинотеатр 'КиноСфера' - Авторизация");
            primaryStage.setScene(scene);

        } catch (Exception e) {
            logger.error("Ошибка выхода: {}", e.getMessage());
        }
    }
}
