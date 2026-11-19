package com.expense.service;

import com.expense.model.Transfer;
import com.expense.model.Wallet;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    List<Wallet> getAll();

    Wallet create(String name, BigDecimal initialBalance);

    Transfer transfer(int fromWalletId, int toWalletId, BigDecimal amount, String note);

    List<Transfer> getRecentTransfers();
}
