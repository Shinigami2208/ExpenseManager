package com.expense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseRequest {
    private int categoryId;
    private BigDecimal amount;
    private String note;
    private LocalDate expenseDate;
    private Integer walletId;

    public int getCategoryId() {
        return categoryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public Integer getWalletId() {
        return walletId;
    }
}
