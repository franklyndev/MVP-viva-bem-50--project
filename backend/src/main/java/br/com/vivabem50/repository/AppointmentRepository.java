package br.com.vivabem50.repository;

import br.com.vivabem50.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /** Busca só se o compromisso pertencer ao usuário. */
    Optional<Appointment> findByIdAndUserId(Long id, Long userId);

    List<Appointment> findByUserIdOrderByStartsAtAsc(Long userId);

    /** Intervalo [from, to): inclui o início e exclui o fim. */
    List<Appointment> findByUserIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            Long userId, OffsetDateTime from, OffsetDateTime to);
}
