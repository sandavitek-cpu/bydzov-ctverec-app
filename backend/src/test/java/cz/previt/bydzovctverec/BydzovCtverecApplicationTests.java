package cz.previt.bydzovctverec;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.previt.bydzovctverec.domain.Checkpoint;
import cz.previt.bydzovctverec.domain.CheckpointRepository;
import cz.previt.bydzovctverec.domain.CrewMember;
import cz.previt.bydzovctverec.domain.CrewMemberRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.RaceCategoryRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.UserRole;
import cz.previt.bydzovctverec.domain.ScheduleItemRepository;
import cz.previt.bydzovctverec.domain.ScoreRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import cz.previt.bydzovctverec.domain.VariantConfig;
import cz.previt.bydzovctverec.domain.VariantConfigRepository;
import java.time.Instant;
import java.util.List;
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
  @Autowired private CrewMemberRepository crewMemberRepository;
  @Autowired private ScoreRepository scoreRepository;
  @Autowired private ScheduleItemRepository scheduleItemRepository;
  @Autowired private CheckpointRepository checkpointRepository;
  @Autowired private RaceCategoryRepository raceCategoryRepository;
  @Autowired private VariantConfigRepository variantConfigRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  void seed() {
    scoreRepository.deleteAll();
    scheduleItemRepository.deleteAll();
    checkpointRepository.deleteAll();
    crewMemberRepository.deleteAll();
    racerRegistrationRepository.deleteAll();
    raceCategoryRepository.deleteAll();
    variantConfigRepository.deleteAll();
    editionRepository.deleteAll();
    userRepository.deleteAll();
    Edition edition = editionRepository.save(new Edition(2026, "30. ročník Novobydžovského čtverce"));
    variantConfigRepository.save(new VariantConfig(edition, "JEDNODENNI", "Jednodenní závod"));
    variantConfigRepository.save(new VariantConfig(edition, "DVODENNI_UZAVRENO", "Dvoudenní závod – UZAVŘENO"));
    variantConfigRepository.save(new VariantConfig(edition, "DVODENNI_BEZ_UBYTOVANI", "Dvoudenní závod bez ubytování"));
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
    userRepository.save(new User("admin@test.cz", "admin", passwordEncoder.encode("pass"), UserRole.ADMIN, "Admin", "", Instant.now()));
    mockMvc
        .perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"login\":\"admin\",\"password\":\"pass\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.role").value("ADMIN"));
  }

  @Test
  void loginWithWrongPasswordReturns401() throws Exception {
    userRepository.save(new User("admin@test.cz", "admin", passwordEncoder.encode("pass"), UserRole.ADMIN, "Admin", "", Instant.now()));
    mockMvc
        .perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"login\":\"admin\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void judgeCanCreateScore() throws Exception {
    User judge = userRepository.save(new User("judge@test.cz", "judge", passwordEncoder.encode("pass"), UserRole.JUDGE, "Judge", "", Instant.now()));
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElseThrow();
    RacerRegistration racer = racerRegistrationRepository.save(new RacerRegistration(edition, "John", "Doe", "john@test.cz", "Kart", Instant.now()));
    Checkpoint cp = checkpointRepository.save(new Checkpoint(edition, "Test CP", 50.0, 15.0, 100, 1));
    cp.setVolunteers(List.of("judge"));
    checkpointRepository.save(cp);

    MvcResult loginResult = mockMvc
        .perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"login\":\"judge\",\"password\":\"pass\"}"))
        .andExpect(status().isOk())
        .andReturn();
    String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();

    mockMvc
        .perform(post("/api/scores")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "racerRegistrationId", racer.getId(),
                "checkpointId", cp.getId(),
                "points", 100))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.points").value(100));
  }

  @Test
  void racerCanViewOwnRegistration() throws Exception {
    User user = userRepository.save(new User("racer@test.cz", "racer", passwordEncoder.encode("pass"), UserRole.RACER, "Racer", "", Instant.now()));
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElseThrow();
    RacerRegistration reg = racerRegistrationRepository.save(new RacerRegistration(edition, "Tým test", "racer@test.cz", "+420111", "OSOBNI", "ABC", 2000, 2, 42, 1000, Instant.now()));
    crewMemberRepository.save(new CrewMember(reg, user, "Racer", "Test", "racer@test.cz"));

    MvcResult loginResult = mockMvc
        .perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"login\":\"racer\",\"password\":\"pass\"}"))
        .andExpect(status().isOk())
        .andReturn();
    String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();

    mockMvc
        .perform(get("/api/racer/registration")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.teamName").value("Tým test"))
        .andExpect(jsonPath("$.startNumber").value(42))
        .andExpect(jsonPath("$.totalPoints").value(0))
        .andExpect(jsonPath("$.rank").value(1))
        .andExpect(jsonPath("$.scores").isEmpty());
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
                "crewCount", 2,
                "variant", "JEDNODENNI"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.teamName").value("Tým test"))
        .andExpect(jsonPath("$.startNumber").isNumber())
        .andExpect(jsonPath("$.startFee").isNumber())
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void adminCanListRegistrations() throws Exception {
    userRepository.save(new User("admin@test.cz", "admin", passwordEncoder.encode("pass"), UserRole.ADMIN, "Admin", "", Instant.now()));
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElseThrow();
    racerRegistrationRepository.save(new RacerRegistration(edition, "TeamA", "a@t.cz", "111", "OSOBNI", "ABC", 2000, 2, 1, 1000, Instant.now()));

    MvcResult loginResult = mockMvc
        .perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"login\":\"admin\",\"password\":\"pass\"}"))
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
