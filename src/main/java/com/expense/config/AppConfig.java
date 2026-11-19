package com.expense.config;

import com.expense.exception.AppException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

    private static final Properties PROPS = loadProperties();

    private AppConfig() {
    }

    public static String dbUrl() {
        return get("DB_URL", "db.url");
    }

    public static String dbUsername() {
        return get("DB_USERNAME", "db.username");
    }

    public static String dbPassword() {
        return get("DB_PASSWORD", "db.password");
    }

    public static int serverPort() {
        String value = System.getenv("SERVER_PORT");
        return value == null ? 8080 : Integer.parseInt(value);
    }

    private static String get(String envName, String propName) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            value = PROPS.getProperty(propName);
        }
        if (value == null || value.isBlank()) {
            throw new AppException("Thiếu cấu hình: đặt biến môi trường " + envName
                    + " hoặc khóa " + propName + " trong db.properties");
        }
        return value;
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new AppException("Lỗi đọc file db.properties", e);
        }
        return props;
    }
}
