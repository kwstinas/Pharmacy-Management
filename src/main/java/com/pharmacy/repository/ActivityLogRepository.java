package com.pharmacy.repository;

import com.pharmacy.model.ActivityLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class ActivityLogRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<ActivityLog> ROW_MAPPER = (rs, rowNum) -> {
        ActivityLog log = new ActivityLog();
        log.setId(rs.getLong("id"));
        log.setAction(rs.getString("action"));
        log.setEntityType(rs.getString("entity_type"));
        log.setEntityId(rs.getLong("entity_id"));
        log.setDescription(rs.getString("description"));
        log.setOccurredAt(rs.getTimestamp("occurred_at").toLocalDateTime());
        return log;
    };

    public ActivityLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(String action, String entityType, Long entityId, String description) {
        jdbc.update(
                "INSERT INTO activity_log (action, entity_type, entity_id, description, occurred_at) VALUES (?, ?, ?, ?, ?)",
                action, entityType, entityId, description, Timestamp.valueOf(LocalDateTime.now()));
    }

    public List<ActivityLog> findAll(int limit) {
        return jdbc.query(
                "SELECT * FROM activity_log ORDER BY occurred_at DESC LIMIT ?",
                ROW_MAPPER, limit);
    }

    public List<ActivityLog> findByDateRange(LocalDateTime from, LocalDateTime to) {
        return jdbc.query(
                "SELECT * FROM activity_log WHERE occurred_at BETWEEN ? AND ? ORDER BY occurred_at DESC",
                ROW_MAPPER, Timestamp.valueOf(from), Timestamp.valueOf(to));
    }

    /**
     * Στατιστικά ανά action για μια περίοδο
     */
    public List<Map<String, Object>> statsByPeriod(LocalDateTime from, LocalDateTime to, String groupBy) {
        // groupBy: "month" ή "day"
        String dateFormat = groupBy.equals("day") ? "%Y-%m-%d" : "%Y-%m";
        return jdbc.queryForList(
                """
                SELECT DATE_FORMAT(occurred_at, ?) AS period,
                       action,
                       COUNT(*) AS count
                FROM activity_log
                WHERE occurred_at BETWEEN ? AND ?
                GROUP BY period, action
                ORDER BY period DESC, action
                """, dateFormat, Timestamp.valueOf(from), Timestamp.valueOf(to));
    }

    /**
     * Σύνοψη: πόσα CREATE, UPDATE, DELETE, STOCK_IN, STOCK_OUT σε μια περίοδο
     */
    public List<Map<String, Object>> summary(LocalDateTime from, LocalDateTime to) {
        return jdbc.queryForList(
                """
                SELECT action, COUNT(*) AS count
                FROM activity_log
                WHERE occurred_at BETWEEN ? AND ?
                GROUP BY action
                ORDER BY count DESC
                """, Timestamp.valueOf(from), Timestamp.valueOf(to));
    }

    /**
     * Top φάρμακα με τις περισσότερες κινήσεις
     */
    public List<Map<String, Object>> topMedicines(LocalDateTime from, LocalDateTime to, int limit) {
        return jdbc.queryForList(
                """
                SELECT al.entity_id, al.description AS medicine_name,
                       COUNT(*) AS movement_count,
                       SUM(CASE WHEN al.action = 'STOCK_IN' THEN 1 ELSE 0 END) AS total_in,
                       SUM(CASE WHEN al.action = 'STOCK_OUT' THEN 1 ELSE 0 END) AS total_out
                FROM activity_log al
                WHERE al.occurred_at BETWEEN ? AND ?
                  AND al.entity_type = 'MEDICINE'
                  AND al.action IN ('STOCK_IN', 'STOCK_OUT')
                GROUP BY al.entity_id, al.description
                ORDER BY movement_count DESC
                LIMIT ?
                """, Timestamp.valueOf(from), Timestamp.valueOf(to), limit);
    }
}