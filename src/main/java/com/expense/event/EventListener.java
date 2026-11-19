package com.expense.event;

@FunctionalInterface
public interface EventListener {
    void onEvent(AppEvent event);
}
