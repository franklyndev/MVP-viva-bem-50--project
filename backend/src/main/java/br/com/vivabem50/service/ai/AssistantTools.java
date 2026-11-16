package br.com.vivabem50.service.ai;

import br.com.vivabem50.dto.agenda.CompleteItemRequest;
import br.com.vivabem50.dto.agenda.DayAgendaResponse;
import br.com.vivabem50.dto.appointment.AppointmentRequest;
import br.com.vivabem50.dto.appointment.AppointmentResponse;
import br.com.vivabem50.dto.wellbeing.WellbeingUpdateRequest;
import br.com.vivabem50.exception.BusinessRuleException;
import br.com.vivabem50.exception.NotFoundException;
import br.com.vivabem50.model.enums.AppointmentType;
import br.com.vivabem50.model.enums.Mood;
import br.com.vivabem50.service.AgendaService;
import br.com.vivabem50.service.AppointmentService;
import br.com.vivabem50.service.UserMapper;
import br.com.vivabem50.service.WellbeingService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ferramentas que a assistente pode usar. Cada uma chama um serviço que já existe,
 * sempre com o id do usuário logado, então a IA só enxerga e altera os dados dessa pessoa.
 */
@Component
public class AssistantTools {

    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_PERIOD_DAYS = 62;

    /** Resultado de uma ferramenta: o texto (JSON) devolvido à IA e se foi um erro. */
    public record Outcome(String content, boolean error, boolean changedData) {
    }

    private final AgendaService agendaService;
    private final AppointmentService appointmentService;
    private final WellbeingService wellbeingService;
    private final UserMapper userMapper;
    private final Validator validator;
    private final ObjectMapper objectMapper;

