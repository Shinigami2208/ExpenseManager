package com.expense.controller;

import com.expense.event.AppEvent;
import com.expense.event.EventBus;
import com.expense.event.EventListener;
import com.expense.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventStreamController implements HttpHandler {

    public static final String PATH = "/api/events";

    private static final Logger log = LoggerFactory.getLogger(EventStreamController.class);

    private final EventBus eventBus;
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sse-heartbeat");
        t.setDaemon(true);
        return t;
    });

    public EventStreamController(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return;
        }
        if (!"GET".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(405, -1);
            ex.close();
            return;
        }

        ex.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.getResponseHeaders().set("Connection", "keep-alive");
        ex.sendResponseHeaders(200, 0);

        SseClient client = new SseClient(ex);
        client.sendComment("connected");
        eventBus.subscribe(client);
        heartbeat.scheduleAtFixedRate(client::ping, 15, 15, TimeUnit.SECONDS);
        log.info("SSE client kết nối từ {} (tổng {} listener)", ex.getRemoteAddress(), eventBus.listenerCount());
    }

    private final class SseClient implements EventListener {

        private final HttpExchange exchange;
        private final OutputStream out;
        private volatile boolean closed = false;

        SseClient(HttpExchange exchange) {
            this.exchange = exchange;
            this.out = exchange.getResponseBody();
        }

        @Override
        public void onEvent(AppEvent event) {
            write("event: " + event.type().code() + "\n"
                    + "data: " + JsonUtil.toJson(event.data()) + "\n\n");
        }

        void sendComment(String text) {
            write(": " + text + "\n\n");
        }

        void ping() {
            if (!closed) {
                sendComment("ping");
            }
        }

        private synchronized void write(String text) {
            if (closed) {
                return;
            }
            try {
                out.write(text.getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (IOException e) {
                close();
            }
        }

        private void close() {
            closed = true;
            eventBus.unsubscribe(this);
            exchange.close();
            log.info("SSE client ngắt kết nối (còn {} listener)", eventBus.listenerCount());
        }
    }
}
