package com.expense.controller;

import com.expense.dto.TransferRequest;
import com.expense.service.WalletService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class TransferController extends BaseController {

    public static final String PATH = "/api/transfers";

    private final WalletService walletService;

    public TransferController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    protected void handleRequest(HttpExchange ex) throws IOException {
        switch (method(ex)) {
            case "GET" -> sendJson(ex, 200, walletService.getRecentTransfers());
            case "POST" -> {
                TransferRequest req = readBody(ex, TransferRequest.class);
                sendJson(ex, 201, walletService.transfer(
                        req.getFromWalletId(), req.getToWalletId(), req.getAmount(), req.getNote()));
            }
            default -> methodNotAllowed(ex);
        }
    }
}
