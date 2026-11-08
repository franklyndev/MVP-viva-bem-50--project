package br.com.vivabem50.repository;

import br.com.vivabem50.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByUser_IdAndDayOfWeekAndActiveTrueOrderByTimeOfDayAsc(Long userId, Short dayOfWeek);

    Optional<Activity> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByUser_Id(Long userId);
}
