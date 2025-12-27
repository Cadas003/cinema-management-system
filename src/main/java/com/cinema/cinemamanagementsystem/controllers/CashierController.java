package com.cinema.cinemamanagementsystem.controllers;

import com.cinema.cinemamanagementsystem.dao.*;
import com.cinema.cinemamanagementsystem.models.*;
import com.cinema.cinemamanagementsystem.services.BookingService;
import com.cinema.cinemamanagementsystem.config.DatabaseConfig;
import com.cinema.cinemamanagementsystem.ui.ConnectionStatusMonitor;
import com.cinema.cinemamanagementsystem.ui.OperationHistory;
import com.cinema.cinemamanagementsystem.ui.OperationRecord;
import com.cinema.cinemamanagementsystem.ui.UiDialogs;
import com.cinema.cinemamanagementsystem.ui.ViewStateController;
import javafx.animation.PauseTransition;
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
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
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
    @FXML private BorderPane cashierRoot;
    @FXML private TabPane mainTabPane;
    @FXML private Label welcomeLabel;
    @FXML private Label currentTimeLabel;
    @FXML private Label connectionStatusLabel;

    // Вкладка ПРОДАЖА
    @FXML private DatePicker datePicker;
    @FXML private TilePane filmsTilePane;
    @FXML private ScrollPane showtimesScrollPane;
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
    @FXML private ComboBox<String> customerFilterCombo;
    @FXML private Button searchCustomerButton;
    @FXML private TextField registrationNameField;
    @FXML private TextField registrationPhoneField;
    @FXML private TextField registrationEmailField;
    @FXML private Button registerCustomerButton;

    // Вкладка ВОЗВРАТЫ
    @FXML private TextField refundTicketIdField;
    @FXML private TextField refundReasonField;
    @FXML private Button refundButton;
    @FXML private TableView<OperationRecord> recentTicketsTable;
    @FXML private StackPane showtimesStatePane;
    @FXML private StackPane bookingsStatePane;
    @FXML private StackPane customersStatePane;
    @FXML private StackPane recentTicketsStatePane;

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
    private List<Customer> allCustomers = new ArrayList<>();

    // Текущие выборы
    private LocalDate selectedDate;
    private User currentUser;
    private Stage primaryStage;
    private ViewStateController showtimesStateController;
    private ViewStateController bookingsStateController;
    private ViewStateController customersStateController;
    private ViewStateController recentTicketsStateController;
    private PauseTransition customerSearchDebounce;
    private final ConnectionStatusMonitor connectionStatusMonitor = new ConnectionStatusMonitor();

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
        setupStateControllers();
        setupAccelerators();
    }

    public void initializeData() {
        if (currentUser != null) {
            welcomeLabel.setText("Кассир: " + currentUser.getFullName());
        }

        categoryMap = seatCategoryDAO.getAllCategories();

        // Устанавливаем сегодняшнюю дату
        selectedDate = LocalDate.now();
        datePicker.setValue(selectedDate);

        // Загружаем данные
        loadAllShowtimes();
        loadActiveBookings();
        loadCustomers();
        setupRecentTicketsTable();

        // Загружаем фильмы на выбранную дату
        loadFilmsForDate(selectedDate);

        customerSearchDebounce = new PauseTransition(Duration.millis(280));
        customerSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            customerSearchDebounce.setOnFinished(event -> filterCustomers());
            customerSearchDebounce.playFromStart();
        });
        customerFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> filterCustomers());

        connectionStatusMonitor.attach(connectionStatusLabel);
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
        confirmBookingButton.setTooltip(new Tooltip("Подтвердить бронирование (Ctrl+S)"));
        cancelBookingButton.setTooltip(new Tooltip("Отменить бронирование"));
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
        customerFilterCombo.getItems().addAll("Все клиенты", "Частые (5+)", "Новые");
        customerFilterCombo.setValue("Все клиенты");
        searchCustomerButton.setTooltip(new Tooltip("Найти клиента (Ctrl+F)"));
        registerCustomerButton.setTooltip(new Tooltip("Добавить нового клиента"));
    }

    private void setupRefundsTab() {
        refundButton.setOnAction(e -> processRefund());
        refundButton.setTooltip(new Tooltip("Оформить возврат (Ctrl+S)"));
    }

    // ===================== ЗАГРУЗКА ДАННЫХ =====================

    private void loadAllShowtimes() {
        showtimesStateController.showLoading("Обновляем расписание");
        Task<List<Showtime>> task = new Task<>() {
            @Override
            protected List<Showtime> call() {
                return showtimeDAO.getFutureShowtimes();
            }
        };
        task.setOnSucceeded(event -> {
            allShowtimesList.setAll(task.getValue());
            if (allShowtimesList.isEmpty()) {
                showtimesStateController.showEmpty(
                        "Сеансов нет",
                        "На выбранную дату еще нет расписания",
                        "Обновить",
                        this::loadAllShowtimes
                );
            } else {
                showtimesStateController.showContent();
            }
            loadFilmsForDate(selectedDate);
        });
        task.setOnFailed(event -> showtimesStateController.showError(
                "Не удалось загрузить расписание",
                this::loadAllShowtimes
        ));
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
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

        if (films.isEmpty()) {
            showtimesStateController.showEmpty(
                    "Нет сеансов на выбранную дату",
                    "Выберите другую дату или обновите расписание",
                    "Обновить",
                    this::reloadData
            );
        } else {
            showtimesStateController.showContent();
        }
    }

    private VBox createFilmCard(Film film, LocalDate date, List<Showtime> filmShowtimesParam) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(12));
        card.setPrefWidth(220);
        card.getStyleClass().add("showtime-card");
        card.setUserData(film);

        // Заголовок фильма
        Label titleLabel = new Label(film.getTitle());
        titleLabel.getStyleClass().add("showtime-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(200);

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

        VBox timesBox = new VBox(4);

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
            hallLabel.setWrapText(true);
            hallLabel.setMaxWidth(120);

            int totalSeats = showtimeDAO.getSeatsForShowtime(showtime.getShowtimeId()).size();
            int takenSeats = showtimeDAO.getTakenSeats(showtime.getShowtimeId()).size();
            int freeSeats = totalSeats - takenSeats;

            Label seatsLabel = new Label(freeSeats + " свободных мест");
            String seatsClass = freeSeats == 0 ? "showtime-seats-busy" :
                    freeSeats < 10 ? "showtime-seats-warning" : "showtime-seats-ok";
            seatsLabel.getStyleClass().add(seatsClass);
            seatsLabel.setWrapText(true);
            seatsLabel.setMaxWidth(140);

            timeRow.getChildren().addAll(timeBtn, hallLabel, seatsLabel);
            timesBox.getChildren().add(timeRow);
        }

        // Если у фильма нет сеансов на эту дату
        if (showtimesList.isEmpty()) {
            Label noShowtimesLabel = new Label("Нет сеансов");
            noShowtimesLabel.getStyleClass().add("showtime-empty");
            timesBox.getChildren().add(noShowtimesLabel);
        }

        // Обработчик клика по карточке - используем effectively final переменную
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) { // Двойной клик
                if (!showtimesList.isEmpty()) {
                    openTicketWindow(showtimesList.get(0));
                }
            }
        });

        card.getChildren().addAll(titleLabel, timesBox);
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
            UiDialogs.showErrorDialog(primaryStage, "Ошибка", "Не удалось открыть окно продажи");
        }
    }

    private void loadActiveBookings() {
        bookingsStateController.showLoading("Загружаем бронирования");
        Task<List<Ticket>> task = new Task<>() {
            @Override
            protected List<Ticket> call() {
                return ticketDAO.getActiveBookings(false);
            }
        };
        task.setOnSucceeded(event -> {
            bookingsList.setAll(task.getValue());
            if (bookingsList.isEmpty()) {
                bookingsStateController.showEmpty(
                        "Бронирований нет",
                        "Новые брони появятся здесь автоматически",
                        null,
                        null
                );
            } else {
                bookingsStateController.showContent();
            }
        });
        task.setOnFailed(event -> bookingsStateController.showError(
                "Не удалось загрузить бронирования",
                this::loadActiveBookings
        ));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadCustomers() {
        customersStateController.showLoading("Загружаем клиентов");
        Task<List<Customer>> task = new Task<>() {
            @Override
            protected List<Customer> call() {
                return customerDAO.getAllCustomers();
            }
        };
        task.setOnSucceeded(event -> {
            allCustomers = new ArrayList<>(task.getValue());
            filterCustomers();
        });
        task.setOnFailed(event -> customersStateController.showError(
                "Не удалось загрузить клиентов",
                this::loadCustomers
        ));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
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
            UiDialogs.showInfoDialog(primaryStage, "Ошибка", "Заполните имя и телефон клиента");
            return;
        }

        if (!phone.matches("\\\\+?\\\\d{10,15}")) {
            UiDialogs.showInfoDialog(primaryStage, "Ошибка", "Введите корректный номер телефона");
            return;
        }

        Customer customer = customerDAO.registerCustomer(name, phone, email.isEmpty() ? null : email);
        if (customer == null) {
            UiDialogs.showErrorDialog(primaryStage, "Ошибка", "Не удалось зарегистрировать клиента");
            return;
        }

        UiDialogs.successToast(primaryStage, "Клиент зарегистрирован");
        registrationNameField.clear();
        registrationPhoneField.clear();
        registrationEmailField.clear();
        loadCustomers();
    }

    // ===================== ОСТАЛЬНЫЕ МЕТОДЫ =====================

    private void confirmBooking() {
        Ticket selectedBooking = bookingsTable.getSelectionModel().getSelectedItem();
        if (selectedBooking == null) {
            UiDialogs.showInfoDialog(primaryStage, "Бронирование", "Выберите бронирование");
            return;
        }

        try {
            int paymentMethodId = 1;

                if (bookingService.confirmBooking(
                    selectedBooking.getTicketId(),
                    currentUser.getUserId(),
                    paymentMethodId)) {

                UiDialogs.successToast(primaryStage, "Бронирование подтверждено");
                OperationHistory.addRecord(new OperationRecord(
                        LocalDateTime.now(),
                        "Подтверждение бронирования",
                        selectedBooking.getFinalPrice(),
                        "#" + selectedBooking.getTicketId()
                ));
                reloadData();
            } else {
                UiDialogs.showErrorDialog(primaryStage, "Ошибка", "Не удалось подтвердить");
            }

        } catch (Exception e) {
            logger.error("Ошибка подтверждения: {}", e.getMessage());
            UiDialogs.showErrorDialog(primaryStage, "Ошибка", e.getMessage());
        }
    }

    private void cancelBooking() {
        Ticket selectedBooking = bookingsTable.getSelectionModel().getSelectedItem();
        if (selectedBooking == null) {
            UiDialogs.showInfoDialog(primaryStage, "Бронирование", "Выберите бронирование");
            return;
        }

        if (UiDialogs.confirmDialog(primaryStage,
                "Отмена бронирования",
                "Отменить #" + selectedBooking.getTicketId() + "?",
                "Отменить")) {
            if (ticketDAO.updateTicketStatus(
                    selectedBooking.getTicketId(),
                    DatabaseConfig.TicketStatus.REFUND)) {

                UiDialogs.successToast(primaryStage, "Бронирование отменено");
                reloadData();
            } else {
                UiDialogs.showErrorDialog(primaryStage, "Ошибка", "Не удалось отменить");
            }
        }
    }

    private void searchCustomers() {
        filterCustomers();
    }

    private void processRefund() {
        try {
            int ticketId = Integer.parseInt(refundTicketIdField.getText().trim());
            String reason = refundReasonField.getText().trim();

            if (reason.isEmpty()) {
                UiDialogs.showInfoDialog(primaryStage, "Возврат", "Укажите причину");
                return;
            }

            if (UiDialogs.confirmDialog(primaryStage,
                    "Возврат билета",
                    "Вернуть билет #" + ticketId + "?",
                    "Вернуть")) {
                if (bookingService.refundTicket(ticketId, currentUser.getUserId())) {
                    UiDialogs.successToast(primaryStage, "Возврат оформлен");
                    OperationHistory.addRecord(new OperationRecord(
                            LocalDateTime.now(),
                            "Возврат билета",
                            0,
                            "#" + ticketId
                    ));
                    refundTicketIdField.clear();
                    refundReasonField.clear();
                    reloadData();
                } else {
                    UiDialogs.showErrorDialog(primaryStage, "Ошибка", "Не удалось оформить возврат");
                }
            }

        } catch (NumberFormatException e) {
            UiDialogs.showErrorDialog(primaryStage, "Ошибка", "Некорректный номер билета");
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

    private void filterCustomers() {
        String searchText = customerSearchField.getText() == null ? "" : customerSearchField.getText().trim().toLowerCase();
        String filter = customerFilterCombo.getValue();

        List<Customer> filtered = allCustomers.stream()
                .filter(customer -> {
                    boolean matchesSearch = searchText.isEmpty()
                            || (customer.getName() != null && customer.getName().toLowerCase().contains(searchText))
                            || (customer.getPhone() != null && customer.getPhone().toLowerCase().contains(searchText))
                            || (customer.getEmail() != null && customer.getEmail().toLowerCase().contains(searchText));
                    boolean matchesFilter = true;
                    if ("Частые (5+)".equals(filter)) {
                        matchesFilter = customer.getVisitCount() >= 5;
                    } else if ("Новые".equals(filter)) {
                        matchesFilter = customer.getVisitCount() <= 1;
                    }
                    return matchesSearch && matchesFilter;
                })
                .collect(Collectors.toList());

        customersList.setAll(filtered);
        if (filtered.isEmpty()) {
            customersStateController.showEmpty(
                    "Клиенты не найдены",
                    "Измените параметры поиска или фильтра",
                    "Сбросить",
                    this::resetCustomerFilters
            );
        } else {
            customersStateController.showContent();
        }
    }

    private void resetCustomerFilters() {
        customerSearchField.clear();
        customerFilterCombo.setValue("Все клиенты");
        customersList.setAll(allCustomers);
        if (customersList.isEmpty()) {
            customersStateController.showEmpty(
                    "Список клиентов пуст",
                    "Добавьте первого клиента через форму регистрации",
                    "Добавить клиента",
                    registrationNameField::requestFocus
            );
        } else {
            customersStateController.showContent();
        }
    }

    private void setupStateControllers() {
        showtimesStateController = new ViewStateController(showtimesStatePane, showtimesScrollPane);
        bookingsStateController = new ViewStateController(bookingsStatePane, bookingsTable);
        customersStateController = new ViewStateController(customersStatePane, customersTable);
        recentTicketsStateController = new ViewStateController(recentTicketsStatePane, recentTicketsTable);
        recentTicketsStateController.showEmpty(
                "История пуста",
                "Последние операции появятся после продаж",
                null,
                null
        );
    }

    private void setupRecentTicketsTable() {
        TableColumn<OperationRecord, String> timeColumn = new TableColumn<>("Время");
        timeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFormattedTime()));
        timeColumn.setPrefWidth(120);

        TableColumn<OperationRecord, String> descriptionColumn = new TableColumn<>("Операция");
        descriptionColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescription()));
        descriptionColumn.setPrefWidth(200);

        TableColumn<OperationRecord, String> amountColumn = new TableColumn<>("Сумма");
        amountColumn.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.2f", cell.getValue().getAmount())));
        amountColumn.setPrefWidth(100);

        TableColumn<OperationRecord, String> ticketsColumn = new TableColumn<>("Билеты");
        ticketsColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTicketIds()));
        ticketsColumn.setPrefWidth(160);

        recentTicketsTable.getColumns().setAll(timeColumn, descriptionColumn, amountColumn, ticketsColumn);
        recentTicketsTable.setItems(OperationHistory.getRecords());

        updateRecentTicketsState();
        OperationHistory.getRecords().addListener((ListChangeListener<OperationRecord>) change -> updateRecentTicketsState());
    }

    private void updateRecentTicketsState() {
        if (OperationHistory.getRecords().isEmpty()) {
            recentTicketsStateController.showEmpty(
                    "История пуста",
                    "Последние операции появятся после продаж",
                    null,
                    null
            );
        } else {
            recentTicketsStateController.showContent();
        }
    }

    private void setupAccelerators() {
        cashierRoot.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene == null) {
                return;
            }

            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), this::reloadData);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), this::focusSearchField);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::performPrimaryAction);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), this::handleLogout);
        });
    }

    private void focusSearchField() {
        int selectedIndex = mainTabPane.getSelectionModel().getSelectedIndex();
        if (selectedIndex == 2) {
            customerSearchField.requestFocus();
        }
    }

    private void performPrimaryAction() {
        int selectedIndex = mainTabPane.getSelectionModel().getSelectedIndex();
        if (selectedIndex == 1) {
            confirmBooking();
        } else if (selectedIndex == 3) {
            processRefund();
        }
    }
}
