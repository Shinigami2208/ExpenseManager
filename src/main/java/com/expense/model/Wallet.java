package com.expense.model;

import java.math.BigDecimal;

public record Wallet(int id, String name, BigDecimal balance) {
}
