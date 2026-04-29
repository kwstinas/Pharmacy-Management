package com.pharmacy.repository;

import com.pharmacy.model.MedicineIngredient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class IngredientRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<MedicineIngredient> ROW_MAPPER = (rs, rowNum) -> {
        MedicineIngredient i = new MedicineIngredient();
        i.setId(rs.getLong("id"));
        i.setMedicineId(rs.getLong("medicine_id"));
        i.setIngredientName(rs.getString("ingredient_name"));
        return i;
    };

    public IngredientRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<MedicineIngredient> findByMedicine(Long medicineId) {
        return jdbc.query(
                "SELECT * FROM medicine_ingredients WHERE medicine_id = ? ORDER BY ingredient_name",
                ROW_MAPPER, medicineId);
    }

    public void saveAll(Long medicineId, List<String> ingredients, Long userId) {
        jdbc.update("DELETE FROM medicine_ingredients WHERE medicine_id = ?", medicineId);
        for (String name : ingredients) {
            if (name != null && !name.trim().isEmpty()) {
                jdbc.update(
                        "INSERT INTO medicine_ingredients (medicine_id, ingredient_name, user_id) VALUES (?, ?, ?)",
                        medicineId, name.trim(), userId);
            }
        }
    }

    public void deleteByMedicine(Long medicineId) {
        jdbc.update("DELETE FROM medicine_ingredients WHERE medicine_id = ?", medicineId);
    }

    public List<Long> findMedicineIdsByIngredient(String ingredient, Long userId) {
        String pattern = "%" + ingredient + "%";
        return jdbc.queryForList(
                "SELECT DISTINCT medicine_id FROM medicine_ingredients WHERE ingredient_name LIKE ? AND user_id = ?",
                Long.class, pattern, userId);
    }
}