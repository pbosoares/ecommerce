package com.pablo.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class EcommerceApplicationTests {
	@Autowired MockMvc mvc;

	@Test
	void contextLoads() {
	}

	@Test
	void healthCheckPodeSerConsultadoSemLogin() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void lojaNormalInformaQueNaoEumaDemoSemLogin() throws Exception {
		mvc.perform(get("/loja/config"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.demo").value(false));
	}

}
