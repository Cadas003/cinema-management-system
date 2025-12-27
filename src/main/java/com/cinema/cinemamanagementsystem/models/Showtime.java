package com.cinema.cinemamanagementsystem.models;

import java.time.LocalDateTime;

public class Showtime {
    private int showtimeId;
    private int filmId;
    private String filmTitle;
    private int hallId;
    private String hallName;
    private LocalDateTime dateTime;
    private double basePrice;
    private int ruleId;
    private String ruleName;
    private double coefficient;

    // Конструкторы
    public Showtime() {}

    // Геттеры и сеттеры
    public int getShowtimeId() { return showtimeId; }
    public void setShowtimeId(int showtimeId) { this.showtimeId = showtimeId; }

    public int getFilmId() { return filmId; }
    public void setFilmId(int filmId) { this.filmId = filmId; }

    public String getFilmTitle() { return filmTitle; }
    public void setFilmTitle(String filmTitle) { this.filmTitle = filmTitle; }

    public int getHallId() { return hallId; }
    public void setHallId(int hallId) { this.hallId = hallId; }

    public String getHallName() { return hallName; }
    public void setHallName(String hallName) { this.hallName = hallName; }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }

    public int getRuleId() { return ruleId; }
    public void setRuleId(int ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public double getCoefficient() { return coefficient; }
    public void setCoefficient(double coefficient) { this.coefficient = coefficient; }

    // Рассчитать итоговую цену
    public double getFinalPrice() {
        return basePrice * coefficient;
    }

    @Override
    public String toString() {
        return filmTitle + " - " + dateTime.toLocalDate() + " " +
                dateTime.toLocalTime() + " (" + hallName + ")";
    }
}
