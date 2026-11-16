package br.com.vivabem50.service.ai;

import br.com.vivabem50.dto.ai.ChatTurn;
import br.com.vivabem50.service.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Assistente com LLM (Google Gemini, API generateContent com function calling). A IA responde
 * dúvidas básicas de saúde e bem-estar e usa as ferramentas de {@link AssistantTools} para
 * consultar e alterar a agenda da pessoa. Só é usada quando GEMINI_API_KEY está configurada.
 */
@Component
public class GeminiAssistant {

    private static final Logger log = LoggerFactory.getLogger(GeminiAssistant.class);
    private static final int MAX_STEPS = 8;
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final Set<String> BLOCKED = Set.of("SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII", "RECITATION");

    private static final String INSTRUCTIONS = """
            Você é a Vivi, assistente do VivaBem 50+, um aplicativo de rotina e saúde para pessoas com 50 anos \
            ou mais, em Sergipe.

            Como falar:
            - Português do Brasil, tom acolhedor e respeitoso, frases curtas e palavras simples.
            - Respostas curtas: até 4 frases. O aplicativo mostra texto puro, então não use markdown, \
            listas com asteriscos nem emojis.
            - Diga horários como "9h30" e datas como "sexta-feira, 20/11".

            Saúde:
            - Dê orientações gerais de bem-estar: hidratação, alimentação equilibrada, atividade física leve, \
            sono, prevenção e a importância de manter consultas e exames em dia.
            - Não faça diagnóstico, não indique remédios nem doses e não mande mudar um tratamento. \
            Nesses casos, oriente a falar com um profissional de saúde.
            - Se a pessoa contar sinais de emergência, como dor no peito, falta de ar forte, desmaio, \
            fraqueza ou dormência de um lado do corpo, fala enrolada ou confusão repentina, oriente a ligar \
            imediatamente para o SAMU no 192.
            - Para assuntos sem relação com saúde, bem-estar ou a rotina no aplicativo, diga com gentileza \
            que só pode ajudar com esses temas.

            Ações no aplicativo:
            - Use as funções para consultar e alterar a agenda e o bem-estar. Nunca invente compromissos: \
            consulte antes de responder sobre eles.
            - Converta datas como "hoje", "amanhã" e "sexta" a partir da data atual informada abaixo.
            - Para criar um compromisso, você precisa do dia e do horário. Se faltar algum, pergunte. \
            Use CONSULTA para médico ou dentista, EXAME para exames e OUTRO para lembretes, como tomar remédio.
            - Antes de excluir, confirme com a pessoa qual item será excluído, dizendo o título, o dia e o \
            horário, a menos que ela já tenha confirmado esse item nesta conversa.
            - Depois de uma ação, diga em poucas palavras o que foi feito, com dia e horário.
            - Se uma função falhar, explique o problema de forma simples e diga como resolver.
            """;

    /** Resposta final da assistente. refused indica que o Gemini bloqueou o pedido. */
    public record Reply(String text, boolean refused, boolean changedData) {
    }

    private final AssistantTools tools;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final RestClient http;

