package com.expense.service;

import com.expense.model.Category;

import java.util.List;

public interface CategoryService {
    List<Category> getAll();

    Category getById(int id);

    Category create(String name);

    void delete(int id);
}
