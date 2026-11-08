package br.com.vivabem50.repository;

import br.com.vivabem50.model.ActivityRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, Long> {

    Optional<ActivityRecord> findByActivity_IdAndPerformedOn(Long activityId, LocalDate performedOn);

    List<ActivityRecord> findByActivity_IdInAndPerformedOn(Collection<Long> activityIds, LocalDate performedOn);
}
