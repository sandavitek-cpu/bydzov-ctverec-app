package cz.previt.bydzovctverec;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BydzovCtverecApplicationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private EditionRepository editionRepository;

  @BeforeEach
  void seed() {
    editionRepository.deleteAll();
    editionRepository.save(new Edition(2026, "30. ročník Novobydžovského čtverce"));
  }

  @Test
  void contextLoads() {}

  @Test
  void actuatorHealthIsOk() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void rootIsPublicForRenderHealthProbe() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().isOk());
  }

  @Test
  void currentEditionIsPublic() throws Exception {
    mockMvc
        .perform(get("/api/public/editions/current"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.year").value(2026))
        .andExpect(jsonPath("$.label").value("30. ročník Novobydžovského čtverce"));
  }
}
