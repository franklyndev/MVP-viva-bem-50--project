package br.com.vivabem50.repository;

import br.com.vivabem50.model.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findAllByOrderByIdAsc();

    List<Goal> findByCodeIn(Collection<String> codes);
}
