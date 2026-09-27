package com.kenez92.plateplan.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(RegisterController.class)
class RegisterControllerTest {

	private final MockMvc mockMvc;

	@Autowired
	RegisterControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void shouldShowTheRegistrationFormBelowTheLoginBar() throws Exception {
		final String html = mockMvc.perform(get("/register"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(html).contains("Załóż konto", "Zaloguj się", "name=\"username\"");
		assertThat(html.indexOf("class=\"login\"")).isLessThan(html.indexOf("id=\"register-title\""));
	}

}
