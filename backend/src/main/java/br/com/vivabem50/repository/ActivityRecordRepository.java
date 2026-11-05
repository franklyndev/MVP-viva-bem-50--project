package br.com.vivabem50.repository;

import br.com.vivabem50.model.ActivityRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, Long> {
}