    public GeminiAssistant(AssistantTools tools, UserMapper userMapper, ObjectMapper objectMapper,
                           @Value("${app.ai.api-key:}") String apiKey,
                           @Value("${app.ai.model:gemini-3.5-flash-lite}") String model,
                           @Value("${app.ai.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl) {
        this.tools = tools;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        var requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(60));
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public boolean enabled() {
        return !apiKey.isBlank();
    }

    public String model() {
        return model;
    }

    /** Lança RestClientException se a API do Gemini falhar. */
    public Reply reply(Long userId, String userName, String message, List<ChatTurn> history) {
        List<Map<String, Object>> contents = new ArrayList<>(toContents(history));
        contents.add(textContent("user", message));
        boolean changedData = false;

        for (int step = 0; step < MAX_STEPS; step++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("systemInstruction", Map.of("parts", List.of(
                    Map.of("text", INSTRUCTIONS), Map.of("text", context(userName)))));
            body.put("contents", contents);
            body.put("tools", List.of(Map.of("functionDeclarations", tools.definitions())));
            body.put("generationConfig", Map.of("maxOutputTokens", 2048));

            Map<?, ?> response = http.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("promptFeedback") instanceof Map<?, ?> feedback
                    && feedback.get("blockReason") != null) {
                return refused(changedData);
            }
            List<?> candidates = response.get("candidates") instanceof List<?> list ? list : List.of();
            if (candidates.isEmpty() || !(candidates.getFirst() instanceof Map<?, ?> candidate)) {
                return refused(changedData);
            }
            if (BLOCKED.contains(String.valueOf(candidate.get("finishReason")))) {
                return refused(changedData);
            }
            Map<?, ?> content = candidate.get("content") instanceof Map<?, ?> c ? c : Map.of();
            List<?> parts = content.get("parts") instanceof List<?> p ? p : List.of();

            List<Map<?, ?>> calls = parts.stream()
                    .filter(part -> part instanceof Map<?, ?> m && m.get("functionCall") instanceof Map)
                    .<Map<?, ?>>map(part -> (Map<?, ?>) ((Map<?, ?>) part).get("functionCall"))
                    .toList();
            if (calls.isEmpty()) {
                return new Reply(text(parts), false, changedData);
            }

            // Devolve a fala do modelo como veio (inclui assinaturas de raciocínio, se houver).
            contents.add(Map.of("role", "model", "parts", parts));
            List<Map<String, Object>> results = new ArrayList<>();
            for (Map<?, ?> call : calls) {
                String name = String.valueOf(call.get("name"));
                @SuppressWarnings("unchecked")
                Map<String, Object> args = call.get("args") instanceof Map<?, ?> a ? (Map<String, Object>) a : Map.of();
                AssistantTools.Outcome outcome = tools.execute(userId, name, args);
                changedData |= outcome.changedData();
                log.debug("Função {} executada; erro={}", name, outcome.error());
                Object payload = outcome.error()
                        ? Map.of("error", outcome.content())
                        : Map.of("result", objectMapper.readValue(outcome.content(), Object.class));
                Map<String, Object> functionResponse = new LinkedHashMap<>();
                if (call.get("id") != null) {
                    functionResponse.put("id", call.get("id"));
                }
                functionResponse.put("name", name);
                functionResponse.put("response", payload);
                results.add(Map.of("functionResponse", functionResponse));
            }
            contents.add(Map.of("role", "user", "parts", results));
        }
        return new Reply("Desculpe, não consegui concluir esse pedido. Pode tentar de novo com outras palavras?",
                false, changedData);
    }

    private static Reply refused(boolean changedData) {
        return new Reply("Desculpe, não posso ajudar com esse pedido. Posso ajudar com sua agenda "
                + "ou com dicas de bem-estar.", true, changedData);
    }

    private String context(String userName) {
        ZonedDateTime now = ZonedDateTime.now(userMapper.zoneId());
        String weekday = now.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR);
        return "Data atual: " + weekday + ", " + now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " (" + now.toLocalDate() + "), " + now.format(DateTimeFormatter.ofPattern("HH:mm"))
                + " no horário de Sergipe. A pessoa se chama " + userName + ".";
    }

    /** O histórico precisa começar com a pessoa; a saudação inicial da Vivi é descartada. */
    private static List<Map<String, Object>> toContents(List<ChatTurn> history) {
        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatTurn turn : history) {
            boolean user = "user".equals(turn.role());
            if (contents.isEmpty() && !user) {
                continue;
            }
            contents.add(textContent(user ? "user" : "model", turn.content()));
        }
        return contents;
    }

    private static Map<String, Object> textContent(String role, String text) {
        return Map.of("role", role, "parts", List.of(Map.of("text", text)));
    }

    /** Junta os textos da resposta, ignorando partes de raciocínio (thought). */
    private static String text(List<?> parts) {
        String text = parts.stream()
                .filter(part -> part instanceof Map<?, ?> m && m.get("text") != null
                        && !Boolean.TRUE.equals(m.get("thought")))
                .map(part -> String.valueOf(((Map<?, ?>) part).get("text")).trim())
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining("\n\n"));
        return text.isEmpty() ? "Pronto." : text;
    }
}
