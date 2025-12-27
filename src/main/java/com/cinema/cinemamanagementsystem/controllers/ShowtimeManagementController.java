package com.cinema.cinemamanagementsystem.controllers;

import com.cinema.cinemamanagementsystem.dao.FilmDAO;
import com.cinema.cinemamanagementsystem.dao.ShowtimeDAO;
import com.cinema.cinemamanagementsystem.models.Film;
import com.cinema.cinemamanagementsystem.models.Showtime;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.stage.FileChooser;
import java.io.File;

import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.Node;
import java.util.stream.Collectors;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ShowtimeManagementController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(ShowtimeManagementController.class);

    @FXML private ComboBox<Film> filmComboBox;
    @FXML private DatePicker showtimeDatePicker;
    @FXML private ComboBox<String> timeComboBox;
    @FXML private ComboBox<String> hallComboBox;
    @FXML private TextField basePriceField;
    @FXML private ComboBox<String> showtimeTypeCombo;
    @FXML private CheckBox is3dCheckbox;
    @FXML private CheckBox hasSubtitlesCheckbox;
    @FXML private CheckBox isDubbedCheckbox;

    @FXML private TableView<Showtime> showtimesTable;
    @FXML private TableColumn<Showtime, String> timeColumn;
    @FXML private TableColumn<Showtime, String> filmColumn;
    @FXML private TableColumn<Showtime, String> hallColumn;
    @FXML private TableColumn<Showtime, String> typeColumn;
    @FXML private TableColumn<Showtime, String> priceColumn;
    @FXML private TableColumn<Showtime, String> bookedColumn;
    @FXML private TableColumn<Showtime, String> availableColumn;
    @FXML private TableColumn<Showtime, String> statusColumn;

    @FXML private DatePicker scheduleDateFilter;
    @FXML private ComboBox<String> hallFilter;
    @FXML private Button addShowtimeButton;
    @FXML private Button checkConflictsButton;
    @FXML private Button refreshButton;
    @FXML private Button exportScheduleButton;
    @FXML private Label conflictLabel;
    @FXML private Label scheduleStatusLabel;
    @FXML private Label lastUpdateLabel;

    private Stage stage;
    private final FilmDAO filmDAO = new FilmDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private ObservableList<Showtime> showtimesList = FXCollections.observableArrayList();
    private ObservableList<Film> filmsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        loadData();
        setupEventHandlers();
        updateStatus();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void setupUI() {
        // Настройка времени
        timeComboBox.getItems().clear();
        for (int hour = 9; hour <= 23; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                String time = String.format("%02d:%02d", hour, minute);
                timeComboBox.getItems().add(time);
            }
        }

        // Настройка залов
        hallComboBox.getItems().addAll(
                "Зал №1 (IMAX)",
                "Зал №2 (3D)",
                "Зал №3 (Dolby Atmos)",
                "Зал №4 (Premium)"
        );

        hallFilter.getItems().add("Все залы");
        hallFilter.getItems().addAll(hallComboBox.getItems());
        hallFilter.setValue("Все залы");

        // Настройка типа сеанса
        showtimeTypeCombo.getItems().addAll(
                "Стандартный",
                "Премьера",
                "Ночной",
                "Утренний",
                "Детский",
                "Семейный"
        );
        showtimeTypeCombo.setValue("Стандартный");

        // Настройка таблицы
        setupTable();

        // Установка текущей даты
        showtimeDatePicker.setValue(LocalDate.now());
        scheduleDateFilter.setValue(LocalDate.now());

        // Установка текущего времени (ближайший временной слот)
        setNearestTimeSlot();
    }

    private void setupTable() {
        timeColumn.setCellValueFactory(cellData -> {
            Showtime showtime = cellData.getValue();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            return new javafx.beans.property.SimpleStringProperty(
                    showtime.getDateTime().format(formatter)
            );
        });

        filmColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFilmTitle()));

        hallColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getHallName()));

        typeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRuleName()));

        priceColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("%.2f руб.", cellData.getValue().getFinalPrice())
                ));

        // Заглушки для остальных колонок
        bookedColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty("0"));

        availableColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty("30"));

        statusColumn.setCellValueFactory(cellData -> {
            Showtime showtime = cellData.getValue();
            if (showtime.getDateTime().isBefore(LocalDateTime.now())) {
                return new javafx.beans.property.SimpleStringProperty("Завершен");
            } else if (showtime.getDateTime().isBefore(LocalDateTime.now().plusHours(1))) {
                return new javafx.beans.property.SimpleStringProperty("Скоро");
            } else {
                return new javafx.beans.property.SimpleStringProperty("Будущий");
            }
        });

        showtimesTable.setItems(showtimesList);
    }

    private void loadData() {
        // Загрузка фильмов
        filmsList.setAll(filmDAO.getAllFilms());
        filmComboBox.setItems(filmsList);

        // Загрузка сеансов
        loadShowtimes();
    }

    private void loadShowtimes() {
        LocalDate filterDate = scheduleDateFilter.getValue();
        String selectedHall = hallFilter.getValue();

        List<Showtime> allShowtimes = showtimeDAO.getAllShowtimes();

        List<Showtime> filtered = allShowtimes.stream()
                .filter(showtime -> {
                    boolean matchesDate = filterDate == null ||
                            showtime.getDateTime().toLocalDate().equals(filterDate);

                    boolean matchesHall = "Все залы".equals(selectedHall) ||
                            showtime.getHallName().equals(selectedHall);

                    return matchesDate && matchesHall;
                })
                .collect(Collectors.toList());


        showtimesList.setAll(filtered);
        updateStatus();
    }

    private void setupEventHandlers() {
        addShowtimeButton.setOnAction(e -> addShowtime());
        checkConflictsButton.setOnAction(e -> checkScheduleConflicts());
        refreshButton.setOnAction(e -> refreshSchedule());
        exportScheduleButton.setOnAction(e -> exportSchedule());

        // Автоматическая загрузка при изменении фильтров
        scheduleDateFilter.valueProperty().addListener((observable, oldValue, newValue) ->
                loadShowtimes());
        hallFilter.valueProperty().addListener((observable, oldValue, newValue) ->
                loadShowtimes());
    }

    @FXML
    private void addShowtime() {
        try {
            // Валидация
            if (filmComboBox.getValue() == null) {
                showAlert("Ошибка", "Выберите фильм", Alert.AlertType.ERROR);
                return;
            }

            if (showtimeDatePicker.getValue() == null) {
                showAlert("Ошибка", "Выберите дату", Alert.AlertType.ERROR);
                return;
            }

            if (timeComboBox.getValue() == null) {
                showAlert("Ошибка", "Выберите время", Alert.AlertType.ERROR);
                return;
            }

            if (hallComboBox.getValue() == null) {
                showAlert("Ошибка", "Выберите зал", Alert.AlertType.ERROR);
                return;
            }

            if (basePriceField.getText().trim().isEmpty()) {
                showAlert("Ошибка", "Введите цену", Alert.AlertType.ERROR);
                return;
            }

            // Парсинг времени
            String[] timeParts = timeComboBox.getValue().split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            LocalDateTime dateTime = LocalDateTime.of(
                    showtimeDatePicker.getValue(),
                    LocalTime.of(hour, minute)
            );

            // Проверка на будущее время
            if (dateTime.isBefore(LocalDateTime.now())) {
                showAlert("Ошибка", "Нельзя создать сеанс в прошлом", Alert.AlertType.ERROR);
                return;
            }

            Film selectedFilm = filmComboBox.getValue();
            LocalDateTime endTime = dateTime.plusMinutes(selectedFilm.getDuration() + 15); // +15 мин на уборку

            int hallId = getHallIdFromName(hallComboBox.getValue());

            // Проверка пересечений
            if (showtimeDAO.hasTimeConflict(hallId, dateTime, endTime, null)) {
                showAlert("Ошибка", "В этом зале уже есть сеанс в выбранное время",
                        Alert.AlertType.ERROR);
                return;
            }

            // Создание сеанса
            Showtime showtime = new Showtime();
            showtime.setFilmId(selectedFilm.getFilmId());
            showtime.setHallId(hallId);
            showtime.setDateTime(dateTime);
            showtime.setBasePrice(Double.parseDouble(basePriceField.getText()));

            // Определение rule_id по типу сеанса и времени
            int ruleId = determineRuleId(dateTime, showtimeTypeCombo.getValue());
            showtime.setRuleId(ruleId);

            // Сохранение
            if (showtimeDAO.addShowtime(showtime)) {
                showAlert("Успех", "Сеанс успешно добавлен", Alert.AlertType.INFORMATION);
                clearForm();
                loadShowtimes();
            } else {
                showAlert("Ошибка", "Не удалось добавить сеанс", Alert.AlertType.ERROR);
            }

        } catch (NumberFormatException e) {
            showAlert("Ошибка", "Некорректная цена", Alert.AlertType.ERROR);
        } catch (Exception e) {
            logger.error("Ошибка при добавлении сеанса: {}", e.getMessage());
            showAlert("Ошибка", "Произошла ошибка: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void checkScheduleConflicts() {
        if (filmComboBox.getValue() == null || showtimeDatePicker.getValue() == null ||
                timeComboBox.getValue() == null || hallComboBox.getValue() == null) {
            conflictLabel.setText("Заполните все поля для проверки");
            conflictLabel.setStyle("-fx-text-fill: #f39c12;");
            return;
        }

        try {
            String[] timeParts = timeComboBox.getValue().split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            LocalDateTime dateTime = LocalDateTime.of(
                    showtimeDatePicker.getValue(),
                    LocalTime.of(hour, minute)
            );

            Film selectedFilm = filmComboBox.getValue();
            LocalDateTime endTime = dateTime.plusMinutes(selectedFilm.getDuration() + 15);

            int hallId = getHallIdFromName(hallComboBox.getValue());

            if (showtimeDAO.hasTimeConflict(hallId, dateTime, endTime, null)) {
                conflictLabel.setText("⚠ Найдены пересечения с другими сеансами!");
                conflictLabel.setStyle("-fx-text-fill: #e74c3c;");
            } else {
                conflictLabel.setText("✓ Время свободно, пересечений нет");
                conflictLabel.setStyle("-fx-text-fill: #27ae60;");
            }

        } catch (Exception e) {
            conflictLabel.setText("Ошибка при проверке");
            conflictLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    @FXML
    private void refreshSchedule() {
        loadShowtimes();
        lastUpdateLabel.setText(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("HH:mm:ss")));
        showAlert("Обновление", "Расписание обновлено", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void exportSchedule() {
        // Реализация экспорта расписания
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Экспорт расписания");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel файлы", "*.xlsx")
        );

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            // Здесь будет логика экспорта
            showAlert("Успех", "Расписание экспортировано в: " + file.getAbsolutePath(),
                    Alert.AlertType.INFORMATION);
        }
    }

    @FXML
    private void editSelectedShowtime() {
        Showtime selected = showtimesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Предупреждение", "Выберите сеанс для редактирования",
                    Alert.AlertType.WARNING);
            return;
        }

        // Здесь будет диалог редактирования сеанса
        showAlert("Информация", "Редактирование сеанса #" + selected.getShowtimeId(),
                Alert.AlertType.INFORMATION);
    }

    @FXML
    private void cancelSelectedShowtime() {
        Showtime selected = showtimesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Предупреждение", "Выберите сеанс для отмены", Alert.AlertType.WARNING);
            return;
        }

        if (selected.getDateTime().isBefore(LocalDateTime.now())) {
            showAlert("Ошибка", "Нельзя отменить завершенный сеанс", Alert.AlertType.ERROR);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Отмена сеанса");
        confirmAlert.setHeaderText("Отменить сеанс?");
        confirmAlert.setContentText(
                "Фильм: " + selected.getFilmTitle() + "\n" +
                        "Время: " + selected.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + "\n" +
                        "Все проданные билеты будут возвращены автоматически."
        );

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Здесь будет логика отмены сеанса
                showAlert("Успех", "Сеанс отменен", Alert.AlertType.INFORMATION);
                loadShowtimes();
            }
        });
    }

    @FXML
    private void viewShowtimeDetails() {
        Showtime selected = showtimesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Предупреждение", "Выберите сеанс для просмотра", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Детали сеанса");

        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(20));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        Label filmLabel = new Label("Фильм: " + selected.getFilmTitle());
        Label timeLabel = new Label("Время: " + selected.getDateTime().format(formatter));
        Label hallLabel = new Label("Зал: " + selected.getHallName());
        Label priceLabel = new Label("Цена: " + String.format("%.2f руб.", selected.getFinalPrice()));
        Label typeLabel = new Label("Тип: " + selected.getRuleName());
        Label durationLabel = new Label("Длительность: " +
                (selected.getDateTime().plusMinutes(selected.getDateTime().getMinute()) // Это временно
                        .format(DateTimeFormatter.ofPattern("HH:mm"))));

        content.getChildren().addAll(filmLabel, timeLabel, hallLabel, priceLabel, typeLabel, durationLabel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void setNearestTimeSlot() {
        LocalTime now = LocalTime.now();
        LocalTime nearest = null;

        for (String timeStr : timeComboBox.getItems()) {
            String[] parts = timeStr.split(":");
            LocalTime slot = LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));

            if (slot.isAfter(now.plusMinutes(30))) { // Ближайший слот через 30 минут
                nearest = slot;
                break;
            }
        }

        if (nearest != null) {
            timeComboBox.setValue(String.format("%02d:%02d", nearest.getHour(), nearest.getMinute()));
        } else {
            timeComboBox.setValue("20:00"); // По умолчанию вечерний сеанс
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

    private int determineRuleId(LocalDateTime dateTime, String showtimeType) {
        int hour = dateTime.getHour();

        // Определяем по времени суток
        if (hour < 12) {
            return 2; // Утренний сеанс (коэффициент 0.8)
        } else if (hour >= 18) {
            return 1; // Вечерний сеанс (коэффициент 1.2)
        } else {
            return 0; // Дневной (коэффициент 1.0)
        }
    }

    private void clearForm() {
        filmComboBox.setValue(null);
        timeComboBox.setValue(null);
        hallComboBox.setValue(null);
        basePriceField.clear();
        showtimeTypeCombo.setValue("Стандартный");
        is3dCheckbox.setSelected(false);
        hasSubtitlesCheckbox.setSelected(false);
        isDubbedCheckbox.setSelected(false);
        conflictLabel.setText("");
        setNearestTimeSlot();
    }

    private void updateStatus() {
        scheduleStatusLabel.setText("Всего сеансов: " + showtimesList.size());
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}