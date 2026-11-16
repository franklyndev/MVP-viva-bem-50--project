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
import java.math.BigDecimal;

@Service
public class WellbeingService {

    public static final BigDecimal WATER_GOAL_LITERS = BigDecimal.valueOf(2);
    public static final int ACTIVITY_GOAL = 30;

    private final WellbeingLogRepository wellbeingLogRepository;
    private final UserRepository userRepository;

    public WellbeingService(WellbeingLogRepository wellbeingLogRepository, UserRepository userRepository) {
        this.wellbeingLogRepository = wellbeingLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public WellbeingResponse get(Long userId, LocalDate date) {
        int activityGoal = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."))
                .getActivityGoal();
        return wellbeingLogRepository.findByUserIdAndLoggedOn(userId, date)
                .map(log -> toResponse(log, activityGoal))
                .orElseGet(() -> new WellbeingResponse(date, BigDecimal.ZERO, WATER_GOAL_LITERS, 0, activityGoal, null));
    }

    @Transactional
    public WellbeingResponse update(Long userId, WellbeingUpdateRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        if (request.activityGoal() != null) {
            user.setActivityGoal(request.activityGoal());
        }
        WellbeingLog log = wellbeingLogRepository.findByUserIdAndLoggedOn(userId, request.date())
                .orElseGet(() -> {
                    WellbeingLog created = new WellbeingLog();
                    created.setUser(user);
                    created.setLoggedOn(request.date());
                    return created;
                });
        if (request.waterLiters() != null) {
            log.setWaterLiters(request.waterLiters());
        } else if (request.waterCups() != null) {
            log.setWaterLiters(BigDecimal.valueOf(request.waterCups()).multiply(BigDecimal.valueOf(0.25)));
        }
        if (request.activityMinutes() != null) {
            log.setActivityMinutes(request.activityMinutes());
        }
        if (request.mood() != null) {
            log.setMood(request.mood());
        }
        return toResponse(wellbeingLogRepository.save(log), user.getActivityGoal());
    }

    private static WellbeingResponse toResponse(WellbeingLog log, int activityGoal) {
        return new WellbeingResponse(log.getLoggedOn(), log.getWaterLiters(), WATER_GOAL_LITERS,
                log.getActivityMinutes(), activityGoal, log.getMood());
    }
}
