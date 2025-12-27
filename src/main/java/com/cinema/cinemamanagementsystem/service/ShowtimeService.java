package com.cinema.cinemamanagementsystem.service;

import com.cinema.cinemamanagementsystem.dao.FilmDao;
import com.cinema.cinemamanagementsystem.dao.ShowtimeDao;
import com.cinema.cinemamanagementsystem.model.Film;
import com.cinema.cinemamanagementsystem.model.Showtime;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class ShowtimeService {
    private final ShowtimeDao showtimeDao = new ShowtimeDao();
    private final FilmDao filmDao = new FilmDao();

    public int createShowtime(Showtime showtime) throws SQLException {
        validateOverlap(showtime, null);
        return showtimeDao.create(showtime);
    }

    public void updateShowtime(Showtime showtime) throws SQLException {
        validateOverlap(showtime, showtime.showtimeId());
        showtimeDao.update(showtime);
    }

    public boolean hasOverlap(int hallId, LocalDateTime start, LocalDateTime end, Integer excludeShowtimeId)
            throws SQLException {
        return showtimeDao.hasOverlap(hallId, start, end, excludeShowtimeId);
    }

    private void validateOverlap(Showtime showtime, Integer excludeShowtimeId) throws SQLException {
        Film film = filmDao.findById(showtime.filmId())
                .orElseThrow(() -> new IllegalArgumentException("Фильм не найден"));
        LocalDateTime start = showtime.dateTime();
        LocalDateTime end = start.plusMinutes(film.duration());
        if (showtimeDao.hasOverlap(showtime.hallId(), start, end, excludeShowtimeId)) {
            throw new IllegalStateException("Сеанс пересекается по времени с другим сеансом в этом зале");
        }
    }
}
