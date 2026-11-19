package com.expense.controller;

import com.expense.dto.CategoryRequest;
import com.expense.model.Category;
import com.expense.service.CategoryService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class CategoryController extends BaseController {

    public static final String PATH = "/api/categories";

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    protected void handleRequest(HttpExchange ex) throws IOException {
        String id = pathId(ex, PATH);

        switch (method(ex)) {
            case "GET" -> {
                if (id == null) {
                    sendJson(ex, 200, categoryService.getAll());
                } else {
                    sendJson(ex, 200, categoryService.getById(Integer.parseInt(id)));
                }
            }
            case "POST" -> {
                CategoryRequest req = readBody(ex, CategoryRequest.class);
                Category created = categoryService.create(req.getName());
                sendJson(ex, 201, created);
            }
            case "DELETE" -> {
                if (id == null) {
                    sendError(ex, 400, "Thiếu id");
                } else {
                    categoryService.delete(Integer.parseInt(id));
                    sendNoContent(ex);
                }
            }
            default -> methodNotAllowed(ex);
        }
    }
}
