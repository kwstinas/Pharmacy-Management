package com.pharmacy.service;

import com.pharmacy.dto.Dtos.*;
import com.pharmacy.model.ActivityLog;
import com.pharmacy.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class LogService {

    private final ActivityLogRepository logRepo;

    public LogService(ActivityLogRepository logRepo) {
        this.logRepo = logRepo;
    }

    public List<ActivityLogResponse> getRecentLogs(int limit) {
        return logRepo.findAll(limit).stream()
                .map(this::toResponse).toList();
    }

    public List<ActivityLogResponse> getLogsByDateRange(LocalDateTime from, LocalDateTime to) {
        return logRepo.findByDateRange(from, to).stream()
                .map(this::toResponse).toList();
    }

    public LogStats getStats(LocalDateTime from, LocalDateTime to, String groupBy) {
        if (groupBy == null || groupBy.isEmpty()) groupBy = "month";

        List<ActionCount> summary = logRepo.summary(from, to).stream()
                .map(row -> new ActionCount(
                        (String) row.get("action"),
                        ((Number) row.get("count")).intValue()
                )).toList();

        List<PeriodActionCount> breakdown = logRepo.statsByPeriod(from, to, groupBy).stream()
                .map(row -> new PeriodActionCount(
                        (String) row.get("period"),
                        (String) row.get("action"),
                        ((Number) row.get("count")).intValue()
                )).toList();

        List<TopMedicine> topMedicines = logRepo.topMedicines(from, to, 10).stream()
                .map(row -> new TopMedicine(
                        ((Number) row.get("entity_id")).longValue(),
                        (String) row.get("medicine_name"),
                        ((Number) row.get("movement_count")).intValue(),
                        ((Number) row.get("total_in")).intValue(),
                        ((Number) row.get("total_out")).intValue()
                )).toList();

        return new LogStats(from, to, summary, breakdown, topMedicines);
    }

    private ActivityLogResponse toResponse(ActivityLog log) {
        return new ActivityLogResponse(
                log.getId(), log.getAction(), log.getEntityType(),
                log.getEntityId(), log.getDescription(), log.getOccurredAt());
    }
}