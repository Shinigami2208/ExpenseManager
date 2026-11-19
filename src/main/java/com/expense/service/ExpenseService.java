package com.expense.service;

import com.expense.dto.ImportResult;
import com.expense.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ExpenseService {
    List<Expense> getAll();

    Expense getById(int id);

    List<Expense> getByMonth(int year, int month);

    Expense create(int categoryId, BigDecimal amount, String note, LocalDate date, Integer walletId);

    void update(int id, int categoryId, BigDecimal amount, String note, LocalDate date, Integer walletId);

    void delete(int id);

    Map<String, BigDecimal> getTotalByCategory();

    BigDecimal getGrandTotal();

    ImportResult importCsv(String csv);
}
