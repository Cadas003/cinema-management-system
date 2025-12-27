module com.cinema.cinemamanagementsystem {

    // Стандартная библиотека для работы с БД
    requires java.sql;

    // HikariCP — пул соединений
    requires com.zaxxer.hikari;

    // SLF4J — логгирование
    requires org.slf4j;

    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    // Открываем пакеты для FXML и биндингов
    opens com.cinema.cinemamanagementsystem to javafx.fxml;
    opens com.cinema.cinemamanagementsystem.controllers to javafx.fxml;
    opens com.cinema.cinemamanagementsystem.dao to javafx.fxml;
    opens com.cinema.cinemamanagementsystem.models to javafx.base;

    // Экспортируем пакеты, если они используются вне модуля
    exports com.cinema.cinemamanagementsystem;
    exports com.cinema.cinemamanagementsystem.controllers;
    exports com.cinema.cinemamanagementsystem.dao;
    exports com.cinema.cinemamanagementsystem.models;
}
