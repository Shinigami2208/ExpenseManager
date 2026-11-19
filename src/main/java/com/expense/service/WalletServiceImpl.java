package com.expense.service;

import com.expense.event.AppEvent;
import com.expense.event.EventBus;
import com.expense.event.EventType;
import com.expense.exception.AppException;
import com.expense.exception.NotFoundException;
import com.expense.model.Transfer;
import com.expense.model.Wallet;
import com.expense.repository.TransactionManager;
import com.expense.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

public class WalletServiceImpl implements WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);

    private final WalletRepository walletRepository;
    private final TransactionManager tx;
    private final EventBus eventBus;

    public WalletServiceImpl(WalletRepository walletRepository, TransactionManager tx, EventBus eventBus) {
        this.walletRepository = walletRepository;
        this.tx = tx;
        this.eventBus = eventBus;
    }

    @Override
    public List<Wallet> getAll() {
        return walletRepository.findAll();
    }

    @Override
    public Wallet create(String name, BigDecimal initialBalance) {
        if (name == null || name.isBlank()) {
            throw new AppException("Tên ví không được để trống");
        }
        BigDecimal balance = initialBalance == null ? BigDecimal.ZERO : initialBalance;
        if (balance.signum() < 0) {
            throw new AppException("Số dư ban đầu không được âm");
        }
        Wallet created = walletRepository.save(name.trim(), balance);
        eventBus.publish(AppEvent.of(EventType.WALLET_CHANGED, created));
        return created;
    }

    @Override
    public Transfer transfer(int fromWalletId, int toWalletId, BigDecimal amount, String note) {
        if (fromWalletId == toWalletId) {
            throw new AppException("Ví nguồn và ví đích phải khác nhau");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new AppException("Số tiền chuyển phải lớn hơn 0");
        }
        Wallet from = getWallet(fromWalletId);
        Wallet to = getWallet(toWalletId);

        int transferId = tx.inTransaction(conn -> {
            if (fromWalletId < toWalletId) {
                debitOrFail(conn, from, amount);
                walletRepository.credit(conn, toWalletId, amount);
            } else {
                walletRepository.credit(conn, toWalletId, amount);
                debitOrFail(conn, from, amount);
            }
            return walletRepository.saveTransfer(conn, fromWalletId, toWalletId, amount, note);
        });

        log.info("Chuyển {} từ ví {} sang ví {} (transfer id={})", amount, from.name(), to.name(), transferId);
        eventBus.publish(AppEvent.of(EventType.WALLET_CHANGED, transferId));

        return walletRepository.findTransferById(transferId).orElseThrow();
    }

    @Override
    public List<Transfer> getRecentTransfers() {
        return walletRepository.findRecentTransfers(20);
    }

    private Wallet getWallet(int id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ví id=" + id));
    }

    private void debitOrFail(Connection conn, Wallet wallet, BigDecimal amount) {
        if (!walletRepository.debit(conn, wallet.id(), amount)) {
            throw new AppException("Ví '" + wallet.name() + "' không đủ số dư để chi " + amount.toPlainString() + " đ");
        }
    }
}
