package br.com.vivabem50.repository;

import br.com.vivabem50.model.HealthProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthProfileRepository extends JpaRepository<HealthProfile, Long> {
}
