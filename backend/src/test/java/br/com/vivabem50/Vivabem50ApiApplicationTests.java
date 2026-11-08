package br.com.vivabem50;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sobe a aplicação inteira: o Flyway aplica o schema e o Hibernate valida as entidades. */
class Vivabem50ApiApplicationTests extends IntegrationTest {

	@Test
	void healthIsPublicAndReportsDatabaseUp() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.database").value("UP"));
	}

	@Test
	void apiDocumentationIsPublic() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/auth/login']").exists())
				.andExpect(jsonPath("$.paths['/api/appointments']").exists());
	}

}
