package br.com.vivabem50.repository;

import br.com.vivabem50.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByUserIdAndDayOfWeekAndActiveTrueOrderByTimeOfDayAsc(Long userId, Short dayOfWeek);

    /** Busca só se a atividade pertencer ao usuário. */
    Optional<Activity> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserId(Long userId);
}
