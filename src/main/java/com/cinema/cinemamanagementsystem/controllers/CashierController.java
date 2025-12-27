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
    @FXML private TextField registrationNameField;
    @FXML private TextField registrationPhoneField;
    @FXML private TextField registrationEmailField;
    @FXML private Button registerCustomerButton;

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

        // Устанавливаем сегодняшнюю дату
        selectedDate = LocalDate.now();
        datePicker.setValue(selectedDate);

        // Загружаем фильмы на выбранную дату
        loadFilmsForDate(selectedDate);
    }

    // ===================== НАСТРОЙКА ВКЛАДОК =====================

    private void setupSellingTab() {
        // Настройка DatePicker
        setupDatePicker();
        totalSelectedLabel.setText("Выберите сеанс для продажи");
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
        });
    }

    @FXML
    private void handleToday() {
        LocalDate today = LocalDate.now();
        datePicker.setValue(today);
        selectedDate = today;
        loadFilmsForDate(selectedDate);
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
        registerCustomerButton.setOnAction(e -> registerCustomer());
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
            Label noFilmsLabel = new Label("Нет сеансов на выбранную дату");
            noFilmsLabel.getStyleClass().add("showtime-empty");
            filmsTilePane.getChildren().add(noFilmsLabel);
        }
    }

    private VBox createFilmCard(Film film, LocalDate date, List<Showtime> filmShowtimesParam) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setPrefWidth(240);
        card.getStyleClass().add("showtime-card");
        card.setUserData(film);

        // Заголовок фильма
        Label titleLabel = new Label(film.getTitle());
        titleLabel.getStyleClass().add("showtime-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(220);

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

        VBox timesBox = new VBox(6);

        for (Showtime showtime : showtimesList) {
            HBox timeRow = new HBox(8);
            timeRow.setAlignment(Pos.CENTER_LEFT);

            // Кнопка времени
            Button timeBtn = new Button(showtime.getDateTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm")));
            timeBtn.getStyleClass().add("showtime-time");
            timeBtn.setUserData(showtime);
            timeBtn.setOnAction(e -> openTicketWindow(showtime));

            // Информация о зале и свободных местах
            Label hallLabel = new Label("Зал " + showtime.getHallName());
            hallLabel.getStyleClass().add("showtime-hall");

            int totalSeats = showtimeDAO.getSeatsForShowtime(showtime.getShowtimeId()).size();
            int takenSeats = showtimeDAO.getTakenSeats(showtime.getShowtimeId()).size();
            int freeSeats = totalSeats - takenSeats;

            Label seatsLabel = new Label(freeSeats + " свободных мест");
            String seatsClass = freeSeats == 0 ? "showtime-seats-busy" :
                    freeSeats < 10 ? "showtime-seats-warning" : "showtime-seats-ok";
            seatsLabel.getStyleClass().add(seatsClass);

            timeRow.getChildren().addAll(timeBtn, hallLabel, seatsLabel);
            timesBox.getChildren().add(timeRow);
        }

        // Если у фильма нет сеансов на эту дату
        if (showtimesList.isEmpty()) {
            Label noShowtimesLabel = new Label("Нет сеансов");
            noShowtimesLabel.getStyleClass().add("showtime-empty");
            timesBox.getChildren().add(noShowtimesLabel);
        }

        Button openButton = new Button("Открыть продажу →");
        openButton.getStyleClass().add("showtime-open");
        if (!showtimesList.isEmpty()) {
            openButton.setOnAction(e -> openTicketWindow(showtimesList.get(0)));
        } else {
            openButton.setDisable(true);
        }

        // Обработчик клика по карточке - используем effectively final переменную
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) { // Двойной клик
                if (!showtimesList.isEmpty()) {
                    openTicketWindow(showtimesList.get(0));
                }
            }
        });

        card.getChildren().addAll(titleLabel, timesBox, openButton);
        return card;
    }

    private void openTicketWindow(Showtime showtime) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/cinema/cinemamanagementsystem/ticket_sale.fxml"));
            Parent root = loader.load();

            TicketSaleController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setShowtime(showtime);
            controller.initializeData();

            Stage stage = new Stage();
            stage.setTitle("Продажа и бронирование - Кинотеатр 'КиноСфера'");
            stage.initOwner(primaryStage);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/com/cinema/cinemamanagementsystem/style.css")).toExternalForm());
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            logger.error("Ошибка при открытии окна продажи: {}", e.getMessage(), e);
            showAlert("Ошибка", "Не удалось открыть окно продажи", Alert.AlertType.ERROR);
        }
    }

    private void loadActiveBookings() {
        bookingsList.setAll(ticketDAO.getActiveBookings(false));
    }

    private void loadCustomers() {
        customersList.setAll(customerDAO.getAllCustomers());
    }

    private boolean hasShowtimesOnDate(LocalDate date) {
        return allShowtimesList.stream()
                .anyMatch(st -> st.getDateTime().toLocalDate().equals(date));
    }

    private void reloadData() {
        loadAllShowtimes();
        loadFilmsForDate(selectedDate);
        loadActiveBookings();
    }

    private void registerCustomer() {
        String name = registrationNameField.getText().trim();
        String phone = registrationPhoneField.getText().trim();
        String email = registrationEmailField.getText().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            showAlert("Ошибка", "Заполните имя и телефон клиента", Alert.AlertType.WARNING);
            return;
        }

        Customer customer = customerDAO.registerCustomer(name, phone, email.isEmpty() ? null : email);
        if (customer == null) {
            showAlert("Ошибка", "Не удалось зарегистрировать клиента", Alert.AlertType.ERROR);
            return;
        }

        showAlert("Успех", "Клиент зарегистрирован", Alert.AlertType.INFORMATION);
        registrationNameField.clear();
        registrationPhoneField.clear();
        registrationEmailField.clear();
        loadCustomers();
    }

    // ===================== ОСТАЛЬНЫЕ МЕТОДЫ =====================

    private void confirmBooking() {
        Ticket selectedBooking = bookingsTable.getSelectionModel().getSelectedItem();
        if (selectedBooking == null) {
            showAlert("Предупреждение", "Выберите бронирование", Alert.AlertType.WARNING);
            return;
        }

        try {
            int paymentMethodId = 1;

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
