package com.cinema.cinemamanagementsystem.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Optional;

public final class UiDialogs {
    private UiDialogs() {
    }

    public static boolean confirmDialog(Stage owner, String title, String message, String confirmText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);

        ButtonType confirm = new ButtonType(confirmText == null ? "Подтвердить" : confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirm, cancel);

        Optional<ButtonType> response = alert.showAndWait();
        return response.isPresent() && response.get() == confirm;
    }

    public static void showErrorDialog(Stage owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);
        alert.showAndWait();
    }

    public static void showInfoDialog(Stage owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);
        alert.showAndWait();
    }

    public static void successToast(Stage owner, String message) {
        showToast(owner, message, "toast-success");
    }

    public static void errorToast(Stage owner, String message) {
        showToast(owner, message, "toast-error");
    }

    private static void showToast(Stage owner, String message, String styleClass) {
        if (owner == null || owner.getScene() == null) {
            return;
        }

        Popup popup = new Popup();
        Label label = new Label(message);
        label.getStyleClass().addAll("toast", styleClass);

        HBox container = new HBox(label);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(8));
        popup.getContent().add(container);

        Scene scene = owner.getScene();
        double x = owner.getX() + scene.getWidth() - 320;
        double y = owner.getY() + scene.getHeight() - 120;
        popup.show(owner, x, y);

        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(event -> popup.hide());
        delay.play();
    }

    public static void showSaleSuccess(Stage owner, SaleSummary summary, Runnable newSaleAction, Runnable openHistoryAction) {
        if (owner == null) {
            return;
        }

        Stage dialogStage = new Stage();
        dialogStage.initOwner(owner);
        dialogStage.setTitle("Продажа завершена");

        Label title = new Label("Оплата прошла успешно");
        title.getStyleClass().add("success-title");

        Label amount = new Label("Сумма: " + summary.totalAmount() + " руб.");
        Label change = new Label("Сдача: " + summary.changeAmount() + " руб.");
        Label operation = new Label("Операция: " + summary.operationId());
        Label tickets = new Label("Билеты: " + summary.ticketIds());
        amount.getStyleClass().add("success-meta");
        change.getStyleClass().add("success-meta");
        operation.getStyleClass().add("success-meta");
        tickets.getStyleClass().add("success-meta");

        javafx.scene.control.Button printButton = new javafx.scene.control.Button("Печать");
        javafx.scene.control.Button newSaleButton = new javafx.scene.control.Button("Новая продажа");
        javafx.scene.control.Button historyButton = new javafx.scene.control.Button("Открыть историю");

        printButton.getStyleClass().addAll("button", "button-secondary");
        newSaleButton.getStyleClass().addAll("button", "button-success");
        historyButton.getStyleClass().addAll("button", "button-primary");

        printButton.setOnAction(event -> {
            successToast(owner, "Отправлено на печать");
            dialogStage.close();
        });
        newSaleButton.setOnAction(event -> {
            if (newSaleAction != null) {
                newSaleAction.run();
            }
            dialogStage.close();
        });
        historyButton.setOnAction(event -> {
            if (openHistoryAction != null) {
                openHistoryAction.run();
            }
            dialogStage.close();
        });

        HBox actions = new HBox(10, printButton, historyButton, newSaleButton);
        actions.setAlignment(Pos.CENTER);

        VBox root = new VBox(12, title, amount, change, operation, tickets, actions);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("success-dialog");

        Scene scene = new Scene(root);
        if (owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    public static final class SaleSummary {
        private final String totalAmount;
        private final String changeAmount;
        private final String operationId;
        private final String ticketIds;

        public SaleSummary(String totalAmount, String changeAmount, String operationId, String ticketIds) {
            this.totalAmount = totalAmount;
            this.changeAmount = changeAmount;
            this.operationId = operationId;
            this.ticketIds = ticketIds;
        }

        public String totalAmount() {
            return totalAmount;
        }

        public String changeAmount() {
            return changeAmount;
        }

        public String operationId() {
            return operationId;
        }

        public String ticketIds() {
            return ticketIds;
        }
    }
}
