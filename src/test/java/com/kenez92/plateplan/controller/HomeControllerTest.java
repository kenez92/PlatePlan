package com.kenez92.plateplan.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import com.kenez92.plateplan.config.SecurityConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(HomeController.class)
@Import(SecurityConfiguration.class)
class HomeControllerTest {

	private final MockMvc mockMvc;

	@Autowired
	HomeControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void shouldDescribeTheProductAndKeepLoginAtTheTop() throws Exception {
		final String html = mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(html).contains(
				"Plan na kolejny dzień, bez gotowej diety.",
				"Co to jest PlatePlan",
				"Jak powstaje plan",
				"Kalorie i preferencje",
				"listę zakupów");
		assertThat(html).contains("href=\"/register\"", "name=\"username\"", "name=\"password\"");
		assertThat(html.indexOf("class=\"login\"")).isLessThan(html.indexOf("id=\"tresc\""));
	}

}
