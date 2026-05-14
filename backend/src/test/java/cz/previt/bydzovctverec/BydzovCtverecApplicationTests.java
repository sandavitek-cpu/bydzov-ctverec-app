package cz.previt.bydzovctverec;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.Role;
import cz.previt.bydzovctverec.domain.ScoreRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BydzovCtverecApplicationTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private EditionRepository editionRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private RacerRegistrationRepository racerRegistrationRepository;
  @Autowired private ScoreRepository scoreRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  void seed() {
    scoreRepository.deleteAll();
    racerRegistrationRepository.deleteAll();
    editionRepository.deleteAll();
    userRepository.deleteAll();
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

  @Test
  void adminCanLogin() throws Exception {
    userRepository.save(new User("admin@test.cz", passwordEncoder.encode("pass"), Role.ADMIN, "Admin", Instant.now()));
    mockMvc
        .perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"admin@test.cz\",\"password\":\"pass\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.role").value("ADMIN"));
  }

  @Test
  void loginWithWrongPasswordReturns401() throws Exception {
    userRepository.save(new User("admin@test.cz", passwordEncoder.encode("pass"), Role.ADMIN, "Admin", Instant.now()));
    mockMvc
        .perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"admin@test.cz\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void judgeCanCreateScore() throws Exception {
    User judge = userRepository.save(new User("judge@test.cz", passwordEncoder.encode("pass"), Role.JUDGE, "Judge", Instant.now()));
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElseThrow();
    RacerRegistration racer = racerRegistrationRepository.save(new RacerRegistration(edition, "John", "Doe", "john@test.cz", "Kart", Instant.now()));

    MvcResult loginResult = mockMvc
        .perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"judge@test.cz\",\"password\":\"pass\"}"))
        .andExpect(status().isOk())
        .andReturn();
    String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();

    mockMvc
        .perform(post("/api/scores")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "racerRegistrationId", racer.getId(),
                "runNumber", 1,
                "points", 100))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.points").value(100));
  }

  @Test
  void racerCanViewOwnRegistration() throws Exception {
    userRepository.save(new User("racer@test.cz", passwordEncoder.encode("pass"), Role.RACER, "Racer", Instant.now()));
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElseThrow();
    racerRegistrationRepository.save(new RacerRegistration(edition, "Fast", "Racer", "racer@test.cz", "Super car", Instant.now()));

    MvcResult loginResult = mockMvc
        .perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"racer@test.cz\",\"password\":\"pass\"}"))
        .andExpect(status().isOk())
        .andReturn();
    String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();

    mockMvc
        .perform(get("/api/racer/registration")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("Fast"))
        .andExpect(jsonPath("$.email").value("racer@test.cz"));
  }

  @Test
  void publicRegistrationIsAllowedWithoutAuth() throws Exception {
    mockMvc
        .perform(post("/api/public/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "teamName", "Tým test",
                "email", "tym@test.cz",
                "phone", "+420777123456",
                "vehicleCategory", "OSOBNI",
                "vehiclePlate", "5H1 2345",
                "vehicleYear", 1990,
                "crewCount", 2))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.teamName").value("Tým test"))
        .andExpect(jsonPath("$.startNumber").isNumber())
        .andExpect(jsonPath("$.startFee").isNumber())
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void adminCanListRegistrations() throws Exception {
    userRepository.save(new User("admin@test.cz", passwordEncoder.encode("pass"), Role.ADMIN, "Admin", Instant.now()));
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElseThrow();
    racerRegistrationRepository.save(new RacerRegistration(edition, "TeamA", "a@t.cz", "111", "OSOBNI", "ABC", 2000, 2, 1, 1000, Instant.now()));

    MvcResult loginResult = mockMvc
        .perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"admin@test.cz\",\"password\":\"pass\"}"))
        .andExpect(status().isOk())
        .andReturn();
    String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();

    mockMvc
        .perform(get("/api/admin/registrations")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].teamName").value("TeamA"))
        .andExpect(jsonPath("$[0].startNumber").value(1));
  }

  @Test
  void nonAdminCannotAccessAdminRegistrations() throws Exception {
    mockMvc
        .perform(get("/api/admin/registrations"))
        .andExpect(status().isForbidden());
  }

  @Test
  void registrationWithInvalidDataReturns400() throws Exception {
    mockMvc
        .perform(post("/api/public/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"teamName\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void unauthenticatedRequestToScoresReturns403() throws Exception {
    mockMvc
        .perform(post("/api/scores")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isForbidden());
  }
}
