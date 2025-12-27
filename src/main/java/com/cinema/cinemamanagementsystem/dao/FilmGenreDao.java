package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.db.DataSourceProvider;
import com.cinema.cinemamanagementsystem.model.FilmGenre;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FilmGenreDao {
    private final DataSource dataSource = DataSourceProvider.getDataSource();

    public List<FilmGenre> findAll() throws SQLException {
        String sql = "SELECT genre_id, name FROM film_genre";
        List<FilmGenre> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new FilmGenre(rs.getInt("genre_id"), rs.getString("name")));
            }
        }
        return result;
    }

    public Optional<FilmGenre> findById(int id) throws SQLException {
        String sql = "SELECT genre_id, name FROM film_genre WHERE genre_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new FilmGenre(rs.getInt("genre_id"), rs.getString("name")));
                }
            }
        }
        return Optional.empty();
    }

    public int create(FilmGenre genre) throws SQLException {
        String sql = "INSERT INTO film_genre(name) VALUES (?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, genre.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void update(FilmGenre genre) throws SQLException {
        String sql = "UPDATE film_genre SET name = ? WHERE genre_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, genre.name());
            statement.setInt(2, genre.genreId());
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM film_genre WHERE genre_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }
}
