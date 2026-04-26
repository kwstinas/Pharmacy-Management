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

    // Απλός mapper χωρίς JOIN
    private static final RowMapper<Medicine> ROW_MAPPER = (rs, rowNum) -> {
        Medicine m = new Medicine();
        m.setId(rs.getLong("id"));
        m.setCode(rs.getString("code"));
        m.setName(rs.getString("name"));
        m.setPrice(rs.getBigDecimal("price"));
        m.setStockQty(rs.getInt("stock_qty"));
        m.setCategoryId(rs.getLong("category_id"));
        return m;
    };

    // Mapper με JOIN — φέρνει και το category name
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

    // Βασικό SELECT με JOIN — το χρησιμοποιούμε παντού
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

    public List<Medicine> findAll() {
        return jdbc.query(SELECT_WITH_JOIN + " ORDER BY m.name",
                ROW_MAPPER_WITH_CATEGORY);
    }

    public Optional<Medicine> findById(Long id) {
        List<Medicine> results = jdbc.query(
                SELECT_WITH_JOIN + " WHERE m.id = ?",
                ROW_MAPPER_WITH_CATEGORY, id);
        return results.stream().findFirst();
    }

    public Optional<Medicine> findByCode(String code) {
        List<Medicine> results = jdbc.query(
                SELECT_WITH_JOIN + " WHERE m.code = ?",
                ROW_MAPPER_WITH_CATEGORY, code);
        return results.stream().findFirst();
    }

    public List<Medicine> findByCategory(Long categoryId) {
        return jdbc.query(
                SELECT_WITH_JOIN + " WHERE m.category_id = ? ORDER BY m.name",
                ROW_MAPPER_WITH_CATEGORY, categoryId);
    }

    public List<Medicine> findLowStock(int threshold) {
        return jdbc.query(
                SELECT_WITH_JOIN + " WHERE m.stock_qty <= ? ORDER BY m.stock_qty ASC",
                ROW_MAPPER_WITH_CATEGORY, threshold);
    }

    public List<Medicine> search(String keyword) {
        String pattern = "%" + keyword + "%";
        return jdbc.query(
                SELECT_WITH_JOIN + " WHERE m.name LIKE ? OR m.code LIKE ? ORDER BY m.name",
                ROW_MAPPER_WITH_CATEGORY, pattern, pattern);
    }

    public Medicine save(Medicine medicine) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO medicines (code, name, price, stock_qty, category_id)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, medicine.getCode());
            ps.setString(2, medicine.getName());
            ps.setBigDecimal(3, medicine.getPrice());
            ps.setInt(4, medicine.getStockQty());
            ps.setLong(5, medicine.getCategoryId());
            return ps;
        }, keyHolder);
        medicine.setId(keyHolder.getKey().longValue());
        return medicine;
    }

    public int update(Medicine medicine) {
        return jdbc.update(
                """
                UPDATE medicines SET code = ?, name = ?, price = ?,
                       stock_qty = ?, category_id = ?
                WHERE id = ?
                """,
                medicine.getCode(), medicine.getName(), medicine.getPrice(),
                medicine.getStockQty(), medicine.getCategoryId(), medicine.getId());
    }

    public int updateStock(Long id, int newQty) {
        return jdbc.update(
                "UPDATE medicines SET stock_qty = ? WHERE id = ?", newQty, id);
    }

    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM medicines WHERE id = ?", id);
    }

    public long count() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM medicines", Long.class);
        return c != null ? c : 0;
    }

    public long countOutOfStock() {
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM medicines WHERE stock_qty = 0", Long.class);
        return c != null ? c : 0;
    }

    public long countLowStock(int threshold) {
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM medicines WHERE stock_qty > 0 AND stock_qty <= ?",
                Long.class, threshold);
        return c != null ? c : 0;
    }
}