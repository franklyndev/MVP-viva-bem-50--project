package br.com.vivabem50.service;

import br.com.vivabem50.dto.profile.UserResponse;
import br.com.vivabem50.model.HealthProfile;
import br.com.vivabem50.model.User;
import br.com.vivabem50.repository.HealthProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

/** Monta os dados públicos do usuário e concentra o fuso horário usado nas datas do dia. */
@Component
public class UserMapper {

    private final HealthProfileRepository healthProfileRepository;
    private final ZoneId zoneId;

    public UserMapper(HealthProfileRepository healthProfileRepository,
                      @Value("${app.timezone:America/Maceio}") String timezone) {
        this.healthProfileRepository = healthProfileRepository;
        this.zoneId = ZoneId.of(timezone);
    }

    public UserResponse toResponse(User user) {
        String city = healthProfileRepository.findByUserId(user.getId())
                .map(HealthProfile::getCity)
                .orElse(null);
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getBirthDate(),
                age(user.getBirthDate()), city);
    }

    public int age(LocalDate birthDate) {
        return Period.between(birthDate, today()).getYears();
    }

    public LocalDate today() {
        return LocalDate.now(zoneId);
    }

    public ZoneId zoneId() {
        return zoneId;
    }
}
