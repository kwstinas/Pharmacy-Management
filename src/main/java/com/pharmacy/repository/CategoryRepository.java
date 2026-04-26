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

    // Αυτό μετατρέπει κάθε row της βάσης σε MedCategory object
    private static final RowMapper<MedCategory> ROW_MAPPER = (rs, rowNum) -> {
        MedCategory c = new MedCategory();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        return c;
    };

    // Constructor injection — το Spring δίνει αυτόματα το JdbcTemplate
    public CategoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<MedCategory> findAll() {
        return jdbc.query("SELECT * FROM med_categories ORDER BY name", ROW_MAPPER);
    }

    public Optional<MedCategory> findById(Long id) {
        List<MedCategory> results = jdbc.query(
                "SELECT * FROM med_categories WHERE id = ?", ROW_MAPPER, id);
        return results.stream().findFirst();
    }

    public Optional<MedCategory> findByName(String name) {
        List<MedCategory> results = jdbc.query(
                "SELECT * FROM med_categories WHERE name = ?", ROW_MAPPER, name);
        return results.stream().findFirst();
    }

    public MedCategory save(MedCategory category) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO med_categories (name, description) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            return ps;
        }, keyHolder);
        category.setId(keyHolder.getKey().longValue());
        return category;
    }

    public int update(MedCategory category) {
        return jdbc.update(
                "UPDATE med_categories SET name = ?, description = ? WHERE id = ?",
                category.getName(), category.getDescription(), category.getId());
    }

    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM med_categories WHERE id = ?", id);
    }

    public int countMedicines(Long categoryId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM medicines WHERE category_id = ?",
                Integer.class, categoryId);
        return count != null ? count : 0;
    }
}