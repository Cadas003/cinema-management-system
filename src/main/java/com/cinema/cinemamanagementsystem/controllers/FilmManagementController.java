package com.cinema.cinemamanagementsystem.controllers;

import com.cinema.cinemamanagementsystem.dao.FilmDAO;
import com.cinema.cinemamanagementsystem.models.Film;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.text.Text;
import java.util.stream.Collectors;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.Node;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class FilmManagementController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(FilmManagementController.class);

    @FXML private TableView<Film> filmsTable;
    @FXML private TableColumn<Film, Integer> filmIdColumn;
    @FXML private TableColumn<Film, String> filmTitleColumn;
    @FXML private TableColumn<Film, String> filmGenreColumn;
    @FXML private TableColumn<Film, Integer> filmDurationColumn;
    @FXML private TableColumn<Film, String> filmDescriptionColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> genreFilter;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button viewButton;
    @FXML private Button scheduleButton;
    @FXML private Label statusLabel;

    private Stage stage;
    private final FilmDAO filmDAO = new FilmDAO();
    private ObservableList<Film> filmsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilters();
        setupEventHandlers();
        loadFilms();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void setupTable() {
        filmIdColumn.setCellValueFactory(new PropertyValueFactory<>("filmId"));
        filmTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        filmGenreColumn.setCellValueFactory(new PropertyValueFactory<>("genreName"));
        filmDurationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));
        filmDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        filmsTable.setItems(filmsList);

        // Автоматическое перенос строк в описании
        filmDescriptionColumn.setCellFactory(tc -> new TableCell<Film, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Text text = new Text(item);
                    text.setWrappingWidth(filmDescriptionColumn.getWidth() - 10);
                    setGraphic(text);
                }
            }
        });
    }

    private void setupFilters() {
        // Заполняем фильтр жанров
        genreFilter.getItems().add("Все жанры");
        genreFilter.getItems().addAll(
                "Фантастика", "Драма", "Боевик", "Комедия", "Ужасы",
                "Фэнтези", "Анимация", "Исторический", "Триллер", "Документальный"
        );
        genreFilter.setValue("Все жанры");

        // Поиск при вводе текста
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterFilms();
        });

        genreFilter.setOnAction(e -> filterFilms());
    }

    private void setupEventHandlers() {
        addButton.setOnAction(e -> showAddFilmDialog());
        editButton.setOnAction(e -> editSelectedFilm());
        deleteButton.setOnAction(e -> deleteSelectedFilm());
        viewButton.setOnAction(e -> viewFilmDetails());
        scheduleButton.setOnAction(e -> scheduleShowtimes());
    }

    private void loadFilms() {
        filmsList.setAll(filmDAO.getAllFilms());
        updateStatusLabel();
    }

    private void filterFilms() {
        String searchText = searchField.getText().trim().toLowerCase();
        String selectedGenre = genreFilter.getValue();

        if ((searchText.isEmpty() || searchText.isBlank()) &&
                "Все жанры".equals(selectedGenre)) {
            loadFilms();
            return;
        }

        List<Film> allFilms = filmDAO.getAllFilms();
        List<Film> filtered = allFilms.stream()
                .filter(film -> {
                    boolean matchesSearch = searchText.isEmpty() ||
                            film.getTitle().toLowerCase().contains(searchText) ||
                            (film.getDescription() != null &&
                                    film.getDescription().toLowerCase().contains(searchText));

                    boolean matchesGenre = "Все жанры".equals(selectedGenre) ||
                            (film.getGenreName() != null &&
                                    film.getGenreName().equals(selectedGenre));

                    return matchesSearch && matchesGenre;
                })
                .collect(Collectors.toList());


        filmsList.setAll(filtered);
        updateStatusLabel();
    }

    @FXML
    private void showAddFilmDialog() {
        Dialog<Film> dialog = new Dialog<>();
        dialog.setTitle("Добавление фильма");
        dialog.setHeaderText("Введите информацию о новом фильме");

        // Создаем форму
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField titleField = new TextField();
        titleField.setPromptText("Название фильма");

        ComboBox<String> genreCombo = new ComboBox<>();
        genreCombo.getItems().addAll(
                "Фантастика", "Драма", "Боевик", "Комедия", "Ужасы",
                "Фэнтези", "Анимация", "Исторический", "Триллер", "Документальный"
        );
        genreCombo.setPromptText("Выберите жанр");

        TextField durationField = new TextField();
        durationField.setPromptText("Длительность (минуты)");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Описание фильма");
        descriptionArea.setPrefRowCount(4);

        grid.add(new Label("Название:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Жанр:"), 0, 1);
        grid.add(genreCombo, 1, 1);
        grid.add(new Label("Длительность:"), 0, 2);
        grid.add(durationField, 1, 2);
        grid.add(new Label("Описание:"), 0, 3);
        grid.add(descriptionArea, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Кнопки
        ButtonType addButtonType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Валидация
        Node addButtonNode = dialog.getDialogPane().lookupButton(addButtonType);
        addButtonNode.setDisable(true);

        // Слушатели изменений
        titleField.textProperty().addListener((observable, oldValue, newValue) ->
                validateForm(addButtonNode, titleField, genreCombo, durationField));
        genreCombo.valueProperty().addListener((observable, oldValue, newValue) ->
                validateForm(addButtonNode, titleField, genreCombo, durationField));
        durationField.textProperty().addListener((observable, oldValue, newValue) ->
                validateForm(addButtonNode, titleField, genreCombo, durationField));

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    Film film = new Film();
                    film.setTitle(titleField.getText().trim());
                    film.setDuration(Integer.parseInt(durationField.getText().trim()));
                    film.setDescription(descriptionArea.getText().trim());

                    // Преобразуем название жанра в ID
                    int genreId = getGenreIdFromName(genreCombo.getValue());
                    film.setGenreId(genreId);

                    return film;
                } catch (NumberFormatException e) {
                    showAlert("Ошибка", "Некорректная длительность", Alert.AlertType.ERROR);
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

    @FXML
    private void editSelectedFilm() {
        Film selected = filmsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Предупреждение", "Выберите фильм для редактирования", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Film> dialog = new Dialog<>();
        dialog.setTitle("Редактирование фильма");
        dialog.setHeaderText("Редактирование: " + selected.getTitle());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField titleField = new TextField(selected.getTitle());
        ComboBox<String> genreCombo = new ComboBox<>();
        genreCombo.getItems().addAll(
                "Фантастика", "Драма", "Боевик", "Комедия", "Ужасы",
                "Фэнтези", "Анимация", "Исторический", "Триллер", "Документальный"
        );
        genreCombo.setValue(selected.getGenreName());

        TextField durationField = new TextField(String.valueOf(selected.getDuration()));
        TextArea descriptionArea = new TextArea(selected.getDescription());
        descriptionArea.setPrefRowCount(4);

        grid.add(new Label("Название:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Жанр:"), 0, 1);
        grid.add(genreCombo, 1, 1);
        grid.add(new Label("Длительность:"), 0, 2);
        grid.add(durationField, 1, 2);
        grid.add(new Label("Описание:"), 0, 3);
        grid.add(descriptionArea, 1, 3);

        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(false); // Все поля уже заполнены

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    selected.setTitle(titleField.getText().trim());
                    selected.setDuration(Integer.parseInt(durationField.getText().trim()));
                    selected.setDescription(descriptionArea.getText().trim());
                    selected.setGenreId(getGenreIdFromName(genreCombo.getValue()));
                    return selected;
                } catch (NumberFormatException e) {
                    showAlert("Ошибка", "Некорректная длительность", Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(film -> {
            if (filmDAO.updateFilm(film)) {
                showAlert("Успех", "Фильм успешно обновлен", Alert.AlertType.INFORMATION);
                loadFilms();
            } else {
                showAlert("Ошибка", "Не удалось обновить фильм", Alert.AlertType.ERROR);
            }
        });
    }

    @FXML
    private void deleteSelectedFilm() {
        Film selected = filmsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Предупреждение", "Выберите фильм для удаления", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение удаления");
        confirmAlert.setHeaderText("Удалить фильм '" + selected.getTitle() + "'?");
        confirmAlert.setContentText("Это действие нельзя отменить. Все связанные сеансы также будут удалены.");

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

    @FXML
    private void viewFilmDetails() {
        Film selected = filmsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Предупреждение", "Выберите фильм для просмотра", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Информация о фильме");
        dialog.setHeaderText(selected.getTitle());

        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(20));

        Label genreLabel = new Label("Жанр: " + selected.getGenreName());
        Label durationLabel = new Label("Длительность: " + selected.getDuration() + " минут");
        Label descriptionLabel = new Label("Описание:");
        TextArea descriptionArea = new TextArea(selected.getDescription());
        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(6);

        content.getChildren().addAll(genreLabel, durationLabel, descriptionLabel, descriptionArea);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    @FXML
    private void scheduleShowtimes() {
        Film selected = filmsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Предупреждение", "Выберите фильм для назначения сеансов", Alert.AlertType.WARNING);
            return;
        }

        // Здесь можно открыть окно управления сеансами с предварительно выбранным фильмом
        showAlert("Информация", "Функция назначения сеансов для фильма '" + selected.getTitle() + "'",
                Alert.AlertType.INFORMATION);
    }

    @FXML
    private void refreshFilms() {
        loadFilms();
        showAlert("Обновление", "Список фильмов обновлен", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void closeWindow() {
        if (stage != null) {
            stage.close();
        }
    }

    private int getGenreIdFromName(String genreName) {
        // В реальной системе здесь должно быть преобразование названия в ID
        // Временная реализация
        switch (genreName) {
            case "Фантастика": return 1;
            case "Драма": return 3;
            case "Боевик": return 4;
            case "Комедия": return 28; // Используем существующий ID
            case "Ужасы": return 5;
            case "Фэнтези": return 6;
            case "Анимация": return 7;
            case "Исторический": return 8;
            case "Триллер": return 9;
            case "Документальный": return 10;
            default: return 1;
        }
    }

    private void validateForm(Node button, TextField titleField,
                              ComboBox<String> genreCombo, TextField durationField) {
        boolean isValid = !titleField.getText().trim().isEmpty() &&
                genreCombo.getValue() != null &&
                !durationField.getText().trim().isEmpty() &&
                durationField.getText().matches("\\d+");

        button.setDisable(!isValid);
    }

    private void updateStatusLabel() {
        statusLabel.setText("Всего фильмов: " + filmsList.size());
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}