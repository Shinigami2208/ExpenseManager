package com.expense.config;

import com.expense.exception.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("Thiếu thư viện mssql-jdbc trên classpath");
        }
        DriverManager.setLoginTimeout(5);
    }

    private DatabaseConnection() {
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(
                    AppConfig.dbUrl(), AppConfig.dbUsername(), AppConfig.dbPassword());
        } catch (SQLException e) {
            throw new DataAccessException("Không kết nối được SQL Server", e);
        }
    }
}
