package br.com.vivabem50.repository;

import br.com.vivabem50.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByUser_IdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            Long userId, OffsetDateTime from, OffsetDateTime to);

    Optional<Appointment> findByIdAndUser_Id(Long id, Long userId);
}
