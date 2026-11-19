package com.expense.repository;

import com.expense.config.DatabaseConnection;
import com.expense.exception.ConflictException;
import com.expense.exception.DataAccessException;
import com.expense.model.Transfer;
import com.expense.model.Wallet;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WalletRepositoryImpl implements WalletRepository {

    private static final int ERR_UNIQUE_VIOLATION = 2627;
    private static final int ERR_DUPLICATE_INDEX = 2601;

    @Override
    public List<Wallet> findAll() {
        String sql = "SELECT id, name, balance FROM wallets ORDER BY id";
        List<Wallet> result = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi lấy danh sách ví", e);
        }
        return result;
    }

    @Override
    public Optional<Wallet> findById(int id) {
        String sql = "SELECT id, name, balance FROM wallets WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi tìm ví id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Wallet save(String name, BigDecimal balance) {
        String sql = "INSERT INTO wallets (name, balance) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setBigDecimal(2, balance);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Wallet(keys.getInt(1), name, balance);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == ERR_UNIQUE_VIOLATION || e.getErrorCode() == ERR_DUPLICATE_INDEX) {
                throw new ConflictException("Ví '" + name + "' đã tồn tại");
            }
            throw new DataAccessException("Lỗi thêm ví '" + name + "'", e);
        }
    }

    @Override
    public boolean debit(Connection conn, int walletId, BigDecimal amount) {
        String sql = "UPDATE wallets SET balance = balance - ? WHERE id = ? AND balance >= ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, walletId);
            ps.setBigDecimal(3, amount);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi trừ tiền ví id=" + walletId, e);
        }
    }

    @Override
    public void credit(Connection conn, int walletId, BigDecimal amount) {
        String sql = "UPDATE wallets SET balance = balance + ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, walletId);
            if (ps.executeUpdate() != 1) {
                throw new DataAccessException("Không cộng được tiền: ví id=" + walletId + " không tồn tại", null);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi cộng tiền ví id=" + walletId, e);
        }
    }

    @Override
    public int saveTransfer(Connection conn, int fromWalletId, int toWalletId, BigDecimal amount, String note) {
        String sql = "INSERT INTO transfers (from_wallet_id, to_wallet_id, amount, note) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, fromWalletId);
            ps.setInt(2, toWalletId);
            ps.setBigDecimal(3, amount);
            ps.setString(4, note);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi ghi lịch sử chuyển tiền", e);
        }
    }

    private static final String TRANSFER_SELECT = """
            SELECT t.id, t.from_wallet_id, f.name AS from_name,
                   t.to_wallet_id, w.name AS to_name, t.amount, t.note, t.created_at
            FROM transfers t
            JOIN wallets f ON f.id = t.from_wallet_id
            JOIN wallets w ON w.id = t.to_wallet_id
            """;

    @Override
    public Optional<Transfer> findTransferById(int id) {
        String sql = TRANSFER_SELECT + " WHERE t.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapTransfer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi tìm giao dịch chuyển tiền id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Transfer> findRecentTransfers(int limit) {
        String sql = TRANSFER_SELECT.replaceFirst("SELECT", "SELECT TOP (?)") + " ORDER BY t.id DESC";
        List<Transfer> result = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapTransfer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi lấy lịch sử chuyển tiền", e);
        }
        return result;
    }

    private Transfer mapTransfer(ResultSet rs) throws SQLException {
        return new Transfer(
                rs.getInt("id"),
                rs.getInt("from_wallet_id"), rs.getString("from_name"),
                rs.getInt("to_wallet_id"), rs.getString("to_name"),
                rs.getBigDecimal("amount"), rs.getString("note"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private Wallet mapRow(ResultSet rs) throws SQLException {
        return new Wallet(rs.getInt("id"), rs.getString("name"), rs.getBigDecimal("balance"));
    }
}
