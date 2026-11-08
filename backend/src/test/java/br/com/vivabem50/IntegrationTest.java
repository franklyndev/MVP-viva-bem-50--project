package br.com.vivabem50;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integração: sobe a aplicação inteira com um PostgreSQL descartável (Testcontainers).
 * Sem Docker aberto, os testes são ignorados.
 */
@SpringBootTest(properties = "app.jwt.secret=chave-usada-apenas-nos-testes-automatizados-0123456789")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTest {

    protected static final String PASSWORD = "senha-segura-123";

    @Autowired
    protected MockMvc mockMvc;

    protected static String uniqueEmail() {
        return "teste-" + UUID.randomUUID() + "@example.com";
    }

    protected static String registerBody(String email) {
        return """
                {"name": "Maria Souza", "email": "%s", "password": "%s",
                 "birthDate": "1960-05-10", "dataConsent": true}
                """.formatted(email, PASSWORD);
    }

    /** Cria um usuário novo e devolve o token dele. */
    protected String registerAndGetToken() throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(uniqueEmail())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
