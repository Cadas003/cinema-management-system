package com.cinema.cinemamanagementsystem.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayDeque;
import java.util.Deque;

public final class OperationHistory {
    private static final int MAX_RECORDS = 10;
    private static final Deque<OperationRecord> records = new ArrayDeque<>();
    private static final ObservableList<OperationRecord> observableRecords = FXCollections.observableArrayList();

    private OperationHistory() {
    }

    public static void addRecord(OperationRecord record) {
        if (record == null) {
            return;
        }
        records.addFirst(record);
        while (records.size() > MAX_RECORDS) {
            records.removeLast();
        }
        refreshObservable();
    }

    public static ObservableList<OperationRecord> getRecords() {
        return observableRecords;
    }

    private static void refreshObservable() {
        observableRecords.setAll(records);
    }
}
