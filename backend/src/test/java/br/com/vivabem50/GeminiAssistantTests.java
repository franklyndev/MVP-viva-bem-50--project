package br.com.vivabem50;

import br.com.vivabem50.dto.ai.ChatTurn;
import br.com.vivabem50.service.UserMapper;
import br.com.vivabem50.service.ai.AssistantTools;
import br.com.vivabem50.service.ai.GeminiAssistant;
import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Ciclo de function calling do Gemini contra um servidor falso que responde no formato da API
 * generateContent. Não usa a internet nem uma chave real.
 */
class GeminiAssistantTests extends IntegrationTest {

    private static final String TOMORROW = LocalDate.now(ZoneId.of("America/Maceio")).plusDays(1).toString();

    @Autowired
    private AssistantTools tools;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private HttpServer server;
    private final Queue<String> responses = new ConcurrentLinkedQueue<>();
    private final List<String> requests = new ArrayList<>();
    private final List<String> apiKeys = new ArrayList<>();

    @BeforeEach
    void startFakeGemini() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            synchronized (requests) {
                requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                apiKeys.add(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            }
            byte[] body = responses.remove().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopFakeGemini() {
        server.stop(0);
    }

    private GeminiAssistant assistant() {
        return new GeminiAssistant(tools, userMapper, objectMapper, "chave-de-teste", "gemini-teste",
                "http://127.0.0.1:" + server.getAddress().getPort());
    }

    private Long newUserId() throws Exception {
        String token = registerAndGetToken();
        String body = mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void runsTheFunctionTheModelAsksForAndReturnsTheFinalText() throws Exception {
        Long userId = newUserId();
        responses.add("""
                {"candidates": [{"content": {"role": "model", "parts": [
                  {"functionCall": {"name": "criar_compromisso", "args": {
                    "tipo": "OUTRO", "titulo": "Tomar remédio", "data": "%s", "hora": "08:00"}}}
                ]}, "finishReason": "STOP"}]}
                """.formatted(TOMORROW));
        responses.add("""
                {"candidates": [{"content": {"role": "model", "parts": [
                  {"text": "Pronto! Criei o lembrete para amanhã às 8h."}
                ]}, "finishReason": "STOP"}]}
                """);

        GeminiAssistant.Reply reply = assistant().reply(userId, "Maria Souza", "sim, pode marcar",
                List.of(new ChatTurn("assistant", "Olá!"),
                        new ChatTurn("user", "Me lembra do remédio amanhã às 8h"),
                        new ChatTurn("assistant", "Posso criar o lembrete para amanhã às 8h?")));

        assertThat(reply.text()).isEqualTo("Pronto! Criei o lembrete para amanhã às 8h.");
        assertThat(reply.changedData()).isTrue();
        assertThat(reply.refused()).isFalse();
        assertThat(apiKeys).containsOnly("chave-de-teste");

        // 1ª chamada: histórico sem a saudação inicial, com papéis user/model, e as funções declaradas.
        String first = requests.get(0);
        assertThat((List<String>) JsonPath.read(first, "$.contents[*].role")).containsExactly("user", "model", "user");
        assertThat((String) JsonPath.read(first, "$.contents[2].parts[0].text")).isEqualTo("sim, pode marcar");
        assertThat((List<String>) JsonPath.read(first, "$.tools[0].functionDeclarations[*].name")).hasSize(6);
        assertThat((String) JsonPath.read(first, "$.systemInstruction.parts[1].text")).contains("Maria Souza", TOMORROW.substring(0, 4));

        // 2ª chamada: a fala do modelo volta como veio, seguida da resposta da função.
        String second = requests.get(1);
        assertThat((String) JsonPath.read(second, "$.contents[3].parts[0].functionCall.name")).isEqualTo("criar_compromisso");
        assertThat((String) JsonPath.read(second, "$.contents[4].parts[0].functionResponse.name")).isEqualTo("criar_compromisso");
        assertThat((String) JsonPath.read(second, "$.contents[4].parts[0].functionResponse.response.result.hora")).isEqualTo("08:00");

        // O lembrete foi criado de verdade, na agenda dessa pessoa.
        assertThat(tools.execute(userId, "listar_compromissos", java.util.Map.of("data_inicio", TOMORROW, "data_fim", TOMORROW))
                .content()).contains("Tomar remédio");
    }

    @Test
    void sendsToolErrorsBackToTheModel() throws Exception {
        Long userId = newUserId();
        responses.add("""
                {"candidates": [{"content": {"role": "model", "parts": [
                  {"functionCall": {"name": "excluir_compromisso", "args": {"id": 999999}}}
                ]}}]}
                """);
        responses.add("""
                {"candidates": [{"content": {"role": "model", "parts": [
                  {"text": "Não encontrei esse compromisso."}]}}]}
                """);

        GeminiAssistant.Reply reply = assistant().reply(userId, "Maria Souza", "apaga a consulta", List.of());

        assertThat(reply.text()).isEqualTo("Não encontrei esse compromisso.");
        assertThat(reply.changedData()).isFalse();
        assertThat((String) JsonPath.read(requests.get(1), "$.contents[2].parts[0].functionResponse.response.error"))
                .isEqualTo("Compromisso não encontrado.");
    }

    @Test
    void blockedAnswersBecomeAFriendlyRefusal() throws Exception {
        Long userId = newUserId();
        responses.add("{\"promptFeedback\": {\"blockReason\": \"SAFETY\"}}");

        GeminiAssistant.Reply reply = assistant().reply(userId, "Maria Souza", "...", List.of());

        assertThat(reply.refused()).isTrue();
        assertThat(reply.text()).startsWith("Desculpe, não posso ajudar");
    }
}
