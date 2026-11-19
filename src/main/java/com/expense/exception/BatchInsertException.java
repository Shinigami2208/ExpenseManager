package com.expense.exception;

import java.sql.SQLException;

public class BatchInsertException extends DataAccessException {

    private final int failedIndex;

    public BatchInsertException(int failedIndex, SQLException cause) {
        super("Batch insert lỗi ở phần tử thứ " + failedIndex + ": " + cause.getMessage(), cause);
        this.failedIndex = failedIndex;
    }

    public int getFailedIndex() {
        return failedIndex;
    }
}
