package com.cinema.cinemamanagementsystem.controllers;

import com.cinema.cinemamanagementsystem.services.ReportService;
import com.cinema.cinemamanagementsystem.services.PaymentService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.print.PrinterJob;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.ResourceBundle;

public class ReportController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    // Компоненты UI
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> reportTypeComboBox;
    @FXML private Button generateButton;
    @FXML private Button exportButton;
    @FXML private Button printButton;

    // Таблицы для разных отчетов
    @FXML private TableView<Map<String, Object>> salesTable;
    @FXML private TableView<Map<String, Object>> filmsTable;
    @FXML private TableView<Map<String, Object>> hallsTable;
    @FXML private TableView<Map<String, Object>> customersTable;

    // Графики
    @FXML private BarChart<String, Number> salesChart;
    @FXML private PieChart filmsPieChart;
    @FXML private LineChart<String, Number> revenueChart;

    // Статистика
    @FXML private Label totalRevenueLabel;
    @FXML private Label totalTicketsLabel;
    @FXML private Label averageTicketLabel;
    @FXML private Label mostPopularFilmLabel;
    @FXML private Label bestHallLabel;

    private final ReportService reportService = new ReportService();
    private final PaymentService paymentService = new PaymentService();

    private ObservableList<Map<String, Object>> salesData = FXCollections.observableArrayList();
    private ObservableList<Map<String, Object>> filmsData = FXCollections.observableArrayList();
    private ObservableList<Map<String, Object>> hallsData = FXCollections.observableArrayList();
    private ObservableList<Map<String, Object>> customersData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupTables();
        setupCharts();

        // Устанавливаем даты по умолчанию (последние 30 дней)
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        endDatePicker.setValue(LocalDate.now());
    }

    private void setupUI() {
        // Заполняем комбобокс типами отчетов
        reportTypeComboBox.getItems().addAll(
                "Продажи за период",
                "Популярность фильмов",
                "Загрузка залов",
                "Активность клиентов",
                "Методы оплаты",
                "Динамика продаж"
        );
        reportTypeComboBox.setValue("Продажи за период");

        // Обработчики кнопок
        generateButton.setOnAction(e -> generateReport());
        exportButton.setOnAction(e -> exportReport());
        printButton.setOnAction(e -> printReport());

        // Обработчик изменения типа отчета
        reportTypeComboBox.setOnAction(e -> updateReportView());
    }

    private void setupTables() {
        // Настраиваем колонки для таблицы продаж
        TableColumn<Map<String, Object>, String> dateColumn = new TableColumn<>("Дата");
        dateColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().get("date").toString()
                ));

        TableColumn<Map<String, Object>, String> ticketsColumn = new TableColumn<>("Билетов");
        ticketsColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().get("tickets").toString()
                ));

        TableColumn<Map<String, Object>, String> revenueColumn = new TableColumn<>("Выручка");
        revenueColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("%.2f руб.", data.getValue().get("revenue"))
                ));

        salesTable.getColumns().addAll(dateColumn, ticketsColumn, revenueColumn);
        salesTable.setItems(salesData);
    }

    private void setupCharts() {
        // Настраиваем оси для графика продаж
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Дни");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Выручка (руб.)");

        salesChart.setTitle("Динамика продаж");
        salesChart.setLegendVisible(false);

        // Настраиваем круговую диаграмму
        filmsPieChart.setTitle("Распределение по фильмам");

        // Настраиваем график выручки
        revenueChart.setTitle("Выручка по дням");
    }

    @FXML
    private void generateReport() {
        try {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            String reportType = reportTypeComboBox.getValue();

            if (startDate == null || endDate == null) {
                showAlert("Ошибка", "Выберите период отчета", Alert.AlertType.ERROR);
                return;
            }

            if (startDate.isAfter(endDate)) {
                showAlert("Ошибка", "Начальная дата не может быть позже конечной", Alert.AlertType.ERROR);
                return;
            }

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            // Генерируем отчет в зависимости от типа
            switch (reportType) {
                case "Продажи за период":
                    generateSalesReport(startDateTime, endDateTime);
                    break;

                case "Популярность фильмов":
                    generateFilmsReport(startDateTime, endDateTime);
                    break;

                case "Загрузка залов":
                    generateHallsReport(startDateTime, endDateTime);
                    break;

                case "Активность клиентов":
                    generateCustomersReport(startDateTime, endDateTime);
                    break;

                case "Методы оплаты":
                    generatePaymentMethodsReport(startDateTime, endDateTime);
                    break;

                case "Динамика продаж":
                    generateSalesTrendsReport(startDateTime, endDateTime);
                    break;
            }

            logger.info("Отчет '{}' сгенерирован за период {} - {}",
                    reportType, startDate, endDate);

        } catch (Exception e) {
            logger.error("Ошибка при генерации отчета: {}", e.getMessage());
            showAlert("Ошибка", "Не удалось сгенерировать отчет: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    private void generateSalesReport(LocalDateTime startDate, LocalDateTime endDate) {
        // Показываем таблицу продаж
        salesTable.setVisible(true);
        filmsTable.setVisible(false);
        hallsTable.setVisible(false);
        customersTable.setVisible(false);
        salesChart.setVisible(false);
        filmsPieChart.setVisible(false);
        revenueChart.setVisible(false);

        // Получаем данные
        Map<String, Object> report = reportService.generateSalesReport(startDate, endDate);

        // Обновляем статистику
        totalRevenueLabel.setText(String.format("%.2f руб.",
                report.getOrDefault("totalRevenue", 0.0)));

        // Здесь должна быть логика заполнения таблицы
        // В реальной системе это будут данные из БД

        // Пример данных
        salesData.clear();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = Map.of(
                    "date", LocalDate.now().minusDays(i).toString(),
                    "tickets", (int)(Math.random() * 100) + 50,
                    "revenue", Math.random() * 50000 + 10000
            );
            salesData.add(row);
        }

        // Обновляем график
        updateSalesChart();
    }

    private void generateFilmsReport(LocalDateTime startDate, LocalDateTime endDate) {
        salesTable.setVisible(false);
        filmsTable.setVisible(true);
        hallsTable.setVisible(false);
        customersTable.setVisible(false);
        salesChart.setVisible(false);
        filmsPieChart.setVisible(true);
        revenueChart.setVisible(false);

        // Получаем данные
        Map<String, Object> report = reportService.generateFilmPopularityReport(startDate, endDate);

        // Обновляем круговую диаграмму
        filmsPieChart.getData().clear();
        filmsPieChart.getData().addAll(
                new PieChart.Data("Интерстеллар", 25),
                new PieChart.Data("Матрица", 20),
                new PieChart.Data("Титаник", 15),
                new PieChart.Data("Властелин колец", 12),
                new PieChart.Data("Остальные", 28)
        );

        mostPopularFilmLabel.setText("Интерстеллар (25% продаж)");
    }

    private void generateHallsReport(LocalDateTime startDate, LocalDateTime endDate) {
        salesTable.setVisible(false);
        filmsTable.setVisible(false);
        hallsTable.setVisible(true);
        customersTable.setVisible(false);
        salesChart.setVisible(false);
        filmsPieChart.setVisible(false);
        revenueChart.setVisible(false);

        Map<String, Object> report = reportService.generateHallOccupancyReport(startDate, endDate);

        // Пример данных для залов
        bestHallLabel.setText("Зал №1 (IMAX) - 85% заполняемости");
    }

    private void generateCustomersReport(LocalDateTime startDate, LocalDateTime endDate) {
        salesTable.setVisible(false);
        filmsTable.setVisible(false);
        hallsTable.setVisible(false);
        customersTable.setVisible(true);
        salesChart.setVisible(false);
        filmsPieChart.setVisible(false);
        revenueChart.setVisible(false);

        // Здесь будет логика загрузки данных о клиентах
    }

    private void generatePaymentMethodsReport(LocalDateTime startDate, LocalDateTime endDate) {
        String statistics = paymentService.getPaymentMethodsStatistics(startDate, endDate);

        TextArea textArea = new TextArea(statistics);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(6);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Статистика методов оплаты");
        alert.setHeaderText("Распределение по методам оплаты");
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    private void generateSalesTrendsReport(LocalDateTime startDate, LocalDateTime endDate) {
        salesTable.setVisible(false);
        filmsTable.setVisible(false);
        hallsTable.setVisible(false);
        customersTable.setVisible(false);
        salesChart.setVisible(true);
        filmsPieChart.setVisible(false);
        revenueChart.setVisible(true);

        updateSalesChart();
        updateRevenueChart();
    }

    private void updateSalesChart() {
        salesChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Выручка");

        // Пример данных
        for (int i = 0; i < 7; i++) {
            String day = LocalDate.now().minusDays(6 - i).getDayOfWeek().toString();
            double revenue = Math.random() * 50000 + 10000;
            series.getData().add(new XYChart.Data<>(day, revenue));
        }

        salesChart.getData().add(series);
    }

    private void updateRevenueChart() {
        revenueChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Ежедневная выручка");

        // Пример данных
        for (int i = 0; i < 30; i++) {
            String date = LocalDate.now().minusDays(29 - i).toString();
            double revenue = Math.random() * 60000 + 8000;
            series.getData().add(new XYChart.Data<>(date, revenue));
        }

        revenueChart.getData().add(series);
    }

    private void updateReportView() {
        String reportType = reportTypeComboBox.getValue();

        // Сбрасываем видимость всех компонентов
        salesTable.setVisible(false);
        filmsTable.setVisible(false);
        hallsTable.setVisible(false);
        customersTable.setVisible(false);
        salesChart.setVisible(false);
        filmsPieChart.setVisible(false);
        revenueChart.setVisible(false);

        // Включаем соответствующие компоненты
        switch (reportType) {
            case "Продажи за период":
                salesTable.setVisible(true);
                break;

            case "Популярность фильмов":
                filmsTable.setVisible(true);
                filmsPieChart.setVisible(true);
                break;

            case "Загрузка залов":
                hallsTable.setVisible(true);
                break;

            case "Активность клиентов":
                customersTable.setVisible(true);
                break;

            case "Динамика продаж":
                salesChart.setVisible(true);
                revenueChart.setVisible(true);
                break;
        }
    }

    @FXML
    private void exportReport() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Экспорт отчета");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Excel файлы", "*.xlsx"),
                    new FileChooser.ExtensionFilter("PDF файлы", "*.pdf"),
                    new FileChooser.ExtensionFilter("CSV файлы", "*.csv")
            );

            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                // Экспортируем отчет
                Map<String, Object> reportData = Map.of(
                        "period", startDatePicker.getValue() + " - " + endDatePicker.getValue(),
                        "type", reportTypeComboBox.getValue(),
                        "generated", LocalDateTime.now()
                );

                reportService.exportToExcel(reportData, file.getAbsolutePath());

                showAlert("Успех", "Отчет успешно экспортирован в: " + file.getAbsolutePath(),
                        Alert.AlertType.INFORMATION);

                logger.info("Отчет экспортирован в: {}", file.getAbsolutePath());
            }

        } catch (Exception e) {
            logger.error("Ошибка при экспорте отчета: {}", e.getMessage());
            showAlert("Ошибка", "Не удалось экспортировать отчет: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void printReport() {
        // Здесь будет логика печати отчета
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(null)) {
            // Печатаем текущее представление отчета
            boolean success = job.printPage(salesTable.getParent());
            if (success) {
                job.endJob();
                showAlert("Успех", "Отчет отправлен на печать", Alert.AlertType.INFORMATION);
            }
        }
    }

    @FXML
    private void showReportHelp() {
        Alert helpAlert = new Alert(Alert.AlertType.INFORMATION);
        helpAlert.setTitle("Справка по отчетам");
        helpAlert.setHeaderText("Типы отчетов:");
        helpAlert.setContentText(
                "1. Продажи за период - детализированный отчет по продажам\n" +
                        "2. Популярность фильмов - рейтинг фильмов по продажам\n" +
                        "3. Загрузка залов - анализ заполняемости залов\n" +
                        "4. Активность клиентов - статистика по клиентам\n" +
                        "5. Методы оплаты - распределение по способам оплаты\n" +
                        "6. Динамика продаж - графики изменения выручки\n\n" +
                        "Для экспорта отчета используйте кнопку 'Экспорт'."
        );
        helpAlert.showAndWait();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
