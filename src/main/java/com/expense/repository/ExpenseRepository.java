package com.expense.repository;

import com.expense.model.Expense;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ExpenseRepository {
    List<Expense> findAll();

    Optional<Expense> findById(int id);

    List<Expense> findByDateRange(LocalDate from, LocalDate to);

    Expense save(Expense expense);

    boolean update(Expense expense);

    boolean delete(int id);

    Expense save(Connection conn, Expense expense);

    boolean update(Connection conn, Expense expense);

    boolean delete(Connection conn, int id);

    int insertBatch(Connection conn, List<Expense> expenses, int batchSize);

    Map<String, BigDecimal> sumByCategory();

    BigDecimal sumAll();
}
