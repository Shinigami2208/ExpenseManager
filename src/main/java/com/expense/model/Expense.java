package com.expense.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Expense {
    private int id;
    private int categoryId;
    private String categoryName;
    private BigDecimal amount;
    private String note;
    private LocalDate expenseDate;
    private Integer walletId;
    private String walletName;

    public Expense() {
    }

    public Expense(int categoryId, BigDecimal amount, String note, LocalDate expenseDate) {
        this.categoryId = categoryId;
        this.amount = amount;
        this.note = note;
        this.expenseDate = expenseDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public Integer getWalletId() {
        return walletId;
    }

    public void setWalletId(Integer walletId) {
        this.walletId = walletId;
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    @Override
    public String toString() {
        return String.format("#%-4d %-12s %-15s %,15.0f đ   %s",
                id, expenseDate, categoryName, amount, note == null ? "" : note);
    }
}
