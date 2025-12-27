package com.cinema.cinemamanagementsystem.models;

public class Seat {
    private int seatId;
    private int row;
    private int number;
    private int categoryId;
    private int hallId;


    public void setHallId(int hallId) {
        this.hallId = hallId;
    }

    public Seat() {}

    public Seat(int seatId, int row, int number, int categoryId) {
        this.seatId = seatId;
        this.row = row;
        this.number = number;
        this.categoryId = categoryId;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public int getHallId() {
        return hallId;
    }
}
