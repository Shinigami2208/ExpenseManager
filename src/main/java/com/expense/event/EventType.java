package com.expense.event;

public enum EventType {
    EXPENSE_CREATED("expense.created"),
    EXPENSE_UPDATED("expense.updated"),
    EXPENSE_DELETED("expense.deleted"),
    EXPENSE_IMPORTED("expense.imported"),
    CATEGORY_CREATED("category.created"),
    CATEGORY_DELETED("category.deleted"),
    WALLET_CHANGED("wallet.changed");

    private final String code;

    EventType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
