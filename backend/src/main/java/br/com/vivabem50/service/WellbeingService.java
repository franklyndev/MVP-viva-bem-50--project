package br.com.vivabem50.service;

import br.com.vivabem50.dto.wellbeing.WellbeingResponse;
import br.com.vivabem50.dto.wellbeing.WellbeingUpdateRequest;
import br.com.vivabem50.exception.NotFoundException;
import br.com.vivabem50.model.WellbeingLog;
import br.com.vivabem50.repository.UserRepository;
import br.com.vivabem50.repository.WellbeingLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class WellbeingService {

    public static final int WATER_GOAL = 8;
    public static final int ACTIVITY_GOAL = 30;

    private final WellbeingLogRepository wellbeingLogRepository;
    private final UserRepository userRepository;

    public WellbeingService(WellbeingLogRepository wellbeingLogRepository, UserRepository userRepository) {
        this.wellbeingLogRepository = wellbeingLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public WellbeingResponse get(Long userId, LocalDate date) {
        return wellbeingLogRepository.findByUserIdAndLoggedOn(userId, date)
                .map(WellbeingService::toResponse)
                .orElseGet(() -> new WellbeingResponse(date, 0, WATER_GOAL, 0, ACTIVITY_GOAL, null));
    }

    @Transactional
    public WellbeingResponse update(Long userId, WellbeingUpdateRequest request) {
        WellbeingLog log = wellbeingLogRepository.findByUserIdAndLoggedOn(userId, request.date())
                .orElseGet(() -> {
                    WellbeingLog created = new WellbeingLog();
                    created.setUser(userRepository.findById(userId)
                            .orElseThrow(() -> new NotFoundException("Usuário não encontrado.")));
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
        return toResponse(wellbeingLogRepository.save(log));
    }

    private static WellbeingResponse toResponse(WellbeingLog log) {
        return new WellbeingResponse(log.getLoggedOn(), log.getWaterCups(), WATER_GOAL,
                log.getActivityMinutes(), ACTIVITY_GOAL, log.getMood());
    }
}
