package br.com.vivabem50.service;

import br.com.vivabem50.dto.profile.GoalResponse;
import br.com.vivabem50.dto.profile.HealthProfileRequest;
import br.com.vivabem50.dto.profile.HealthProfileResponse;
import br.com.vivabem50.dto.profile.ProfileResponse;
import br.com.vivabem50.dto.profile.UpdateGoalsRequest;
import br.com.vivabem50.dto.profile.UpdateProfileRequest;
import br.com.vivabem50.exception.BusinessRuleException;
import br.com.vivabem50.exception.NotFoundException;
import br.com.vivabem50.model.Goal;
import br.com.vivabem50.model.HealthProfile;
import br.com.vivabem50.model.User;
import br.com.vivabem50.repository.GoalRepository;
import br.com.vivabem50.repository.HealthProfileRepository;
import br.com.vivabem50.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final GoalRepository goalRepository;

    public ProfileService(UserRepository userRepository, HealthProfileRepository healthProfileRepository,
                          GoalRepository goalRepository) {
        this.userRepository = userRepository;
        this.healthProfileRepository = healthProfileRepository;
        this.goalRepository = goalRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse get(Long userId) {
        return toResponse(findUser(userId));
    }

    @Transactional
    public ProfileResponse update(Long userId, UpdateProfileRequest request) {
        BirthDates.validate(request.birthDate());
        User user = findUser(userId);
        user.setName(request.name().trim());
        user.setBirthDate(request.birthDate());
        return toResponse(user);
    }

    @Transactional
    public ProfileResponse updateHealthProfile(Long userId, HealthProfileRequest request) {
        User user = findUser(userId);
        HealthProfile profile = healthProfileRepository.findByUserId(userId).orElseGet(() -> {
            HealthProfile created = new HealthProfile();
            created.setUser(user);
            return created;
        });
        profile.setActivityLevel(request.activityLevel());
        profile.setNotes(request.notes());
        healthProfileRepository.save(profile);
        return toResponse(user);
    }

    @Transactional
    public ProfileResponse updateGoals(Long userId, UpdateGoalsRequest request) {
        User user = findUser(userId);
        Set<String> codes = new LinkedHashSet<>(request.goalCodes());
        List<Goal> goals = goalRepository.findByCodeIn(codes);
        if (goals.size() != codes.size()) {
            Set<String> found = goals.stream().map(Goal::getCode).collect(Collectors.toSet());
            codes.removeAll(found);
            throw new BusinessRuleException("Objetivo desconhecido: " + String.join(", ", codes) + ".");
        }
        user.getGoals().clear();
        user.getGoals().addAll(goals);
        return toResponse(user);
    }

    /** Exclui a conta e, em cascata no banco, todos os dados do usuário (LGPD). */
    @Transactional
    public void delete(Long userId) {
        userRepository.delete(findUser(userId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
    }

    private ProfileResponse toResponse(User user) {
        List<GoalResponse> goals = user.getGoals().stream()
                .sorted(Comparator.comparing(Goal::getId))
                .map(goal -> new GoalResponse(goal.getCode(), goal.getLabel()))
                .toList();
        HealthProfileResponse health = healthProfileRepository.findByUserId(user.getId())
                .map(profile -> new HealthProfileResponse(profile.getActivityLevel(), profile.getNotes()))
                .orElse(null);
        return new ProfileResponse(user.getId(), user.getName(), user.getEmail(), user.getBirthDate(),
                BirthDates.age(user.getBirthDate()), goals, health);
    }
}
