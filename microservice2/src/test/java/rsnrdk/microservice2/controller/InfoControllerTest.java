package rsnrdk.microservice2.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void info_returnsServiceStatus() throws Exception {
        mockMvc.perform(get("/api/v1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("microservice2"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void hello_withoutName_greetsWorld() throws Exception {
        mockMvc.perform(get("/api/v1/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, world!"));
    }

    @Test
    void hello_withName_greetsByName() throws Exception {
        mockMvc.perform(get("/api/v1/hello").param("name", "Arsenii"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, Arsenii!"));
    }

    @Test
    void unknownRoute_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/private"))
                .andExpect(status().isUnauthorized());
    }
}