    public AssistantTools(AgendaService agendaService, AppointmentService appointmentService,
                          WellbeingService wellbeingService, UserMapper userMapper,
                          Validator validator, ObjectMapper objectMapper) {
        this.agendaService = agendaService;
        this.appointmentService = appointmentService;
        this.wellbeingService = wellbeingService;
        this.userMapper = userMapper;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    /** Declarações no formato functionDeclarations da API do Gemini (schema OpenAPI). */
    public List<Map<String, Object>> definitions() {
        return List.of(
                tool("consultar_agenda",
                        "Mostra a agenda de um dia: compromissos (consultas, exames, lembretes) e atividades, "
                                + "com horário local, se já foram feitos, e o bem-estar do dia (água, atividade, humor). "
                                + "Use para responder o que a pessoa tem em um dia e para achar o item antes de concluir ou excluir.",
                        Map.of("data", dateProp("Dia no formato AAAA-MM-DD.")),
                        List.of("data")),
                tool("listar_compromissos",
                        "Lista consultas, exames e lembretes de um período (até 62 dias), com id, tipo, título, data e hora local. "
                                + "Use para perguntas como 'quais lembretes eu tenho esta semana' ou 'quando é minha próxima consulta'.",
                        Map.of("data_inicio", dateProp("Primeiro dia do período, AAAA-MM-DD."),
                                "data_fim", dateProp("Último dia do período (incluído), AAAA-MM-DD.")),
                        List.of("data_inicio", "data_fim")),
                tool("criar_compromisso",
                        "Cria uma consulta, um exame ou um lembrete (ex.: tomar remédio) na agenda da pessoa.",
                        Map.of("tipo", enumProp("CONSULTA para médico ou dentista, EXAME para exames, OUTRO para lembretes.",
                                        List.of("CONSULTA", "EXAME", "OUTRO")),
                                "titulo", stringProp("Título curto, ex.: 'Consulta com cardiologista' ou 'Tomar remédio da pressão'."),
                                "data", dateProp("Dia no formato AAAA-MM-DD."),
                                "hora", stringProp("Horário local no formato HH:mm, ex.: 09:30."),
                                "local", stringProp("Local, se a pessoa disser."),
                                "observacoes", stringProp("Observações, se a pessoa disser."),
                                "duracao_minutos", intProp("Duração em minutos (1 a 1440), se a pessoa disser.")),
                        List.of("tipo", "titulo", "data", "hora")),
                tool("excluir_compromisso",
                        "Exclui uma consulta, um exame ou um lembrete pelo id. Atividades não podem ser excluídas.",
                        Map.of("id", intProp("id do compromisso (sourceId dos itens com source APPOINTMENT).")),
                        List.of("id")),
                tool("marcar_como_feito",
                        "Marca ou desmarca um item da agenda como feito em um dia.",
                        Map.of("origem", enumProp("source do item na agenda.", List.of("APPOINTMENT", "ACTIVITY")),
                                "id", intProp("sourceId do item na agenda."),
                                "data", dateProp("Dia do item, AAAA-MM-DD."),
                                "feito", Map.of("type", "BOOLEAN", "description", "true para marcar como feito, false para desmarcar.")),
                        List.of("origem", "id", "data", "feito")),
                tool("registrar_bem_estar",
                        "Atualiza o bem-estar de um dia. Envie só o que a pessoa informou. Os valores substituem o total do dia: "
                                + "se ela disser que bebeu mais um copo, consulte a agenda do dia e some 0,25 litro ao valor atual.",
                        Map.of("data", dateProp("Dia no formato AAAA-MM-DD."),
                                "agua_litros", Map.of("type", "NUMBER",
                                        "description", "Total de água no dia, em litros (0 a 20). Um copo tem cerca de 0,25 litro."),
                                "minutos_atividade", intProp("Total de minutos de atividade física no dia (0 a 600)."),
                                "humor", enumProp("Como a pessoa está se sentindo.", List.of("OTIMO", "BEM", "NORMAL", "TRISTE"))),
                        List.of("data")));
    }

    public Outcome execute(Long userId, String name, Map<String, Object> input) {
        try {
            return switch (name) {
                case "consultar_agenda" -> ok(dayAgenda(userId, date(input, "data")), false);
                case "listar_compromissos" -> ok(listAppointments(userId, input), false);
                case "criar_compromisso" -> ok(createAppointment(userId, input), true);
                case "excluir_compromisso" -> {
                    appointmentService.delete(userId, longValue(input, "id"));
                    yield ok(Map.of("excluido", true), true);
                }
                case "marcar_como_feito" -> {
                    agendaService.complete(userId, new CompleteItemRequest(
                            text(input, "origem"), longValue(input, "id"), date(input, "data"),
                            Boolean.TRUE.equals(input.get("feito"))));
                    yield ok(Map.of("atualizado", true), true);
                }
                case "registrar_bem_estar" -> ok(updateWellbeing(userId, input), true);
                default -> error("Ferramenta desconhecida: " + name);
            };
        } catch (InvalidInput | BusinessRuleException | NotFoundException e) {
            return error(e.getMessage());
        }
    }

    private Map<String, Object> dayAgenda(Long userId, LocalDate date) {
        DayAgendaResponse day = agendaService.day(userId, date);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", day.date());
        result.put("itens", day.items());
        result.put("bem_estar", day.wellbeing());
        return result;
    }

    private List<Map<String, Object>> listAppointments(Long userId, Map<String, Object> input) {
        LocalDate start = date(input, "data_inicio");
        LocalDate end = date(input, "data_fim");
        if (end.isBefore(start)) {
            throw new InvalidInput("data_fim deve ser igual ou posterior a data_inicio.");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_PERIOD_DAYS) {
            throw new InvalidInput("Consulte no máximo " + MAX_PERIOD_DAYS + " dias de cada vez.");
        }
        var zone = userMapper.zoneId();
        return appointmentService.list(userId, start.atStartOfDay(zone).toOffsetDateTime(),
                        end.plusDays(1).atStartOfDay(zone).toOffsetDateTime())
                .stream().map(this::describe).toList();
    }

    private Map<String, Object> createAppointment(Long userId, Map<String, Object> input) {
        AppointmentType type = enumValue(AppointmentType.class, input, "tipo");
        LocalDate date = date(input, "data");
        LocalTime time;
        try {
            time = LocalTime.parse(text(input, "hora"), HOUR);
        } catch (DateTimeParseException e) {
            throw new InvalidInput("Use a hora no formato HH:mm.");
        }
        Integer duration = input.get("duracao_minutos") instanceof Number n ? n.intValue() : null;
        var request = new AppointmentRequest(type, text(input, "titulo"), optionalText(input, "local"),
                optionalText(input, "observacoes"), AgendaService.at(date, time, userMapper.zoneId()), duration);
        validate(request);
        return describe(appointmentService.create(userId, request));
    }

    private Object updateWellbeing(Long userId, Map<String, Object> input) {
        BigDecimal liters = input.get("agua_litros") instanceof Number n ? new BigDecimal(n.toString()) : null;
        Integer minutes = input.get("minutos_atividade") instanceof Number n ? n.intValue() : null;
        Mood mood = input.get("humor") == null ? null : enumValue(Mood.class, input, "humor");
        var request = new WellbeingUpdateRequest(date(input, "data"), liters, null, minutes, null, mood);
        validate(request);
        return wellbeingService.update(userId, request);
    }

    private Map<String, Object> describe(AppointmentResponse appointment) {
        var local = appointment.startsAt().atZoneSameInstant(userMapper.zoneId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", appointment.id());
        result.put("tipo", appointment.type());
        result.put("titulo", appointment.title());
        result.put("data", local.toLocalDate());
        result.put("hora", local.toLocalTime().format(HOUR));
        result.put("local", appointment.location());
        result.put("observacoes", appointment.notes());
        result.put("status", appointment.status());
        return result;
    }

    private void validate(Object request) {
        Set<ConstraintViolation<Object>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new InvalidInput(violations.stream().map(ConstraintViolation::getMessage)
                    .sorted().collect(Collectors.joining(" ")));
        }
    }

    private Outcome ok(Object value, boolean changedData) {
        return new Outcome(objectMapper.writeValueAsString(value), false, changedData);
    }

    private static Outcome error(String message) {
        return new Outcome(message, true, false);
    }

    private static LocalDate date(Map<String, Object> input, String key) {
        try {
            return LocalDate.parse(text(input, key));
        } catch (DateTimeParseException e) {
            throw new InvalidInput("Use " + key + " no formato AAAA-MM-DD.");
        }
    }

    private static String text(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new InvalidInput("Informe " + key + ".");
        }
        return value.toString().trim();
    }

