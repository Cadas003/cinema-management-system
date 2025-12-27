package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.db.DataSourceProvider;
import com.cinema.cinemamanagementsystem.model.Film;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FilmDao {
    private final DataSource dataSource = DataSourceProvider.getDataSource();

    public List<Film> findAll() throws SQLException {
        String sql = "SELECT film_id, title, genre_id, duration, description FROM film";
        List<Film> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(mapFilm(rs));
            }
        }
        return result;
    }

    public Optional<Film> findById(int id) throws SQLException {
        String sql = "SELECT film_id, title, genre_id, duration, description FROM film WHERE film_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapFilm(rs));
                }
            }
        }
        return Optional.empty();
    }

    public int create(Film film) throws SQLException {
        String sql = "INSERT INTO film(title, genre_id, duration, description) VALUES (?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, film.title());
            statement.setInt(2, film.genreId());
            statement.setInt(3, film.duration());
            statement.setString(4, film.description());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void update(Film film) throws SQLException {
        String sql = "UPDATE film SET title = ?, genre_id = ?, duration = ?, description = ? WHERE film_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, film.title());
            statement.setInt(2, film.genreId());
            statement.setInt(3, film.duration());
            statement.setString(4, film.description());
            statement.setInt(5, film.filmId());
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM film WHERE film_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private Film mapFilm(ResultSet rs) throws SQLException {
        return new Film(
                rs.getInt("film_id"),
                rs.getString("title"),
                rs.getInt("genre_id"),
                rs.getInt("duration"),
                rs.getString("description")
        );
    }
}
