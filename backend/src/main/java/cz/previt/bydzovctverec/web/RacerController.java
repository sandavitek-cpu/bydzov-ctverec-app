package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.Score;
import cz.previt.bydzovctverec.domain.ScoreRepository;
import cz.previt.bydzovctverec.domain.User;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/racer")
public class RacerController {

  private final RacerRegistrationRepository racerRegistrationRepository;
  private final ScoreRepository scoreRepository;

  public RacerController(RacerRegistrationRepository racerRegistrationRepository, ScoreRepository scoreRepository) {
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.scoreRepository = scoreRepository;
  }

  @GetMapping("/registration")
  public ResponseEntity<?> myRegistration(Authentication auth) {
    User user = (User) auth.getPrincipal();
    RacerRegistration reg = racerRegistrationRepository.findByEmail(user.getEmail()).orElse(null);
    if (reg == null) {
      return ResponseEntity.ok(new RegistrationResponse(null, null, null, null, null));
    }
    return ResponseEntity.ok(new RegistrationResponse(
        reg.getId(), reg.getFirstName(), reg.getLastName(),
        reg.getEmail(), reg.getVehicleDescription()));
  }

  @GetMapping("/scores")
  public ResponseEntity<?> myScores(Authentication auth) {
    User user = (User) auth.getPrincipal();
    RacerRegistration reg = racerRegistrationRepository.findByEmail(user.getEmail()).orElse(null);
    if (reg == null) {
      return ResponseEntity.ok(List.of());
    }
    List<Score> scores = scoreRepository.findByRacerRegistrationIdOrderByRunNumber(reg.getId());
    List<ScoreResponse> resp = scores.stream()
        .map(s -> new ScoreResponse(s.getId(), s.getRunNumber(), s.getPoints(), s.getNote()))
        .toList();
    return ResponseEntity.ok(resp);
  }

  public record RegistrationResponse(Long id, String firstName, String lastName, String email, String vehicleDescription) {}
  public record ScoreResponse(Long id, Integer runNumber, Integer value, String note) {}
}
