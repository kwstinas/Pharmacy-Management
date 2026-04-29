package com.pharmacy.repository;

import com.pharmacy.model.Medicine;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class MedicineRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Medicine> ROW_MAPPER_WITH_CATEGORY = (rs, rowNum) -> {
        Medicine m = new Medicine();
        m.setId(rs.getLong("id"));
        m.setCode(rs.getString("code"));
        m.setName(rs.getString("m_name"));
        m.setPrice(rs.getBigDecimal("price"));
        m.setStockQty(rs.getInt("stock_qty"));
        m.setCategoryId(rs.getLong("category_id"));
        m.setCategoryName(rs.getString("cat_name"));
        return m;
    };

    private static final String SELECT_WITH_JOIN =
            """
            SELECT m.id, m.code, m.name AS m_name, m.price, m.stock_qty,
                   m.category_id, c.name AS cat_name
            FROM medicines m
            JOIN med_categories c ON m.category_id = c.id
            """;

    public MedicineRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Medicine> findAll(Long userId) {
        return jdbc.query(SELECT_WITH_JOIN + " WHERE m.user_id = ? ORDER BY m.name",
                ROW_MAPPER_WITH_CATEGORY, userId);
    }

    public Optional<Medicine> findById(Long id, Long userId) {
        List<Medicine> results = jdbc.query(
                SELECT_WITH_JOIN + " WHERE m.id = ? AND m.user_id = ?",
                ROW_MAPPER_WITH_CATEGORY, id, userId);
        return results.stream().findFirst();
    }

    public Optional<Medicine> findByCode(String code, Long userId) {
        List<Medicine> results = jdbc.query(
                SELECT_WITH_JOIN + " WHERE m.code = ? AND m.user_id = ?",
                ROW_MAPPER_WITH_CATEGORY, code, userId);
        return results.stream().findFirst();
    }

    public List<Medicine> findByCategory(Long categoryId, Long userId) {
        return jdbc.query(
                SELECT_WITH_JOIN + " WHERE m.category_id = ? AND m.user_id = ? ORDER BY m.name",
                ROW_MAPPER_WITH_CATEGORY, categoryId, userId);
    }

    public List<Medicine> findLowStock(int threshold, Long userId) {
        return jdbc.query(
                SELECT_WITH_JOIN + " WHERE m.stock_qty <= ? AND m.user_id = ? ORDER BY m.stock_qty ASC",
                ROW_MAPPER_WITH_CATEGORY, threshold, userId);
    }

    public List<Medicine> search(String keyword, Long userId) {
        String pattern = "%" + keyword + "%";
        return jdbc.query(
                SELECT_WITH_JOIN + " WHERE (m.name LIKE ? OR m.code LIKE ?) AND m.user_id = ? ORDER BY m.name",
                ROW_MAPPER_WITH_CATEGORY, pattern, pattern, userId);
    }

    public Medicine save(Medicine medicine, Long userId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO medicines (code, name, price, stock_qty, category_id, user_id) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, medicine.getCode());
            ps.setString(2, medicine.getName());
            ps.setBigDecimal(3, medicine.getPrice());
            ps.setInt(4, medicine.getStockQty());
            ps.setLong(5, medicine.getCategoryId());
            ps.setLong(6, userId);
            return ps;
        }, keyHolder);
        medicine.setId(keyHolder.getKey().longValue());
        return medicine;
    }

    public int update(Medicine medicine, Long userId) {
        return jdbc.update(
                "UPDATE medicines SET code = ?, name = ?, price = ?, stock_qty = ?, category_id = ? WHERE id = ? AND user_id = ?",
                medicine.getCode(), medicine.getName(), medicine.getPrice(),
                medicine.getStockQty(), medicine.getCategoryId(), medicine.getId(), userId);
    }

    public int updateStock(Long id, int newQty, Long userId) {
        return jdbc.update(
                "UPDATE medicines SET stock_qty = ? WHERE id = ? AND user_id = ?", newQty, id, userId);
    }

    public int deleteById(Long id, Long userId) {
        return jdbc.update("DELETE FROM medicines WHERE id = ? AND user_id = ?", id, userId);
    }

    public long count(Long userId) {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM medicines WHERE user_id = ?", Long.class, userId);
        return c != null ? c : 0;
    }

    public long countOutOfStock(Long userId) {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM medicines WHERE stock_qty = 0 AND user_id = ?", Long.class, userId);
        return c != null ? c : 0;
    }

    public long countLowStock(int threshold, Long userId) {
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM medicines WHERE stock_qty > 0 AND stock_qty <= ? AND user_id = ?",
                Long.class, threshold, userId);
        return c != null ? c : 0;
    }
}