package br.com.vivabem50.service;

import br.com.vivabem50.dto.ChatResponse;
import br.com.vivabem50.dto.DayAgendaResponse;
import br.com.vivabem50.model.AiInteraction;
import br.com.vivabem50.model.User;
import br.com.vivabem50.model.enums.AiUseCase;
import br.com.vivabem50.repository.AiInteractionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
public class AiChatService {

    private final AgendaService agendaService;
    private final AiInteractionRepository interactions;
    private final UserMapper userMapper;

    public AiChatService(AgendaService agendaService, AiInteractionRepository interactions, UserMapper userMapper) {
        this.agendaService = agendaService;
        this.interactions = interactions;
        this.userMapper = userMapper;
    }

    @Transactional
    public ChatResponse reply(User user, String message) {
        LocalDate today = LocalDate.now(userMapper.zoneId());
        DayAgendaResponse day = agendaService.day(user, today);
        String reply = buildReply(message.toLowerCase(), day);

        AiInteraction interaction = new AiInteraction();
        interaction.setUser(user);
        interaction.setUseCase(AiUseCase.ANALISE_AGENDA);
        interaction.setModelName("regras-mvp");
        interaction.setResponseValid(true);
        interactions.save(interaction);

        return new ChatResponse(reply);
    }

    private static String buildReply(String message, DayAgendaResponse day) {
        String firstName = day.user().name().split(" ")[0];
        if (contains(message, "consulta", "agendar", "marcar", "médic", "exame")) {
            if (day.nextAppointment() != null) {
                return "Olá, " + firstName + "! Você já tem " + day.nextAppointment().title()
                        + " às " + day.nextAppointment().time()
                        + ". Para marcar outro compromisso, abra Minha Agenda e toque em Adicionar lembrete.";
            }
            return "Claro, " + firstName + "! Abra Minha Agenda e use Adicionar lembrete para marcar consulta ou exame.";
        }
        if (contains(message, "aliment", "fruta", "comida", "almoço")) {
            return "Que tal incluir uma fruta no lanche da tarde? Banana e mamão ajudam a digestão e dão energia.";
        }
        if (contains(message, "remédio", "lembrar", "vitamina", "comprimido")) {
            return "Combinado. Cadastre o horário em Minha Agenda como um lembrete e eu mostro na linha do tempo do dia.";
        }
        if (contains(message, "exerc", "caminh", "along", "atividade")) {
            return "Experimente 10 minutos de alongamento: gire os ombros devagar e estique os braços. Sua meta do dia é "
                    + day.wellbeing().activityGoal() + " minutos.";
        }
        if (contains(message, "água", "copo", "hidrata")) {
            return "Hoje você está com " + day.wellbeing().waterCups() + " de " + day.wellbeing().waterGoal()
                    + " copos. Um copo agora já ajuda.";
        }
        if (day.items().isEmpty()) {
            return "Olá, " + firstName + "! Sua agenda de hoje está livre. Posso ajudar a marcar uma caminhada ou uma consulta.";
        }
        String summary = day.items().stream()
                .map(item -> item.time() + " " + item.title())
                .collect(Collectors.joining("; "));
        return "Olá, " + firstName + "! Para hoje: " + summary + ". Em que mais posso ajudar?";
    }

    private static boolean contains(String message, String... terms) {
        for (String term : terms) {
            if (message.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
