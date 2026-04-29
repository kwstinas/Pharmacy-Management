package com.pharmacy.repository;

import com.pharmacy.model.MedCategory;
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
public class CategoryRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<MedCategory> ROW_MAPPER = (rs, rowNum) -> {
        MedCategory c = new MedCategory();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        return c;
    };

    public CategoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<MedCategory> findAll(Long userId) {
        return jdbc.query("SELECT * FROM med_categories WHERE user_id = ? ORDER BY name", ROW_MAPPER, userId);
    }

    public Optional<MedCategory> findById(Long id, Long userId) {
        List<MedCategory> results = jdbc.query(
                "SELECT * FROM med_categories WHERE id = ? AND user_id = ?", ROW_MAPPER, id, userId);
        return results.stream().findFirst();
    }

    public Optional<MedCategory> findByName(String name, Long userId) {
        List<MedCategory> results = jdbc.query(
                "SELECT * FROM med_categories WHERE name = ? AND user_id = ?", ROW_MAPPER, name, userId);
        return results.stream().findFirst();
    }

    public MedCategory save(MedCategory category, Long userId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO med_categories (name, description, user_id) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setLong(3, userId);
            return ps;
        }, keyHolder);
        category.setId(keyHolder.getKey().longValue());
        return category;
    }

    public int update(MedCategory category, Long userId) {
        return jdbc.update(
                "UPDATE med_categories SET name = ?, description = ? WHERE id = ? AND user_id = ?",
                category.getName(), category.getDescription(), category.getId(), userId);
    }

    public int deleteById(Long id, Long userId) {
        return jdbc.update("DELETE FROM med_categories WHERE id = ? AND user_id = ?", id, userId);
    }

    public int countMedicines(Long categoryId, Long userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM medicines WHERE category_id = ? AND user_id = ?",
                Integer.class, categoryId, userId);
        return count != null ? count : 0;
    }
}