package com.pharmacy.repository;

import com.pharmacy.model.StockMovement;
import com.pharmacy.model.StockMovement.MovementType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class StockMovementRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StockMovement> ROW_MAPPER_WITH_NAME = (rs, rowNum) -> {
        StockMovement sm = new StockMovement();
        sm.setId(rs.getLong("id"));
        sm.setMedicineId(rs.getLong("medicine_id"));
        sm.setType(MovementType.valueOf(rs.getString("type")));
        sm.setQuantity(rs.getInt("quantity"));
        sm.setOccurredAt(rs.getTimestamp("occurred_at").toLocalDateTime());
        sm.setNote(rs.getString("note"));
        sm.setMedicineName(rs.getString("med_name"));
        return sm;
    };

    private static final String SELECT_WITH_JOIN =
            """
            SELECT sm.id, sm.medicine_id, sm.type, sm.quantity,
                   sm.occurred_at, sm.note, m.name AS med_name
            FROM stock_movements sm
            JOIN medicines m ON sm.medicine_id = m.id
            """;

    public StockMovementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<StockMovement> findAll(int limit) {
        return jdbc.query(
                SELECT_WITH_JOIN + " ORDER BY sm.occurred_at DESC LIMIT ?",
                ROW_MAPPER_WITH_NAME, limit);
    }

    public List<StockMovement> findByMedicine(Long medicineId) {
        return jdbc.query(
                SELECT_WITH_JOIN + " WHERE sm.medicine_id = ? ORDER BY sm.occurred_at DESC",
                ROW_MAPPER_WITH_NAME, medicineId);
    }

    public List<StockMovement> findByDateRange(LocalDateTime from, LocalDateTime to) {
        return jdbc.query(
                SELECT_WITH_JOIN + " WHERE sm.occurred_at BETWEEN ? AND ? ORDER BY sm.occurred_at DESC",
                ROW_MAPPER_WITH_NAME, Timestamp.valueOf(from), Timestamp.valueOf(to));
    }

    public StockMovement save(StockMovement movement) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO stock_movements (medicine_id, type, quantity, occurred_at, note)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, movement.getMedicineId());
            ps.setString(2, movement.getType().name());
            ps.setInt(3, movement.getQuantity());
            ps.setTimestamp(4, movement.getOccurredAt() != null
                    ? Timestamp.valueOf(movement.getOccurredAt())
                    : Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, movement.getNote());
            return ps;
        }, keyHolder);
        movement.setId(keyHolder.getKey().longValue());
        return movement;
    }

    // Μηνιαία σύνοψη IN/OUT
    public List<Map<String, Object>> monthlySummary(int months) {
        return jdbc.queryForList(
                """
                SELECT DATE_FORMAT(occurred_at, '%Y-%m') AS period,
                       SUM(CASE WHEN type = 'IN'  THEN quantity ELSE 0 END) AS total_in,
                       SUM(CASE WHEN type = 'OUT' THEN quantity ELSE 0 END) AS total_out
                FROM stock_movements
                WHERE occurred_at >= DATE_SUB(NOW(), INTERVAL ? MONTH)
                GROUP BY period
                ORDER BY period DESC
                """, months);
    }

    // Στατιστικά ανά κατηγορία
    public List<Map<String, Object>> categoryStats() {
        return jdbc.queryForList(
                """
                SELECT c.id AS category_id, c.name AS category_name,
                       COUNT(m.id) AS medicine_count,
                       COALESCE(SUM(m.stock_qty), 0) AS total_stock,
                       COALESCE(SUM(m.stock_qty * m.price), 0) AS total_value
                FROM med_categories c
                LEFT JOIN medicines m ON c.id = m.category_id
                GROUP BY c.id, c.name
                ORDER BY total_value DESC
                """);
    }
}