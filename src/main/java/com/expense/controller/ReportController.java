package com.expense.controller;

import com.expense.service.ExpenseService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public class ReportController extends BaseController {

    public static final String PATH = "/api/reports";

    private final ExpenseService expenseService;

    public ReportController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @Override
    protected void handleRequest(HttpExchange ex) throws IOException {
        if (!"GET".equals(method(ex))) {
            methodNotAllowed(ex);
            return;
        }
        String report = pathId(ex, PATH);

        if ("by-category".equals(report)) {
            sendJson(ex, 200, expenseService.getTotalByCategory());
        } else if ("total".equals(report)) {
            sendJson(ex, 200, Map.of("total", expenseService.getGrandTotal()));
        } else {
            sendError(ex, 404, "Không có báo cáo: " + report);
        }
    }
}
