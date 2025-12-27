package com.cinema.cinemamanagementsystem.controllers;

import com.cinema.cinemamanagementsystem.dao.*;
import com.cinema.cinemamanagementsystem.models.*;
import com.cinema.cinemamanagementsystem.services.BookingService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Objects;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;


public class AdminController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    // Компоненты UI
    @FXML private TabPane mainTabPane;
    @FXML private Label welcomeLabel;
    @FXML private Label currentTimeLabel;

    // Вкладка фильмы
    @FXML private TableView<Film> filmsTable;
    @FXML private TableColumn<Film, Integer> filmIdColumn;
    @FXML private TableColumn<Film, String> filmTitleColumn;
    @FXML private TableColumn<Film, String> filmGenreColumn;
    @FXML private TableColumn<Film, Integer> filmDurationColumn;
    @FXML private TextField filmSearchField;
    @FXML private Button addFilmButton;
    @FXML private Button editFilmButton;
    @FXML private Button deleteFilmButton;

    // Вкладка сеансы
    @FXML private TableView<Showtime> showtimesTable;
    @FXML private TableColumn<Showtime, Integer> showtimeIdColumn;
    @FXML private TableColumn<Showtime, String> showtimeFilmColumn;
    @FXML private TableColumn<Showtime, String> showtimeHallColumn;
    @FXML private TableColumn<Showtime, String> showtimeDateTimeColumn;
    @FXML private TableColumn<Showtime, Double> showtimePriceColumn;
    @FXML private DatePicker showtimeDatePicker;
    @FXML private ComboBox<Film> filmComboBox;
    @FXML private ComboBox<String> hallComboBox;
    @FXML private ComboBox<String> timeComboBox;
    @FXML private TextField basePriceField;
    @FXML private Button addShowtimeButton;

    // Вкладка отчеты
    @FXML private DatePicker reportStartDate;
    @FXML private DatePicker reportEndDate;
    @FXML private ComboBox<String> reportTypeComboBox;
    @FXML private TableView<?> reportTable;
    @FXML private Button generateReportButton;
    @FXML private Button exportReportButton;
    @FXML private Label totalRevenueLabel;
    @FXML private Label totalTicketsLabel;

    private Stage primaryStage;
    private User currentUser;
    private final FilmDAO filmDAO = new FilmDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final BookingService bookingService = new BookingService();

    private ObservableList<Film> filmsList = FXCollections.observableArrayList();
    private ObservableList<Showtime> showtimesList = FXCollections.observableArrayList();

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilmsTab();
        setupShowtimesTab();
        setupReportsTab();
        startClock();
    }

    public void initializeData() {
        if (currentUser != null) {
            welcomeLabel.setText("Добро пожаловать, " + currentUser.getFullName() + "!");
        }
        loadFilms();
        loadShowtimes();
        loadHalls();
        loadFilmsForComboBox();
    }

    private void setupFilmsTab() {
        // Настраиваем колонки таблицы фильмов
        filmIdColumn.setCellValueFactory(new PropertyValueFactory<>("filmId"));
        filmTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        filmGenreColumn.setCellValueFactory(new PropertyValueFactory<>("genreName"));
        filmDurationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));

        filmsTable.setItems(filmsList);

        // Обработчики кнопок
        addFilmButton.setOnAction(e -> showAddFilmDialog());
        editFilmButton.setOnAction(e -> editSelectedFilm());
        deleteFilmButton.setOnAction(e -> deleteSelectedFilm());

        // Поиск фильмов
        filmSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchFilms(newValue);
        });
    }

    private void setupShowtimesTab() {
        // Настраиваем колонки таблицы сеансов
        showtimeIdColumn.setCellValueFactory(new PropertyValueFactory<>("showtimeId"));
        showtimeFilmColumn.setCellValueFactory(new PropertyValueFactory<>("filmTitle"));
        showtimeHallColumn.setCellValueFactory(new PropertyValueFactory<>("hallName"));
        showtimeDateTimeColumn.setCellValueFactory(cellData -> {
            Showtime showtime = cellData.getValue();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            return new SimpleStringProperty(
                    showtime.getDateTime().format(formatter)
            );

        });
        showtimePriceColumn.setCellValueFactory(new PropertyValueFactory<>("finalPrice"));

        showtimesTable.setItems(showtimesList);

        // Настраиваем комбобоксы
        timeComboBox.getItems().addAll(
                "10:00", "12:00", "14:00", "16:00", "18:00", "20:00", "22:00"
        );

        addShowtimeButton.setOnAction(e -> addShowtime());
    }

    private void setupReportsTab() {
        reportTypeComboBox.getItems().addAll(
                "Продажи за период",
                "Популярность фильмов",
                "Загрузка залов",
                "Активность клиентов"
        );
        reportTypeComboBox.setValue("Продажи за период");

        // Устанавливаем даты по умолчанию (последние 30 дней)
        reportStartDate.setValue(java.time.LocalDate.now().minusDays(30));
        reportEndDate.setValue(java.time.LocalDate.now());

        generateReportButton.setOnAction(e -> generateReport());
        exportReportButton.setOnAction(e -> exportReport());
    }

    private void loadFilms() {
        filmsList.setAll(filmDAO.getAllFilms());
    }

    private void loadShowtimes() {
        showtimesList.setAll(showtimeDAO.getAllShowtimes());
    }

    private void loadHalls() {
        hallComboBox.getItems().clear();
        hallComboBox.getItems().addAll("Зал №1 (IMAX)", "Зал №2 (3D)", "Зал №3 (Dolby Atmos)", "Зал №4 (Premium)");
    }

    private void loadFilmsForComboBox() {
        filmComboBox.getItems().clear();
        filmComboBox.getItems().addAll(filmDAO.getAllFilms());
    }

    private void searchFilms(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            loadFilms();
        } else {
            filmsList.setAll(filmDAO.searchFilms(searchText));
        }
    }

    private void showAddFilmDialog() {
        Dialog<Film> dialog = new Dialog<>();
        dialog.setTitle("Добавить фильм");
        dialog.setHeaderText("Введите информацию о новом фильме");

        // Создаем поля формы
        TextField titleField = new TextField();
        titleField.setPromptText("Название фильма");

        ComboBox<String> genreCombo = new ComboBox<>();
        genreCombo.getItems().addAll("Фантастика", "Драма", "Боевик", "Комедия", "Ужасы");
        genreCombo.setPromptText("Выберите жанр");

        TextField durationField = new TextField();
        durationField.setPromptText("Длительность (минут)");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Описание фильма");
        descriptionArea.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        grid.add(new Label("Название:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Жанр:"), 0, 1);
        grid.add(genreCombo, 1, 1);
        grid.add(new Label("Длительность:"), 0, 2);
        grid.add(durationField, 1, 2);
        grid.add(new Label("Описание:"), 0, 3);
        grid.add(descriptionArea, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Добавляем кнопки
        ButtonType addButtonType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Валидация
        Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);

        ChangeListener<String> fieldListener = (observable, oldValue, newValue) -> {
            addButton.setDisable(
                    titleField.getText().trim().isEmpty() ||
                            genreCombo.getValue() == null ||
                            durationField.getText().trim().isEmpty()
            );
        };

        titleField.textProperty().addListener(fieldListener);
        genreCombo.valueProperty().addListener((observable, oldValue, newValue) ->
                fieldListener.changed(null, null, null));
        durationField.textProperty().addListener(fieldListener);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    Film film = new Film();
                    film.setTitle(titleField.getText().trim());
                    film.setDuration(Integer.parseInt(durationField.getText().trim()));
                    film.setDescription(descriptionArea.getText().trim());
                    // Здесь нужно преобразовать жанр в ID
                    return film;
                } catch (NumberFormatException e) {
                    showAlert("Ошибка", "Некорректная длительность фильма", Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(film -> {
            if (filmDAO.addFilm(film)) {
                showAlert("Успех", "Фильм успешно добавлен", Alert.AlertType.INFORMATION);
                loadFilms();
            } else {
                showAlert("Ошибка", "Не удалось добавить фильм", Alert.AlertType.ERROR);
            }
        });
    }

    private void editSelectedFilm() {
        Film selected = filmsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Предупреждение", "Выберите фильм для редактирования", Alert.AlertType.WARNING);
            return;
        }
        // Реализация редактирования фильма
    }

    private void deleteSelectedFilm() {
        Film selected = filmsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Предупреждение", "Выберите фильм для удаления", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение удаления");
        confirmAlert.setHeaderText("Удалить фильм '" + selected.getTitle() + "'?");
        confirmAlert.setContentText("Это действие нельзя отменить.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (filmDAO.deleteFilm(selected.getFilmId())) {
                    showAlert("Успех", "Фильм успешно удален", Alert.AlertType.INFORMATION);
                    loadFilms();
                } else {
                    showAlert("Ошибка", "Не удалось удалить фильм", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void addShowtime() {
        try {
            Film selectedFilm = filmComboBox.getValue();
            String hall = hallComboBox.getValue();
            String time = timeComboBox.getValue();
            java.time.LocalDate date = showtimeDatePicker.getValue();

            if (selectedFilm == null || hall == null || date == null || time == null) {
                showAlert("Предупреждение", "Заполните все поля", Alert.AlertType.WARNING);
                return;
            }

            // Парсим время и создаем LocalDateTime
            String[] timeParts = time.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            LocalDateTime dateTime = LocalDateTime.of(date.getYear(), date.getMonth(),
                    date.getDayOfMonth(), hour, minute);

            // Проверяем пересечение времени
            LocalDateTime endTime = dateTime.plusMinutes(selectedFilm.getDuration() + 15);
            int hallId = getHallIdFromName(hall);

            if (showtimeDAO.hasTimeConflict(hallId, dateTime, endTime, null)) {
                showAlert("Ошибка", "В этом зале уже есть сеанс в выбранное время", Alert.AlertType.ERROR);
                return;
            }

            // Создаем сеанс
            Showtime showtime = new Showtime();
            showtime.setFilmId(selectedFilm.getFilmId());
            showtime.setHallId(hallId);
            showtime.setDateTime(dateTime);
            showtime.setBasePrice(Double.parseDouble(basePriceField.getText()));
            // Здесь нужно определить rule_id по времени

            if (showtimeDAO.addShowtime(showtime)) {
                showAlert("Успех", "Сеанс успешно добавлен", Alert.AlertType.INFORMATION);
                loadShowtimes();
                clearShowtimeForm();
            } else {
                showAlert("Ошибка", "Не удалось добавить сеанс", Alert.AlertType.ERROR);
            }

        } catch (Exception e) {
            logger.error("Ошибка при добавлении сеанса: {}", e.getMessage());
            showAlert("Ошибка", "Произошла ошибка: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private int getHallIdFromName(String hallName) {
        switch (hallName) {
            case "Зал №1 (IMAX)": return 1;
            case "Зал №2 (3D)": return 2;
            case "Зал №3 (Dolby Atmos)": return 3;
            case "Зал №4 (Premium)": return 4;
            default: return 1;
        }
    }

    private void clearShowtimeForm() {
        filmComboBox.setValue(null);
        hallComboBox.setValue(null);
        showtimeDatePicker.setValue(null);
        timeComboBox.setValue(null);
        basePriceField.clear();
    }

    private void generateReport() {
        String reportType = reportTypeComboBox.getValue();
        java.time.LocalDate startDate = reportStartDate.getValue();
        java.time.LocalDate endDate = reportEndDate.getValue();

        if (startDate == null || endDate == null) {
            showAlert("Предупреждение", "Выберите период отчета", Alert.AlertType.WARNING);
            return;
        }

        if (startDate.isAfter(endDate)) {
            showAlert("Ошибка", "Начальная дата не может быть позже конечной", Alert.AlertType.ERROR);
            return;
        }

        // Здесь должна быть логика генерации отчета
        // В реальной системе здесь будут запросы к БД

        switch (reportType) {
            case "Продажи за период":
                generateSalesReport(startDate, endDate);
                break;
            case "Популярность фильмов":
                generateFilmPopularityReport(startDate, endDate);
                break;
            case "Загрузка залов":
                generateHallOccupancyReport(startDate, endDate);
                break;
            case "Активность клиентов":
                generateCustomerActivityReport(startDate, endDate);
                break;
        }
    }

    private void generateSalesReport(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        // Реализация отчета по продажам
        totalRevenueLabel.setText("Общая выручка: рассчитывается...");
        totalTicketsLabel.setText("Продано билетов: рассчитывается...");
    }

    private void generateFilmPopularityReport(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        // Реализация отчета по популярности фильмов
    }

    private void generateHallOccupancyReport(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        // Реализация отчета по загрузке залов
    }

    private void generateCustomerActivityReport(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        // Реализация отчета по активности клиентов
    }

    private void exportReport() {
        // Реализация экспорта отчета в Excel/PDF
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Экспорт отчета");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel файлы", "*.xlsx")
        );

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            // Логика экспорта
            showAlert("Успех", "Отчет успешно экспортирован", Alert.AlertType.INFORMATION);
        }
    }

    private void startClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
            currentTimeLabel.setText(LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
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
            // Возвращаемся к окну авторизации
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cinema/fxml/login.fxml"));
            Parent root = loader.load();

            LoginController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/com/cinema/styles/styles.css")).toExternalForm());

            primaryStage.setTitle("Кинотеатр 'КиноСфера' - Авторизация");
            primaryStage.setScene(scene);

        } catch (Exception e) {
            logger.error("Ошибка при выходе из системы: {}", e.getMessage());
        }
    }

    @FXML
    private void handleExit() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Выход из системы");
        confirmAlert.setHeaderText("Вы уверены, что хотите выйти?");
        confirmAlert.setContentText("Все несохраненные данные будут потеряны.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                primaryStage.close();
            }
        });
    }
}