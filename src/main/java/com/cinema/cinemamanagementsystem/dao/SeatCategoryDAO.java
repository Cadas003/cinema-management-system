package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.models.SeatCategory;
import com.cinema.cinemamanagementsystem.config.DatabaseConfig;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class SeatCategoryDAO {

    public Map<Integer, SeatCategory> getAllCategories() {
        Map<Integer, SeatCategory> map = new HashMap<>();

        String query = "SELECT category_id, name, color FROM seat_category";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                SeatCategory cat = new SeatCategory();
                cat.setCategoryId(rs.getInt("category_id"));
                cat.setName(rs.getString("name"));
                cat.setColor(rs.getString("color")); // HEX из БД

                map.put(cat.getCategoryId(), cat);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }
}
