package br.com.vivabem50.repository;

import br.com.vivabem50.model.WellbeingLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WellbeingLogRepository extends JpaRepository<WellbeingLog, Long> {

    Optional<WellbeingLog> findByUser_IdAndLoggedOn(Long userId, LocalDate loggedOn);
}
