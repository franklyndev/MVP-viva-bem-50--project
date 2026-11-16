package br.com.vivabem50;

import br.com.vivabem50.service.ai.AssistantTools;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ferramentas da assistente com IA, executadas sem chamar o LLM. */
class AssistantToolsTests extends IntegrationTest {

    private static final String TOMORROW = LocalDate.now(ZoneId.of("America/Maceio")).plusDays(1).toString();

    @Autowired
    private AssistantTools tools;

    private Long newUserId() throws Exception {
        String token = registerAndGetToken();
        String body = mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static Map<String, Object> reminder(String time) {
        Map<String, Object> input = new HashMap<>();
        input.put("tipo", "OUTRO");
        input.put("titulo", "Tomar remédio da pressão");
        input.put("data", TOMORROW);
        input.put("hora", time);
        return input;
    }

    @Test
    void definesSixToolsWithRequiredFields() {
        assertThat(tools.definitions()).extracting(tool -> tool.get("name")).containsExactly(
                "consultar_agenda", "listar_compromissos", "criar_compromisso",
                "excluir_compromisso", "marcar_como_feito", "registrar_bem_estar");
    }

    @Test
    void createsListsCompletesAndDeletesAReminder() throws Exception {
        Long userId = newUserId();

        AssistantTools.Outcome created = tools.execute(userId, "criar_compromisso", reminder("08:00"));
        assertThat(created.error()).isFalse();
        assertThat(created.changedData()).isTrue();
        assertThat((String) JsonPath.read(created.content(), "$.hora")).isEqualTo("08:00");
        assertThat((String) JsonPath.read(created.content(), "$.data")).isEqualTo(TOMORROW);
        long id = ((Number) JsonPath.read(created.content(), "$.id")).longValue();

        AssistantTools.Outcome listed = tools.execute(userId, "listar_compromissos",
                Map.of("data_inicio", TOMORROW, "data_fim", TOMORROW));
        assertThat((List<String>) JsonPath.read(listed.content(), "$[*].titulo")).containsExactly("Tomar remédio da pressão");

        AssistantTools.Outcome day = tools.execute(userId, "consultar_agenda", Map.of("data", TOMORROW));
        assertThat((List<String>) JsonPath.read(day.content(), "$.itens[*].time")).containsExactly("08:00");

        AssistantTools.Outcome done = tools.execute(userId, "marcar_como_feito",
                Map.of("origem", "APPOINTMENT", "id", id, "data", TOMORROW, "feito", true));
        assertThat(done.error()).isFalse();

        AssistantTools.Outcome deleted = tools.execute(userId, "excluir_compromisso", Map.of("id", id));
        assertThat(deleted.error()).isFalse();
        AssistantTools.Outcome empty = tools.execute(userId, "listar_compromissos",
                Map.of("data_inicio", TOMORROW, "data_fim", TOMORROW));
        assertThat(empty.content()).isEqualTo("[]");
    }

    @Test
    void cannotTouchAnotherUsersAppointment() throws Exception {
        Long owner = newUserId();
        Long other = newUserId();
        AssistantTools.Outcome created = tools.execute(owner, "criar_compromisso", reminder("10:00"));
        long id = ((Number) JsonPath.read(created.content(), "$.id")).longValue();

        AssistantTools.Outcome attempt = tools.execute(other, "excluir_compromisso", Map.of("id", id));
        assertThat(attempt.error()).isTrue();
        assertThat(attempt.content()).isEqualTo("Compromisso não encontrado.");
        assertThat(tools.execute(owner, "listar_compromissos",
                Map.of("data_inicio", TOMORROW, "data_fim", TOMORROW)).content()).contains("Tomar remédio");
    }

    @Test
    void returnsReadableErrorsForInvalidInput() throws Exception {
        Long userId = newUserId();

        assertThat(tools.execute(userId, "criar_compromisso", reminder("8h")).content())
                .isEqualTo("Use a hora no formato HH:mm.");
        Map<String, Object> longTitle = reminder("09:00");
        longTitle.put("titulo", "x".repeat(121));
        assertThat(tools.execute(userId, "criar_compromisso", longTitle).content())
                .isEqualTo("O título pode ter no máximo 120 caracteres.");
        assertThat(tools.execute(userId, "registrar_bem_estar", Map.of("data", TOMORROW, "agua_litros", 50)).error())
                .isTrue();
        assertThat(tools.execute(userId, "listar_compromissos",
                Map.of("data_inicio", "2026-01-01", "data_fim", "2026-12-31")).content())
                .isEqualTo("Consulte no máximo 62 dias de cada vez.");
        assertThat(tools.execute(userId, "apagar_tudo", Map.of()).error()).isTrue();
    }

    @Test
    void recordsWellbeing() throws Exception {
        Long userId = newUserId();
        AssistantTools.Outcome outcome = tools.execute(userId, "registrar_bem_estar",
                Map.of("data", TOMORROW, "agua_litros", 1.5, "humor", "bem"));
        assertThat(outcome.error()).isFalse();
        assertThat(((Number) JsonPath.read(outcome.content(), "$.waterLiters")).doubleValue()).isEqualTo(1.5);
        assertThat((String) JsonPath.read(outcome.content(), "$.mood")).isEqualTo("BEM");
    }

    @Test
    void chatWithoutApiKeyUsesRulesAndAcceptsHistory() throws Exception {
        String token = registerAndGetToken();
        mockMvc.perform(post("/api/ai/chat").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "sim, pode marcar",
                                 "history": [{"role": "assistant", "content": "Olá! Como posso ajudar?"},
                                             {"role": "user", "content": "Quero uma consulta amanhã"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").isString())
                .andExpect(jsonPath("$.agendaChanged").value(false));

        mockMvc.perform(post("/api/ai/chat").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"oi\", \"history\": [{\"role\": \"sistema\", \"content\": \"x\"}]}"))
                .andExpect(status().isBadRequest());
    }

}
