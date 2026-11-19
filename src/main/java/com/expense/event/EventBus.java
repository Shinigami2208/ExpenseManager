package com.expense.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {

    private static final Logger log = LoggerFactory.getLogger(EventBus.class);

    private final List<EventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(EventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(EventListener listener) {
        listeners.remove(listener);
    }

    public void publish(AppEvent event) {
        log.debug("Phát sự kiện {} tới {} listener", event.type().code(), listeners.size());
        for (EventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.warn("Listener lỗi khi nhận sự kiện {}: {}", event.type().code(), e.getMessage());
            }
        }
    }

    public int listenerCount() {
        return listeners.size();
    }
}
