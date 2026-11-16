package br.com.vivabem50;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

	@Test
	void corsAcceptsAnyLocalPortAndRejectsOtherSites() throws Exception {
		for (String origin : new String[]{"http://localhost:5173", "http://localhost:9100", "http://127.0.0.1:54321"}) {
			mockMvc.perform(options("/api/auth/login")
							.header(HttpHeaders.ORIGIN, origin)
							.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
							.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
					.andExpect(status().isOk())
					.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
		}
		mockMvc.perform(options("/api/auth/login")
						.header(HttpHeaders.ORIGIN, "https://site-malicioso.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isForbidden());
	}

}
