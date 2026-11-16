package br.com.vivabem50;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentTests extends IntegrationTest {

    private static String appointment(String title, String startsAt) {
        return """
                {"type": "CONSULTA", "title": "%s", "location": "Clínica Centro",
                 "startsAt": "%s", "durationMinutes": 30}
                """.formatted(title, startsAt);
    }

    private long create(String token, String body) throws Exception {
        String response = mockMvc.perform(post("/api/appointments").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    @Test
    void createsAndReadsAppointment() throws Exception {
        String token = registerAndGetToken();

        var result = mockMvc.perform(post("/api/appointments").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointment("Cardiologista", "2026-11-20T09:00:00-03:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status").value("AGENDADO"))
                .andExpect(jsonPath("$.startsAt").value("2026-11-20T12:00:00Z"))
                .andReturn();
        long id = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .endsWith("/api/appointments/" + id);

        mockMvc.perform(get("/api/appointments/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Cardiologista"))
                .andExpect(jsonPath("$.location").value("Clínica Centro"))
                .andExpect(jsonPath("$.durationMinutes").value(30));
    }

    @Test
    void listsInDateOrderAndFiltersByPeriod() throws Exception {
        String token = registerAndGetToken();
        create(token, appointment("Exame de sangue", "2026-11-24T08:00:00-03:00"));
        create(token, appointment("Clínico geral", "2026-11-23T09:00:00-03:00"));
        create(token, appointment("Dentista", "2026-12-02T14:00:00-03:00"));

        mockMvc.perform(get("/api/appointments").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title").value("Clínico geral"))
                .andExpect(jsonPath("$[2].title").value("Dentista"));

        mockMvc.perform(get("/api/appointments").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("from", "2026-11-23T00:00:00-03:00")
                        .param("to", "2026-11-30T00:00:00-03:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void rejectsInvalidPeriodFilter() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(get("/api/appointments").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("from", "2026-11-30T00:00:00-03:00"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/appointments").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("from", "2026-11-30T00:00:00-03:00")
                        .param("to", "2026-11-01T00:00:00-03:00"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/appointments").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("from", "ontem")
                        .param("to", "amanha"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.from", notNullValue()));
    }

    @Test
    void updatesAndReschedulesAppointment() throws Exception {
        String token = registerAndGetToken();
        long id = create(token, appointment("Cardiologista", "2026-11-20T09:00:00-03:00"));

        mockMvc.perform(put("/api/appointments/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "EXAME", "title": "Eletrocardiograma", "startsAt": "2026-11-21T10:30:00-03:00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("EXAME"))
                .andExpect(jsonPath("$.title").value("Eletrocardiograma"))
                .andExpect(jsonPath("$.startsAt").value("2026-11-21T13:30:00Z"))
                .andExpect(jsonPath("$.location").doesNotExist());
    }

    @Test
    void deletesAppointment() throws Exception {
        String token = registerAndGetToken();
        long id = create(token, appointment("Cardiologista", "2026-11-20T09:00:00-03:00"));

        mockMvc.perform(delete("/api/appointments/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/appointments/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotSeeOrChangeSomeoneElsesAppointment() throws Exception {
        String owner = registerAndGetToken();
        String intruder = registerAndGetToken();
        long id = create(owner, appointment("Cardiologista", "2026-11-20T09:00:00-03:00"));

        mockMvc.perform(get("/api/appointments/" + id).header(HttpHeaders.AUTHORIZATION, bearer(intruder)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/appointments/" + id).header(HttpHeaders.AUTHORIZATION, bearer(intruder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointment("Alterado", "2026-11-20T09:00:00-03:00")))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/appointments/" + id).header(HttpHeaders.AUTHORIZATION, bearer(intruder)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/appointments").header(HttpHeaders.AUTHORIZATION, bearer(intruder)))
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/appointments/" + id).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(jsonPath("$.title").value("Cardiologista"));
    }

    @Test
    void validatesRequiredFields() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(post("/api/appointments").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"  \", \"durationMinutes\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.type", notNullValue()))
                .andExpect(jsonPath("$.errors.title", notNullValue()))
                .andExpect(jsonPath("$.errors.startsAt", notNullValue()))
                .andExpect(jsonPath("$.errors.durationMinutes", notNullValue()));
    }

    @Test
    void rejectsUnknownTypeAndMissingTimezone() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(post("/api/appointments").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointment("Teste", "2026-11-20T09:00:00-03:00").replace("CONSULTA", "FESTA")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/appointments").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointment("Teste", "2026-11-20T09:00:00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/appointments")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON)
                        .content(appointment("Teste", "2026-11-20T09:00:00-03:00")))
                .andExpect(status().isUnauthorized());
    }
}
