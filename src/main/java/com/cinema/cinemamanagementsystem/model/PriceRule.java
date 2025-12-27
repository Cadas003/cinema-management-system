package com.cinema.cinemamanagementsystem.model;

import java.math.BigDecimal;

public record PriceRule(int ruleId, String name, BigDecimal coefficient) {
}
