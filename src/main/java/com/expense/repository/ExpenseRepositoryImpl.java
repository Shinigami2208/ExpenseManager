package com.expense.repository;

import com.expense.config.DatabaseConnection;
import com.expense.exception.BatchInsertException;
import com.expense.exception.DataAccessException;
import com.expense.model.Expense;

import java.math.BigDecimal;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExpenseRepositoryImpl implements ExpenseRepository {

    private static final String BASE_SELECT = """
            SELECT e.id, e.category_id, c.name AS category_name,
                   e.amount, e.note, e.expense_date,
                   e.wallet_id, w.name AS wallet_name
            FROM expenses e
            JOIN categories c ON c.id = e.category_id
            LEFT JOIN wallets w ON w.id = e.wallet_id
            """;

    private static final String INSERT_SQL =
            "INSERT INTO expenses (category_id, amount, note, expense_date, wallet_id) VALUES (?, ?, ?, ?, ?)";

    @Override
    public List<Expense> findAll() {
        String sql = BASE_SELECT + " ORDER BY e.expense_date DESC, e.id DESC";
        List<Expense> result = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi lấy danh sách chi tiêu", e);
        }
        return result;
    }

    @Override
    public Optional<Expense> findById(int id) {
        String sql = BASE_SELECT + " WHERE e.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi tìm chi tiêu id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Expense> findByDateRange(LocalDate from, LocalDate to) {
        String sql = BASE_SELECT + " WHERE e.expense_date BETWEEN ? AND ? ORDER BY e.expense_date";
        List<Expense> result = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi lọc chi tiêu theo ngày", e);
        }
        return result;
    }

    @Override
    public Expense save(Expense expense) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return save(conn, expense);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi thêm khoản chi", e);
        }
    }

    @Override
    public boolean update(Expense expense) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return update(conn, expense);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi cập nhật khoản chi id=" + expense.getId(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return delete(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi xóa khoản chi id=" + id, e);
        }
    }

    @Override
    public Expense save(Connection conn, Expense expense) {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bindInsert(ps, expense);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    expense.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi thêm khoản chi", e);
        }
        return expense;
    }

    @Override
    public boolean update(Connection conn, Expense expense) {
        String sql = "UPDATE expenses SET category_id = ?, amount = ?, note = ?, expense_date = ?, wallet_id = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindInsert(ps, expense);
            ps.setInt(6, expense.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi cập nhật khoản chi id=" + expense.getId(), e);
        }
    }

    @Override
    public boolean delete(Connection conn, int id) {
        String sql = "DELETE FROM expenses WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi xóa khoản chi id=" + id, e);
        }
    }

    @Override
    public int insertBatch(Connection conn, List<Expense> expenses, int batchSize) {
        int sent = 0;

        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            int inBatch = 0;
            for (Expense e : expenses) {
                bindInsert(ps, e);
                ps.addBatch();
                if (++inBatch == batchSize) {
                    ps.executeBatch();
                    sent += inBatch;
                    inBatch = 0;
                }
            }
            if (inBatch > 0) {
                ps.executeBatch();
                sent += inBatch;
            }
            return sent;
        } catch (BatchUpdateException e) {
            throw new BatchInsertException(sent + firstFailed(e.getUpdateCounts()), e);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi nhập hàng loạt khoản chi", e);
        }
    }

    private void bindInsert(PreparedStatement ps, Expense expense) throws SQLException {
        ps.setInt(1, expense.getCategoryId());
        ps.setBigDecimal(2, expense.getAmount());
        ps.setString(3, expense.getNote());
        ps.setDate(4, Date.valueOf(expense.getExpenseDate()));
        ps.setObject(5, expense.getWalletId(), Types.INTEGER);
    }

    private static int firstFailed(int[] counts) {
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] == Statement.EXECUTE_FAILED) {
                return i;
            }
        }
        return counts.length;
    }

    @Override
    public Map<String, BigDecimal> sumByCategory() {
        String sql = """
                SELECT c.name, SUM(e.amount) AS total
                FROM expenses e
                JOIN categories c ON c.id = e.category_id
                GROUP BY c.name
                ORDER BY total DESC
                """;
        Map<String, BigDecimal> result = new LinkedHashMap<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.put(rs.getString("name"), rs.getBigDecimal("total"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi thống kê theo danh mục", e);
        }
        return result;
    }

    @Override
    public BigDecimal sumAll() {
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total FROM expenses";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            return rs.getBigDecimal("total");
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi tính tổng chi tiêu", e);
        }
    }

    private Expense mapRow(ResultSet rs) throws SQLException {
        Expense e = new Expense();
        e.setId(rs.getInt("id"));
        e.setCategoryId(rs.getInt("category_id"));
        e.setCategoryName(rs.getString("category_name"));
        e.setAmount(rs.getBigDecimal("amount"));
        e.setNote(rs.getString("note"));
        e.setExpenseDate(rs.getDate("expense_date").toLocalDate());
        e.setWalletId(rs.getObject("wallet_id", Integer.class));
        e.setWalletName(rs.getString("wallet_name"));
        return e;
    }
}
