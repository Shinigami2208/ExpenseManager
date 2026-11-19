package com.expense.repository;

import com.expense.config.DatabaseConnection;
import com.expense.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class TransactionManager {

    private static final Logger log = LoggerFactory.getLogger(TransactionManager.class);

    public <T> T inTransaction(TransactionCallback<T> work) {
        return inTransaction(Connection.TRANSACTION_READ_COMMITTED, work);
    }

    public <T> T inTransaction(int isolationLevel, TransactionCallback<T> work) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setTransactionIsolation(isolationLevel);
            conn.setAutoCommit(false);
            try {
                T result = work.doInTransaction(conn);
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi chạy transaction", e);
        }
    }

    private void rollback(Connection conn, Exception cause) {
        try {
            conn.rollback();
            log.warn("Đã rollback transaction vì: {}", cause.getMessage());
        } catch (SQLException rollbackError) {
            cause.addSuppressed(rollbackError);
        }
    }
}
