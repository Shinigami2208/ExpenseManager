package com.expense.controller;

import com.expense.dto.ErrorResponse;
import com.expense.exception.AppException;
import com.expense.exception.ConflictException;
import com.expense.exception.DataAccessException;
import com.expense.exception.NotFoundException;
import com.expense.util.JsonUtil;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseController implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(BaseController.class);

    @Override
    public final void handle(HttpExchange ex) throws IOException {
        long start = System.currentTimeMillis();
        addCorsHeaders(ex);

        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        try {
            handleRequest(ex);
        } catch (NotFoundException e) {
            sendError(ex, 404, e.getMessage());
        } catch (ConflictException e) {
            sendError(ex, 409, e.getMessage());
        } catch (DataAccessException e) {
            log.error("Lỗi truy cập dữ liệu: {}", e.getMessage(), e);
            sendError(ex, 500, "Lỗi hệ thống, vui lòng thử lại sau");
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Giá trị phải là số: " + e.getMessage());
        } catch (DateTimeParseException e) {
            sendError(ex, 400, "Ngày phải có dạng yyyy-MM-dd");
        } catch (AppException | JsonSyntaxException e) {
            sendError(ex, 400, e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi không xác định", e);
            sendError(ex, 500, "Lỗi hệ thống, vui lòng thử lại sau");
        } finally {
            log.info("{} {} -> {} ({} ms)", ex.getRequestMethod(), ex.getRequestURI(),
                    ex.getResponseCode(), System.currentTimeMillis() - start);
            ex.close();
        }
    }

    protected abstract void handleRequest(HttpExchange ex) throws IOException;

    protected String method(HttpExchange ex) {
        return ex.getRequestMethod();
    }

    protected String pathId(HttpExchange ex, String basePath) {
        String path = ex.getRequestURI().getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.length() <= basePath.length()) {
            return null;
        }
        return path.substring(basePath.length() + 1);
    }

    protected Map<String, String> queryParams(HttpExchange ex) {
        Map<String, String> map = new HashMap<>();
        String query = ex.getRequestURI().getQuery();
        if (query == null) {
            return map;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            map.put(kv[0], kv.length > 1 ? kv[1] : "");
        }
        return map;
    }

    protected String readText(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    protected <T> T readBody(HttpExchange ex, Class<T> type) throws IOException {
        T body = JsonUtil.fromJson(readText(ex), type);
        if (body == null) {
            throw new AppException("Body JSON không được để trống");
        }
        return body;
    }

    protected void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = JsonUtil.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    protected void sendNoContent(HttpExchange ex) throws IOException {
        ex.sendResponseHeaders(204, -1);
    }

    protected void sendError(HttpExchange ex, int status, String message) throws IOException {
        sendJson(ex, status, new ErrorResponse(status, message));
    }

    protected void methodNotAllowed(HttpExchange ex) throws IOException {
        sendError(ex, 405, "Method " + ex.getRequestMethod() + " không được hỗ trợ");
    }

    private void addCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }
}
