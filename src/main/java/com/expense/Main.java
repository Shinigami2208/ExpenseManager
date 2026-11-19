package com.expense;

import com.expense.controller.CategoryController;
import com.expense.controller.EventStreamController;
import com.expense.controller.ExpenseController;
import com.expense.controller.ReportController;
import com.expense.controller.TransferController;
import com.expense.controller.WalletController;
import com.expense.config.AppConfig;
import com.expense.event.EventBus;
import com.expense.repository.CategoryRepository;
import com.expense.repository.CategoryRepositoryImpl;
import com.expense.repository.ExpenseRepository;
import com.expense.repository.ExpenseRepositoryImpl;
import com.expense.repository.TransactionManager;
import com.expense.repository.WalletRepository;
import com.expense.repository.WalletRepositoryImpl;
import com.expense.service.CategoryService;
import com.expense.service.CategoryServiceImpl;
import com.expense.service.ExpenseService;
import com.expense.service.ExpenseServiceImpl;
import com.expense.service.WalletService;
import com.expense.service.WalletServiceImpl;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        CategoryRepository categoryRepository = new CategoryRepositoryImpl();
        ExpenseRepository expenseRepository = new ExpenseRepositoryImpl();
        WalletRepository walletRepository = new WalletRepositoryImpl();
        TransactionManager transactionManager = new TransactionManager();

        EventBus eventBus = new EventBus();

        CategoryService categoryService = new CategoryServiceImpl(categoryRepository, eventBus);
        ExpenseService expenseService = new ExpenseServiceImpl(
                expenseRepository, categoryRepository, walletRepository, transactionManager, eventBus);
        WalletService walletService = new WalletServiceImpl(walletRepository, transactionManager, eventBus);

        int port = AppConfig.serverPort();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(CategoryController.PATH, new CategoryController(categoryService));
        server.createContext(ExpenseController.PATH, new ExpenseController(expenseService));
        server.createContext(ReportController.PATH, new ReportController(expenseService));
        server.createContext(WalletController.PATH, new WalletController(walletService));
        server.createContext(TransferController.PATH, new TransferController(walletService));
        server.createContext(EventStreamController.PATH, new EventStreamController(eventBus));

        server.setExecutor(Executors.newFixedThreadPool(10));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Đang dừng server...");
            server.stop(0);
        }));

        server.start();
        log.info("API đang chạy tại http://localhost:{}", port);
        log.info("  GET http://localhost:{}/api/categories", port);
        log.info("  GET http://localhost:{}/api/expenses", port);
        log.info("  GET http://localhost:{}/api/reports/by-category", port);
        log.info("  GET http://localhost:{}/api/wallets", port);
        log.info("  GET http://localhost:{}/api/transfers", port);
        log.info("  GET http://localhost:{}/api/events   (SSE realtime)", port);
        log.info("Nhấn Ctrl+C để dừng.");
    }
}
