package com.expense.dto;

import java.math.BigDecimal;

public class TransferRequest {
    private int fromWalletId;
    private int toWalletId;
    private BigDecimal amount;
    private String note;

    public int getFromWalletId() {
        return fromWalletId;
    }

    public int getToWalletId() {
        return toWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }
}
