package com.expense.dto;

public record ImportResult(int imported, int batches, int batchSize, long millis) {
}
