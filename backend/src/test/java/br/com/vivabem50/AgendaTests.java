package br.com.vivabem50;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Agenda do dia, bem-estar, chat e o formato de cadastro usado pelo front (web e mobile). */
class AgendaTests extends IntegrationTest {

    private static final String TODAY = LocalDate.now(ZoneId.of("America/Maceio")).toString();

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    @Test
    void registerAcceptsTheFrontEndPayload() throws Exception {
        String email = uniqueEmail();
        String body = """
                {"name": "José Santos", "email": "%s", "password": "%s",
                 "birthDate": "1958-08-01", "city": "Aracaju", "consent": true}
                """.formatted(email, PASSWORD);

        String response = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.city").value("Aracaju"))
                .andExpect(jsonPath("$.user.age", notNullValue()))
                .andExpect(jsonPath("$.user.birthDate").value("1958-08-01"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.token");

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Aracaju"))
                .andExpect(jsonPath("$.healthProfile", nullValue()));
    }

    @Test
    void registerRejectsFrontEndPayloadWithoutConsent() throws Exception {
        String body = """
                {"name": "José", "email": "%s", "password": "%s", "birthDate": "1958-08-01", "consent": false}
                """.formatted(uniqueEmail(), PASSWORD);
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dataConsent", notNullValue()));
    }

    @Test
    void demoUserLogsInAndSeesTodaysRoutine() throws Exception {
        String token = login(DEMO_EMAIL, DEMO_PASSWORD);

        mockMvc.perform(get("/api/agenda").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(TODAY))
                .andExpect(jsonPath("$.user.name").value("Maria Souza"))
                .andExpect(jsonPath("$.user.city").value("Aracaju"))
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.items[0].time").value("08:00"))
                .andExpect(jsonPath("$.items[1].source").value("ACTIVITY"))
                .andExpect(jsonPath("$.wellbeing.waterGoal").value(8));
    }

    @Test
    void demoLoginFailsWithWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"errada\"}".formatted(DEMO_EMAIL)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void appointmentCreatedTodayShowsInTimelineAndCanBeCompleted() throws Exception {
        String token = registerAndGetToken();
        String startsAt = TODAY + "T10:00:00-03:00";

        String created = mockMvc.perform(post("/api/appointments").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"CONSULTA\", \"title\": \"Clínico geral\", \"location\": \"UBS Centro\", \"startsAt\": \"%s\"}"
                                .formatted(startsAt)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/agenda").header(HttpHeaders.AUTHORIZATION, bearer(token)).param("date", TODAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].time").value("10:00"))
                .andExpect(jsonPath("$.items[0].kind").value("Consulta"))
                .andExpect(jsonPath("$.items[0].detail").value("UBS Centro"))
                .andExpect(jsonPath("$.items[0].done").value(false))
                .andExpect(jsonPath("$.nextAppointment.sourceId").value(id));

        mockMvc.perform(post("/api/agenda/complete").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\": \"APPOINTMENT\", \"sourceId\": %d, \"date\": \"%s\", \"done\": true}"
                                .formatted(id, TODAY)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/agenda").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items[0].done").value(true))
                .andExpect(jsonPath("$.nextAppointment", nullValue()));
        mockMvc.perform(get("/api/appointments/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.status").value("REALIZADO"));
    }

    @Test
    void demoActivityCanBeCompletedAndUndone() throws Exception {
        String token = login(DEMO_EMAIL, DEMO_PASSWORD);
        String agenda = mockMvc.perform(get("/api/agenda").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> activities = JsonPath.read(agenda, "$.items[?(@.source == 'ACTIVITY')]");
        int activityId = (Integer) activities.getFirst().get("sourceId");
        String payload = "{\"source\": \"ACTIVITY\", \"sourceId\": %d, \"date\": \"%s\", \"done\": %s}";

        mockMvc.perform(post("/api/agenda/complete").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(payload.formatted(activityId, TODAY, true)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/agenda").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items[?(@.source == 'ACTIVITY')].done").value(true));

        mockMvc.perform(post("/api/agenda/complete").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(payload.formatted(activityId, TODAY, false)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/agenda").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items[?(@.source == 'ACTIVITY')].done").value(false));
    }

    @Test
    void completeRejectsInvalidSourceAndSomeoneElsesItem() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(post("/api/agenda/complete").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\": \"FESTA\", \"sourceId\": 1, \"date\": \"%s\", \"done\": true}".formatted(TODAY)))
                .andExpect(status().isBadRequest());

        String demoToken = login(DEMO_EMAIL, DEMO_PASSWORD);
        String agenda = mockMvc.perform(get("/api/agenda").header(HttpHeaders.AUTHORIZATION, bearer(demoToken)))
                .andReturn().getResponse().getContentAsString();
        int demoAppointment = JsonPath.read(agenda, "$.items[0].sourceId");

        mockMvc.perform(post("/api/agenda/complete").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\": \"APPOINTMENT\", \"sourceId\": %d, \"date\": \"%s\", \"done\": true}"
                                .formatted(demoAppointment, TODAY)))
                .andExpect(status().isNotFound());
    }

    @Test
    void wellbeingStartsEmptyAndUpdatesOnlySentFields() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(get("/api/wellbeing").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(TODAY))
                .andExpect(jsonPath("$.waterCups").value(0))
                .andExpect(jsonPath("$.mood", nullValue()));

        mockMvc.perform(put("/api/wellbeing").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\": \"%s\", \"waterCups\": 3, \"mood\": \"BEM\"}".formatted(TODAY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waterCups").value(3))
                .andExpect(jsonPath("$.mood").value("BEM"));

        mockMvc.perform(put("/api/wellbeing").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\": \"%s\", \"activityMinutes\": 25}".formatted(TODAY)))
                .andExpect(jsonPath("$.waterCups").value(3))
                .andExpect(jsonPath("$.activityMinutes").value(25))
                .andExpect(jsonPath("$.mood").value("BEM"));
    }

    @Test
    void wellbeingValidatesLimits() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(put("/api/wellbeing").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\": \"%s\", \"waterCups\": 99}".formatted(TODAY)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.waterCups", notNullValue()));
    }

    @Test
    void chatAnswersWithTheDaysAgendaAndRecordsTheInteraction() throws Exception {
        String token = login(DEMO_EMAIL, DEMO_PASSWORD);

        mockMvc.perform(post("/api/ai/chat").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"Quero marcar uma consulta\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value(org.hamcrest.Matchers.startsWith("Olá, Maria! Você já tem ")));

        mockMvc.perform(post("/api/ai/chat").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void newRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/agenda")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/wellbeing")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON).content("{\"message\": \"oi\"}"))
                .andExpect(status().isUnauthorized());
    }
}
