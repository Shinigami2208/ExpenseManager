package com.expense.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transfer(int id,
                       int fromWalletId, String fromWalletName,
                       int toWalletId, String toWalletName,
                       BigDecimal amount, String note, LocalDateTime createdAt) {
}
