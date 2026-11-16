package br.com.vivabem50.dto.profile;

import br.com.vivabem50.model.enums.ActivityLevel;

public record HealthProfileResponse(ActivityLevel activityLevel, String notes) {
}
