package com.cinema.cinemamanagementsystem.models;

import java.time.LocalDate;

public class Film {
    private int filmId;
    private String title;
    private int genreId;
    private String genreName;
    private int duration; // в минутах
    private String description;
    private LocalDate releaseDate;

    // Конструкторы
    public Film() {}

    public Film(int filmId, String title, int genreId, int duration, String description) {
        this.filmId = filmId;
        this.title = title;
        this.genreId = genreId;
        this.duration = duration;
        this.description = description;
    }

    // Геттеры и сеттеры
    public int getFilmId() { return filmId; }
    public void setFilmId(int filmId) { this.filmId = filmId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getGenreId() { return genreId; }
    public void setGenreId(int genreId) { this.genreId = genreId; }

    public String getGenreName() { return genreName; }
    public void setGenreName(String genreName) { this.genreName = genreName; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }

    @Override
    public String toString() {
        return title + " (" + duration + " мин)";
    }
}