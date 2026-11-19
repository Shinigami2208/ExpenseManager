package com.expense.service;

import com.expense.dto.ImportResult;
import com.expense.event.AppEvent;
import com.expense.event.EventBus;
import com.expense.event.EventType;
import com.expense.exception.AppException;
import com.expense.exception.BatchInsertException;
import com.expense.exception.NotFoundException;
import com.expense.model.Category;
import com.expense.model.Expense;
import com.expense.model.Wallet;
import com.expense.repository.CategoryRepository;
import com.expense.repository.ExpenseRepository;
import com.expense.repository.TransactionManager;
import com.expense.repository.WalletRepository;
import com.expense.util.CsvParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpenseServiceImpl implements ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseServiceImpl.class);

    private static final int IMPORT_BATCH_SIZE = 500;

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final TransactionManager tx;
    private final EventBus eventBus;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository,
                              CategoryRepository categoryRepository,
                              WalletRepository walletRepository,
                              TransactionManager tx,
                              EventBus eventBus) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.walletRepository = walletRepository;
        this.tx = tx;
        this.eventBus = eventBus;
    }

    @Override
    public List<Expense> getAll() {
        return expenseRepository.findAll();
    }

    @Override
    public Expense getById(int id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khoản chi id=" + id));
    }

    @Override
    public List<Expense> getByMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new AppException("Tháng phải từ 1 đến 12");
        }
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        return expenseRepository.findByDateRange(from, to);
    }

    @Override
    public Expense create(int categoryId, BigDecimal amount, String note, LocalDate date, Integer walletId) {
        validate(categoryId, amount, date);
        Wallet wallet = walletId == null ? null : getWallet(walletId);

        Expense expense = new Expense(categoryId, amount, note, date);
        expense.setWalletId(walletId);

        Expense saved = tx.inTransaction(conn -> {
            if (wallet != null) {
                debitOrFail(conn, wallet, amount);
            }
            return expenseRepository.save(conn, expense);
        });
        log.info("Đã tạo khoản chi id={} amount={} wallet={}", saved.getId(), amount, walletId);

        Expense full = getById(saved.getId());
        eventBus.publish(AppEvent.of(EventType.EXPENSE_CREATED, full));
        if (wallet != null) {
            eventBus.publish(AppEvent.of(EventType.WALLET_CHANGED, walletId));
        }
        return full;
    }

    @Override
    public void update(int id, int categoryId, BigDecimal amount, String note, LocalDate date, Integer walletId) {
        Expense existing = getById(id);
        validate(categoryId, amount, date);
        Wallet newWallet = walletId == null ? null : getWallet(walletId);

        Integer oldWalletId = existing.getWalletId();
        BigDecimal oldAmount = existing.getAmount();

        existing.setCategoryId(categoryId);
        existing.setAmount(amount);
        existing.setNote(note);
        existing.setExpenseDate(date);
        existing.setWalletId(walletId);

        tx.inTransaction(conn -> {
            if (oldWalletId != null) {
                walletRepository.credit(conn, oldWalletId, oldAmount);
            }
            if (newWallet != null) {
                debitOrFail(conn, newWallet, amount);
            }
            expenseRepository.update(conn, existing);
            return null;
        });
        log.info("Đã cập nhật khoản chi id={}", id);

        eventBus.publish(AppEvent.of(EventType.EXPENSE_UPDATED, getById(id)));
        if (oldWalletId != null || walletId != null) {
            eventBus.publish(AppEvent.of(EventType.WALLET_CHANGED, id));
        }
    }

    @Override
    public void delete(int id) {
        Expense existing = getById(id);

        tx.inTransaction(conn -> {
            expenseRepository.delete(conn, id);
            if (existing.getWalletId() != null) {
                walletRepository.credit(conn, existing.getWalletId(), existing.getAmount());
            }
            return null;
        });
        log.info("Đã xóa khoản chi id={}", id);

        eventBus.publish(AppEvent.of(EventType.EXPENSE_DELETED, Map.of("id", id)));
        if (existing.getWalletId() != null) {
            eventBus.publish(AppEvent.of(EventType.WALLET_CHANGED, existing.getWalletId()));
        }
    }

    @Override
    public Map<String, BigDecimal> getTotalByCategory() {
        return expenseRepository.sumByCategory();
    }

    @Override
    public BigDecimal getGrandTotal() {
        return expenseRepository.sumAll();
    }

    @Override
    public ImportResult importCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new AppException("File CSV trống");
        }
        Map<String, Integer> categoryIds = new HashMap<>();
        for (Category c : categoryRepository.findAll()) {
            categoryIds.put(c.getName().toLowerCase(Locale.ROOT), c.getId());
        }

        String[] lines = csv.replace("﻿", "").split("\\R");
        List<Expense> rows = new ArrayList<>();
        List<Integer> lineNumbers = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            try {
                rows.add(parseRow(CsvParser.parseLine(lines[i]), categoryIds));
                lineNumbers.add(i + 1);
            } catch (AppException | NumberFormatException | DateTimeParseException e) {
                errors.add("dòng " + (i + 1) + ": " + e.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            throw new AppException("CSV có " + errors.size() + " dòng sai, chưa nhập dòng nào. "
                    + String.join("; ", errors.subList(0, Math.min(5, errors.size()))));
        }
        if (rows.isEmpty()) {
            throw new AppException("CSV không có dòng dữ liệu nào");
        }

        long start = System.nanoTime();
        int imported;
        try {
            imported = tx.inTransaction(conn -> expenseRepository.insertBatch(conn, rows, IMPORT_BATCH_SIZE));
        } catch (BatchInsertException e) {
            int idx = Math.min(e.getFailedIndex(), lineNumbers.size() - 1);
            throw new AppException("Dòng " + lineNumbers.get(idx) + " bị database từ chối ("
                    + e.getCause().getMessage() + "). Đã rollback, không dòng nào được nhập.");
        }
        long millis = (System.nanoTime() - start) / 1_000_000;
        int batches = (imported + IMPORT_BATCH_SIZE - 1) / IMPORT_BATCH_SIZE;

        log.info("Đã nhập {} khoản chi từ CSV trong {} batch, {} ms", imported, batches, millis);
        eventBus.publish(AppEvent.of(EventType.EXPENSE_IMPORTED, Map.of("count", imported)));
        return new ImportResult(imported, batches, IMPORT_BATCH_SIZE, millis);
    }

    private Expense parseRow(List<String> cols, Map<String, Integer> categoryIds) {
        if (cols.size() < 3) {
            throw new AppException("cần ít nhất 3 cột date,category,amount");
        }
        LocalDate date = LocalDate.parse(cols.get(0));
        Integer categoryId = categoryIds.get(cols.get(1).toLowerCase(Locale.ROOT));
        if (categoryId == null) {
            throw new AppException("không có danh mục '" + cols.get(1) + "'");
        }
        BigDecimal amount = new BigDecimal(cols.get(2));
        if (amount.signum() <= 0) {
            throw new AppException("số tiền phải lớn hơn 0");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new AppException("ngày ở tương lai");
        }
        String note = cols.size() > 3 && !cols.get(3).isEmpty() ? cols.get(3) : null;
        return new Expense(categoryId, amount, note, date);
    }

    private Wallet getWallet(int id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new AppException("Ví id=" + id + " không tồn tại"));
    }

    private void debitOrFail(Connection conn, Wallet wallet, BigDecimal amount) {
        if (!walletRepository.debit(conn, wallet.id(), amount)) {
            throw new AppException("Ví '" + wallet.name() + "' không đủ số dư để chi " + amount.toPlainString() + " đ");
        }
    }

    private void validate(int categoryId, BigDecimal amount, LocalDate date) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException("Số tiền phải lớn hơn 0");
        }
        if (date == null) {
            throw new AppException("Ngày chi không được để trống");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new AppException("Ngày chi không được ở tương lai");
        }
        if (categoryRepository.findById(categoryId).isEmpty()) {
            throw new AppException("Danh mục id=" + categoryId + " không tồn tại");
        }
    }
}
