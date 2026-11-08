package br.com.vivabem50.service;

import br.com.vivabem50.dto.UserResponse;
import br.com.vivabem50.model.HealthProfile;
import br.com.vivabem50.model.User;
import br.com.vivabem50.repository.HealthProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

@Component
public class UserMapper {

    private final HealthProfileRepository healthProfiles;
    private final ZoneId zoneId;

    public UserMapper(HealthProfileRepository healthProfiles, @Value("${app.timezone}") String timezone) {
        this.healthProfiles = healthProfiles;
        this.zoneId = ZoneId.of(timezone);
    }

    public UserResponse toResponse(User user) {
        String city = healthProfiles.findByUser_Id(user.getId()).map(HealthProfile::getCity).orElse(null);
        int age = Period.between(user.getBirthDate(), LocalDate.now(zoneId)).getYears();
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getBirthDate(), age, city);
    }

    public ZoneId zoneId() {
        return zoneId;
    }
}
