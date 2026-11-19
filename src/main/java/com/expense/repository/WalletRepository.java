package com.expense.repository;

import com.expense.model.Transfer;
import com.expense.model.Wallet;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface WalletRepository {
    List<Wallet> findAll();

    Optional<Wallet> findById(int id);

    Wallet save(String name, BigDecimal balance);

    boolean debit(Connection conn, int walletId, BigDecimal amount);

    void credit(Connection conn, int walletId, BigDecimal amount);

    int saveTransfer(Connection conn, int fromWalletId, int toWalletId, BigDecimal amount, String note);

    Optional<Transfer> findTransferById(int id);

    List<Transfer> findRecentTransfers(int limit);
}
