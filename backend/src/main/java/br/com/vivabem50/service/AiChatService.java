package br.com.vivabem50.service;

import br.com.vivabem50.dto.agenda.DayAgendaResponse;
import br.com.vivabem50.dto.ai.ChatResponse;
import br.com.vivabem50.model.AiInteraction;
import br.com.vivabem50.model.enums.AiUseCase;
import br.com.vivabem50.repository.AiInteractionRepository;
import br.com.vivabem50.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Assistente do MVP baseado em regras sobre a agenda do dia. Ainda não usa um LLM:
 * a integração com o modelo generativo entra na Release 2, por trás deste mesmo endpoint.
 */
@Service
public class AiChatService {

    private final AgendaService agendaService;
    private final AiInteractionRepository aiInteractionRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public AiChatService(AgendaService agendaService, AiInteractionRepository aiInteractionRepository,
                         UserRepository userRepository, UserMapper userMapper) {
        this.agendaService = agendaService;
        this.aiInteractionRepository = aiInteractionRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional
    public ChatResponse reply(Long userId, String message) {
        DayAgendaResponse day = agendaService.day(userId, userMapper.today());
        String reply = buildReply(message.toLowerCase(Locale.ROOT), day);

        AiInteraction interaction = new AiInteraction();
        interaction.setUser(userRepository.getReferenceById(userId));
        interaction.setUseCase(AiUseCase.ANALISE_AGENDA);
        interaction.setModelName("regras-mvp");
        interaction.setResponseValid(true);
        aiInteractionRepository.save(interaction);

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
            String consumed = day.wellbeing().waterLiters().stripTrailingZeros().toPlainString().replace('.', ',');
            String goal = day.wellbeing().waterGoalLiters().stripTrailingZeros().toPlainString().replace('.', ',');
            return "Hoje você está com " + consumed + " de " + goal
                    + " litros de água. Um pouco de água agora já ajuda.";
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
