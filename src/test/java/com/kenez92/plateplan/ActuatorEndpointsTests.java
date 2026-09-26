package com.kenez92.plateplan;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorEndpointsTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void allActuatorEndpointsArePublic() throws Exception {
		String index = mockMvc.perform(get("/actuator"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(index).contains(
				"/actuator/beans",
				"/actuator/health",
				"/actuator/heapdump",
				"/actuator/info",
				"/actuator/shutdown");

		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/beans")).andExpect(status().isOk());
	}

}
