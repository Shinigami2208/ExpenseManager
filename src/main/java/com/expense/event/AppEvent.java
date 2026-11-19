package com.expense.event;

public record AppEvent(EventType type, Object data) {

    public static AppEvent of(EventType type, Object data) {
        return new AppEvent(type, data);
    }
}
