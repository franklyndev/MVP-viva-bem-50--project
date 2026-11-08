package br.com.vivabem50.service;

import br.com.vivabem50.dto.WellbeingResponse;
import br.com.vivabem50.dto.WellbeingUpdateRequest;
import br.com.vivabem50.model.User;
import br.com.vivabem50.model.WellbeingLog;
import br.com.vivabem50.repository.WellbeingLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class WellbeingService {

    public static final int WATER_GOAL = 8;
    public static final int ACTIVITY_GOAL = 30;

    private final WellbeingLogRepository logs;

    public WellbeingService(WellbeingLogRepository logs) {
        this.logs = logs;
    }

    @Transactional(readOnly = true)
    public WellbeingResponse get(User user, LocalDate date) {
        return logs.findByUser_IdAndLoggedOn(user.getId(), date)
                .map(this::toResponse)
                .orElseGet(() -> new WellbeingResponse(date, 0, WATER_GOAL, 0, ACTIVITY_GOAL, null));
    }

    @Transactional
    public WellbeingResponse update(User user, WellbeingUpdateRequest request) {
        WellbeingLog log = logs.findByUser_IdAndLoggedOn(user.getId(), request.date())
                .orElseGet(() -> {
                    WellbeingLog created = new WellbeingLog();
                    created.setUser(user);
                    created.setLoggedOn(request.date());
                    return created;
                });
        if (request.waterCups() != null) {
            log.setWaterCups(request.waterCups());
        }
        if (request.activityMinutes() != null) {
            log.setActivityMinutes(request.activityMinutes());
        }
        if (request.mood() != null) {
            log.setMood(request.mood());
        }
        logs.save(log);
        return toResponse(log);
    }

    private WellbeingResponse toResponse(WellbeingLog log) {
        return new WellbeingResponse(
                log.getLoggedOn(),
                log.getWaterCups(),
                WATER_GOAL,
                log.getActivityMinutes(),
                ACTIVITY_GOAL,
                log.getMood()
        );
    }
}
