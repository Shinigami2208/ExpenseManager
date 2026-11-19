package com.expense.controller;

import com.expense.dto.WalletRequest;
import com.expense.service.WalletService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class WalletController extends BaseController {

    public static final String PATH = "/api/wallets";

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    protected void handleRequest(HttpExchange ex) throws IOException {
        switch (method(ex)) {
            case "GET" -> sendJson(ex, 200, walletService.getAll());
            case "POST" -> {
                WalletRequest req = readBody(ex, WalletRequest.class);
                sendJson(ex, 201, walletService.create(req.getName(), req.getBalance()));
            }
            default -> methodNotAllowed(ex);
        }
    }
}
