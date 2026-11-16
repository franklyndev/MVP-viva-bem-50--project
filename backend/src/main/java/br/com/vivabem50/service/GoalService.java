package br.com.vivabem50.service;

import br.com.vivabem50.dto.profile.GoalResponse;
import br.com.vivabem50.repository.GoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GoalService {

    private final GoalRepository goalRepository;

    public GoalService(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> listAll() {
        return goalRepository.findAllByOrderByIdAsc().stream()
                .map(goal -> new GoalResponse(goal.getCode(), goal.getLabel()))
                .toList();
    }
}
