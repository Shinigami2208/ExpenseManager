package com.expense.controller;

import com.expense.dto.ExpenseRequest;
import com.expense.model.Expense;
import com.expense.service.ExpenseService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public class ExpenseController extends BaseController {

    public static final String PATH = "/api/expenses";

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @Override
    protected void handleRequest(HttpExchange ex) throws IOException {
        String id = pathId(ex, PATH);

        switch (method(ex)) {
            case "GET" -> {
                if (id != null) {
                    sendJson(ex, 200, expenseService.getById(Integer.parseInt(id)));
                    return;
                }
                Map<String, String> q = queryParams(ex);
                if (q.containsKey("year") && q.containsKey("month")) {
                    int year = Integer.parseInt(q.get("year"));
                    int month = Integer.parseInt(q.get("month"));
                    sendJson(ex, 200, expenseService.getByMonth(year, month));
                } else {
                    sendJson(ex, 200, expenseService.getAll());
                }
            }
            case "POST" -> {
                if ("import".equals(id)) {
                    sendJson(ex, 200, expenseService.importCsv(readText(ex)));
                    return;
                }
                ExpenseRequest req = readBody(ex, ExpenseRequest.class);
                Expense created = expenseService.create(req.getCategoryId(), req.getAmount(),
                        req.getNote(), req.getExpenseDate(), req.getWalletId());
                sendJson(ex, 201, expenseService.getById(created.getId()));
            }
            case "PUT" -> {
                if (id == null) {
                    sendError(ex, 400, "Thiếu id");
                    return;
                }
                ExpenseRequest req = readBody(ex, ExpenseRequest.class);
                int expenseId = Integer.parseInt(id);
                expenseService.update(expenseId, req.getCategoryId(), req.getAmount(),
                        req.getNote(), req.getExpenseDate(), req.getWalletId());
                sendJson(ex, 200, expenseService.getById(expenseId));
            }
            case "DELETE" -> {
                if (id == null) {
                    sendError(ex, 400, "Thiếu id");
                } else {
                    expenseService.delete(Integer.parseInt(id));
                    sendNoContent(ex);
                }
            }
            default -> methodNotAllowed(ex);
        }
    }
}
