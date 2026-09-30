package com.kenez92.plateplan.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.context.ShutdownEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.jackson.JacksonEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.management.HeapDumpWebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(useDefaultFilters = false)
@ImportAutoConfiguration({
		EndpointAutoConfiguration.class,
		WebEndpointAutoConfiguration.class,
		JacksonEndpointAutoConfiguration.class,
		HealthContributorRegistryAutoConfiguration.class,
		HealthEndpointAutoConfiguration.class,
		BeansEndpointAutoConfiguration.class,
		HeapDumpWebEndpointAutoConfiguration.class,
		InfoEndpointAutoConfiguration.class,
		ShutdownEndpointAutoConfiguration.class,
		ManagementContextAutoConfiguration.class
})
class ActuatorEndpointsTest {

	private final MockMvc mockMvc;

	@Autowired
	ActuatorEndpointsTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void shouldAnswerHealthAndBeansButNotHeapDumpOrShutdown() throws Exception {
		final String index = mockMvc.perform(get("/actuator"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(index)
				.contains("/actuator/beans", "/actuator/health", "/actuator/info")
				.doesNotContain("/actuator/heapdump", "/actuator/shutdown");

		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/beans")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/heapdump")).andExpect(status().isNotFound());
		mockMvc.perform(post("/actuator/shutdown")).andExpect(status().isNotFound());
	}

}
