package br.com.vivabem50;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileTests extends IntegrationTest {

    @Test
    void profileShowsCalculatedAgeAndStartsEmpty() throws Exception {
        String token = registerAndGetToken();
        int expectedAge = Period.between(LocalDate.of(1960, 5, 10), LocalDate.now(ZoneId.of("America/Maceio"))).getYears();

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria Souza"))
                .andExpect(jsonPath("$.birthDate").value("1960-05-10"))
                .andExpect(jsonPath("$.age").value(expectedAge))
                .andExpect(jsonPath("$.goals", hasSize(0)))
                .andExpect(jsonPath("$.healthProfile", nullValue()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void updatesNameAndBirthDate() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(put("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Maria S. Lima\", \"birthDate\": \"1958-01-20\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria S. Lima"))
                .andExpect(jsonPath("$.birthDate").value("1958-01-20"));
    }

    @Test
    void rejectsBirthDateBefore1900() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(put("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Maria\", \"birthDate\": \"1850-01-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthProfileIsCreatedThenUpdated() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(put("/api/me/health-profile").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityLevel\": \"LEVE\", \"notes\": \"Prefiro atividades pela manhã\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthProfile.activityLevel").value("LEVE"));

        mockMvc.perform(put("/api/me/health-profile").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityLevel\": \"MODERADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthProfile.activityLevel").value("MODERADO"))
                .andExpect(jsonPath("$.healthProfile.notes", nullValue()));
    }

    @Test
    void rejectsUnknownActivityLevel() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(put("/api/me/health-profile").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityLevel\": \"ATLETA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void goalCatalogIsPublic() throws Exception {
        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].code").value("ATIVIDADE_FISICA"));
    }

    @Test
    void goalsAreReplacedByTheGivenCodes() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(put("/api/me/goals").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalCodes\": [\"ACOMPANHAMENTO_PREVENTIVO\", \"ATIVIDADE_FISICA\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goals", hasSize(2)))
                .andExpect(jsonPath("$.goals[0].code").value("ATIVIDADE_FISICA"));

        mockMvc.perform(put("/api/me/goals").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalCodes\": [\"ORGANIZAR_COMPROMISSOS\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goals", hasSize(1)))
                .andExpect(jsonPath("$.goals[0].code").value("ORGANIZAR_COMPROMISSOS"));
    }

    @Test
    void rejectsUnknownGoalCode() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(put("/api/me/goals").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalCodes\": [\"ATIVIDADE_FISICA\", \"CORRIDA\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Objetivo desconhecido: CORRIDA."));
    }

    @Test
    void deletingAccountRemovesAccess() throws Exception {
        String token = registerAndGetToken();
        mockMvc.perform(put("/api/me/goals").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalCodes\": [\"ATIVIDADE_FISICA\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }
}
