package com.expense.dto;

import java.math.BigDecimal;

public class WalletRequest {
    private String name;
    private BigDecimal balance;

    public String getName() {
        return name;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
