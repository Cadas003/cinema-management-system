module com.cinema.cinemamanagementsystem {
    requires java.sql;
    requires java.naming;

    requires com.zaxxer.hikari;
    requires org.slf4j;
    requires jbcrypt;

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    opens com.cinema.cinemamanagementsystem.ui to javafx.fxml;
    opens com.cinema.cinemamanagementsystem.ui.controllers to javafx.fxml;
    opens com.cinema.cinemamanagementsystem.model to javafx.base;

    exports com.cinema.cinemamanagementsystem;
    exports com.cinema.cinemamanagementsystem.ui.controllers;
    exports com.cinema.cinemamanagementsystem.service;
    exports com.cinema.cinemamanagementsystem.model;
}
