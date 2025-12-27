package com.cinema.cinemamanagementsystem.service;

import com.cinema.cinemamanagementsystem.dao.CustomerDao;
import com.cinema.cinemamanagementsystem.model.Customer;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CustomerService {
    private final CustomerDao customerDao = new CustomerDao();

    public List<Customer> search(String query) throws SQLException {
        return customerDao.search(query);
    }

    public Optional<Customer> findById(int id) throws SQLException {
        return customerDao.findById(id);
    }

    public int create(Customer customer) throws SQLException {
        return customerDao.create(customer);
    }

    public void update(Customer customer) throws SQLException {
        customerDao.update(customer);
    }
}
