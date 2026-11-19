package com.expense.service;

import com.expense.event.AppEvent;
import com.expense.event.EventBus;
import com.expense.event.EventType;
import com.expense.exception.AppException;
import com.expense.exception.NotFoundException;
import com.expense.model.Category;
import com.expense.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;
    private final EventBus eventBus;

    public CategoryServiceImpl(CategoryRepository categoryRepository, EventBus eventBus) {
        this.categoryRepository = categoryRepository;
        this.eventBus = eventBus;
    }

    @Override
    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Category getById(int id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục id=" + id));
    }

    @Override
    public Category create(String name) {
        if (name == null || name.isBlank()) {
            throw new AppException("Tên danh mục không được để trống");
        }
        Category saved = categoryRepository.save(new Category(0, name.trim()));
        log.info("Đã tạo danh mục id={} name='{}'", saved.getId(), saved.getName());
        eventBus.publish(AppEvent.of(EventType.CATEGORY_CREATED, saved));
        return saved;
    }

    @Override
    public void delete(int id) {
        getById(id);
        categoryRepository.delete(id);
        log.info("Đã xóa danh mục id={}", id);
        eventBus.publish(AppEvent.of(EventType.CATEGORY_DELETED, Map.of("id", id)));
    }
}
