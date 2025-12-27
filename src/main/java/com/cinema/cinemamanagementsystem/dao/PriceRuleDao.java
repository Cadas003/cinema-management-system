package com.cinema.cinemamanagementsystem.dao;

import com.cinema.cinemamanagementsystem.db.DataSourceProvider;
import com.cinema.cinemamanagementsystem.model.PriceRule;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PriceRuleDao {
    private final DataSource dataSource = DataSourceProvider.getDataSource();

    public List<PriceRule> findAll() throws SQLException {
        String sql = "SELECT rule_id, name, coefficient FROM price_rule";
        List<PriceRule> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(mapRule(rs));
            }
        }
        return result;
    }

    public Optional<PriceRule> findById(int id) throws SQLException {
        String sql = "SELECT rule_id, name, coefficient FROM price_rule WHERE rule_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRule(rs));
                }
            }
        }
        return Optional.empty();
    }

    public int create(PriceRule rule) throws SQLException {
        String sql = "INSERT INTO price_rule(name, coefficient) VALUES (?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, rule.name());
            statement.setBigDecimal(2, rule.coefficient());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void update(PriceRule rule) throws SQLException {
        String sql = "UPDATE price_rule SET name = ?, coefficient = ? WHERE rule_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, rule.name());
            statement.setBigDecimal(2, rule.coefficient());
            statement.setInt(3, rule.ruleId());
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM price_rule WHERE rule_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private PriceRule mapRule(ResultSet rs) throws SQLException {
        return new PriceRule(
                rs.getInt("rule_id"),
                rs.getString("name"),
                rs.getBigDecimal("coefficient")
        );
    }
}