    private static String optionalText(Map<String, Object> input, String key) {
        Object value = input.get(key);
        return value == null || value.toString().isBlank() ? null : value.toString().trim();
    }

    private static long longValue(Map<String, Object> input, String key) {
        if (input.get(key) instanceof Number n) {
            return n.longValue();
        }
        throw new InvalidInput("Informe " + key + " como número.");
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, Map<String, Object> input, String key) {
        try {
            return Enum.valueOf(type, text(input, key).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidInput("Valor inválido para " + key + ".");
        }
    }

    private static Map<String, Object> tool(String name, String description, Map<String, Object> properties,
                                            List<String> required) {
        return Map.of("name", name, "description", description,
                "parameters", Map.of("type", "OBJECT", "properties", properties, "required", required));
    }

    private static Map<String, Object> stringProp(String description) {
        return Map.of("type", "STRING", "description", description);
    }

    private static Map<String, Object> intProp(String description) {
        return Map.of("type", "INTEGER", "description", description);
    }

    private static Map<String, Object> dateProp(String description) {
        return Map.of("type", "STRING", "description", description);
    }

    private static Map<String, Object> enumProp(String description, List<String> values) {
        return Map.of("type", "STRING", "format", "enum", "enum", values, "description", description);
    }

    /** Entrada da IA que não passou na validação; volta para ela como erro da ferramenta. */
    private static class InvalidInput extends RuntimeException {
        InvalidInput(String message) {
            super(message);
        }
    }
}
