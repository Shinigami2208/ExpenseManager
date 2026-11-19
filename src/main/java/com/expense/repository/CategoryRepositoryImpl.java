package com.expense.repository;

import com.expense.config.DatabaseConnection;
import com.expense.exception.ConflictException;
import com.expense.exception.DataAccessException;
import com.expense.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryRepositoryImpl implements CategoryRepository {

    private static final int ERR_UNIQUE_VIOLATION = 2627;
    private static final int ERR_DUPLICATE_INDEX = 2601;
    private static final int ERR_FK_VIOLATION = 547;

    @Override
    public List<Category> findAll() {
        String sql = "SELECT id, name FROM categories ORDER BY id";
        List<Category> result = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi lấy danh sách danh mục", e);
        }
        return result;
    }

    @Override
    public Optional<Category> findById(int id) {
        String sql = "SELECT id, name FROM categories WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi tìm danh mục id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Category save(Category category) {
        String sql = "INSERT INTO categories (name) VALUES (?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, category.getName());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    category.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == ERR_UNIQUE_VIOLATION || e.getErrorCode() == ERR_DUPLICATE_INDEX) {
                throw new ConflictException("Danh mục '" + category.getName() + "' đã tồn tại");
            }
            throw new DataAccessException("Lỗi thêm danh mục '" + category.getName() + "'", e);
        }
        return category;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM categories WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == ERR_FK_VIOLATION) {
                throw new ConflictException("Không thể xóa danh mục id=" + id
                        + " vì đang có khoản chi thuộc danh mục này");
            }
            throw new DataAccessException("Lỗi xóa danh mục id=" + id, e);
        }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        return new Category(rs.getInt("id"), rs.getString("name"));
    }
}
