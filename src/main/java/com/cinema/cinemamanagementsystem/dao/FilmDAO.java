package com.cinema.cinemamanagementsystem.dao;


import com.cinema.cinemamanagementsystem.models.Film;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FilmDAO {
    private static final Logger logger = LoggerFactory.getLogger(FilmDAO.class);

    // Получить все фильмы
    public List<Film> getAllFilms() {
        List<Film> films = new ArrayList<>();
        String query = "SELECT f.*, g.name as genre_name " +
                "FROM film f " +
                "LEFT JOIN film_genre g ON f.genre_id = g.genre_id " +
                "ORDER BY f.title";


        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Film film = new Film();
                film.setFilmId(rs.getInt("film_id"));
                film.setTitle(rs.getString("title"));
                film.setGenreId(rs.getInt("genre_id"));
                film.setGenreName(rs.getString("genre_name"));
                film.setDuration(rs.getInt("duration"));
                film.setDescription(rs.getString("description"));
                films.add(film);
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении фильмов: {}", e.getMessage());
        }
        return films;
    }

    // Добавить новый фильм
    public boolean addFilm(Film film) {
        String query = "INSERT INTO film (title, genre_id, duration, description) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, film.getTitle());
            pstmt.setInt(2, film.getGenreId());
            pstmt.setInt(3, film.getDuration());
            pstmt.setString(4, film.getDescription());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при добавлении фильма: {}", e.getMessage());
            return false;
        }
    }

    // Обновить фильм
    public boolean updateFilm(Film film) {
        String query = "UPDATE film SET title = ?, genre_id = ?, duration = ?, description = ? WHERE film_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, film.getTitle());
            pstmt.setInt(2, film.getGenreId());
            pstmt.setInt(3, film.getDuration());
            pstmt.setString(4, film.getDescription());
            pstmt.setInt(5, film.getFilmId());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении фильма: {}", e.getMessage());
            return false;
        }
    }

    // Удалить фильм
    public boolean deleteFilm(int filmId) {
        String query = "DELETE FROM film WHERE film_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, filmId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при удалении фильма: {}", e.getMessage());
            return false;
        }
    }

    // Поиск фильма по названию
    public List<Film> searchFilms(String searchText) {
        List<Film> films = new ArrayList<>();
        String query = " SELECT f.*, g.name as genre_name"+
           "FROM film f" +
            "LEFT JOIN film_genre g ON f.genre_id = g.genre_id"+
            "WHERE f.title LIKE ?"+
            "ORDER BY f.title ";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, "%" + searchText + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Film film = new Film();
                film.setFilmId(rs.getInt("film_id"));
                film.setTitle(rs.getString("title"));
                film.setGenreId(rs.getInt("genre_id"));
                film.setGenreName(rs.getString("genre_name"));
                film.setDuration(rs.getInt("duration"));
                film.setDescription(rs.getString("description"));
                films.add(film);
            }
        } catch (SQLException e) {
            logger.error("Ошибка при поиске фильмов: {}", e.getMessage());
        }
        return films;
    }
}