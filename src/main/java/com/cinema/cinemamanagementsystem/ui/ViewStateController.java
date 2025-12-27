package com.cinema.cinemamanagementsystem.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ViewStateController {
    private final StackPane root;
    private final Node content;
    private final VBox stateBox;
    private final Label iconLabel;
    private final Label titleLabel;
    private final Label messageLabel;
    private final Button actionButton;
    private final ProgressIndicator progressIndicator;

    public ViewStateController(StackPane root, Node content) {
        this.root = root;
        this.content = content;
        this.stateBox = new VBox(8);
        this.iconLabel = new Label();
        this.titleLabel = new Label();
        this.messageLabel = new Label();
        this.actionButton = new Button();
        this.progressIndicator = new ProgressIndicator();

        stateBox.setAlignment(Pos.CENTER);
        stateBox.getStyleClass().add("state-pane");
        iconLabel.getStyleClass().add("state-icon");
        titleLabel.getStyleClass().add("state-title");
        messageLabel.getStyleClass().add("state-message");
        actionButton.getStyleClass().addAll("button", "button-primary", "state-action");
        progressIndicator.getStyleClass().add("loading-indicator");

        stateBox.getChildren().addAll(iconLabel, titleLabel, messageLabel, actionButton);
        root.getChildren().add(stateBox);

        hideState();
    }

    public void showLoading(String message) {
        iconLabel.setText("⏳");
        titleLabel.setText("Загрузка");
        messageLabel.setText(message == null ? "Подождите, данные обновляются" : message);
        actionButton.setVisible(false);
        actionButton.setManaged(false);

        if (!stateBox.getChildren().contains(progressIndicator)) {
            stateBox.getChildren().add(1, progressIndicator);
        }

        showState();
    }

    public void showEmpty(String title, String message, String actionText, Runnable action) {
        iconLabel.setText("🗂️");
        titleLabel.setText(title == null ? "Пока пусто" : title);
        messageLabel.setText(message == null ? "Здесь пока нет данных" : message);

        if (stateBox.getChildren().contains(progressIndicator)) {
            stateBox.getChildren().remove(progressIndicator);
        }

        configureAction(actionText, action);
        showState();
    }

    public void showError(String message, Runnable retryAction) {
        iconLabel.setText("⚠️");
        titleLabel.setText("Не удалось загрузить данные");
        messageLabel.setText(message == null ? "Попробуйте еще раз" : message);

        if (stateBox.getChildren().contains(progressIndicator)) {
            stateBox.getChildren().remove(progressIndicator);
        }

        configureAction("Повторить", retryAction);
        showState();
    }

    public void showContent() {
        hideState();
    }

    private void configureAction(String text, Runnable action) {
        if (action == null) {
            actionButton.setVisible(false);
            actionButton.setManaged(false);
            actionButton.setOnAction(null);
            return;
        }

        actionButton.setText(text == null ? "Действие" : text);
        actionButton.setOnAction(event -> action.run());
        actionButton.setVisible(true);
        actionButton.setManaged(true);
    }

    private void showState() {
        content.setVisible(false);
        content.setManaged(false);
        stateBox.setVisible(true);
        stateBox.setManaged(true);
    }

    private void hideState() {
        content.setVisible(true);
        content.setManaged(true);
        stateBox.setVisible(false);
        stateBox.setManaged(false);
        actionButton.setVisible(false);
        actionButton.setManaged(false);
    }
}
