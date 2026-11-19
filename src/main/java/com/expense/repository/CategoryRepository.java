package com.expense.repository;

import com.expense.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    List<Category> findAll();

    Optional<Category> findById(int id);

    Category save(Category category);

    boolean delete(int id);
}
