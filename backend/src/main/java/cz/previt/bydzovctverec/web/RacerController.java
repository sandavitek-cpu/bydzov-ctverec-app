package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.ScheduleItem;
import cz.previt.bydzovctverec.domain.ScheduleItemRepository;
import cz.previt.bydzovctverec.domain.Score;
import cz.previt.bydzovctverec.domain.ScoreRepository;
import cz.previt.bydzovctverec.domain.User;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
  private final EditionRepository editionRepository;
  private final ScheduleItemRepository scheduleItemRepository;

  public RacerController(RacerRegistrationRepository racerRegistrationRepository, ScoreRepository scoreRepository, EditionRepository editionRepository, ScheduleItemRepository scheduleItemRepository) {
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.scoreRepository = scoreRepository;
    this.editionRepository = editionRepository;
    this.scheduleItemRepository = scheduleItemRepository;
  }

  @GetMapping("/registration")
  public ResponseEntity<?> myRegistration(Authentication auth) {
    User user = (User) auth.getPrincipal();
    RacerRegistration reg = racerRegistrationRepository.findByEmail(user.getEmail()).orElse(null);
    if (reg == null) {
      return ResponseEntity.ok(new RacerRegistrationResponse(null, null, null, null, null));
    }
    return ResponseEntity.ok(new RacerRegistrationResponse(
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

  @GetMapping("/schedule")
  public ResponseEntity<?> mySchedule() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      return ResponseEntity.ok(List.of());
    }
    List<ScheduleItem> items = scheduleItemRepository.findByEditionOrderBySortOrder(edition);
    return ResponseEntity.ok(items.stream().map(i -> new ScheduleItemResponse(i.getTime(), i.getLabel(), i.getDescription())).toList());
  }

  @GetMapping("/standing")
  public ResponseEntity<?> myStanding(Authentication auth) {
    User user = (User) auth.getPrincipal();
    RacerRegistration reg = racerRegistrationRepository.findByEmail(user.getEmail()).orElse(null);
    if (reg == null) {
      return ResponseEntity.ok(Map.of("error", "Nejste přihlášen k závodu"));
    }

    List<Score> myScores = scoreRepository.findByRacerRegistrationIdOrderByRunNumber(reg.getId());
    int totalPoints = myScores.stream().mapToInt(Score::getPoints).sum();

    Edition edition = reg.getEdition();
    List<Score> allScores = scoreRepository.findByEditionYearWithRacer(edition.getEditionYear());

    Map<Long, Integer> totals = allScores.stream()
        .collect(Collectors.groupingBy(
            s -> s.getRacerRegistration().getId(),
            Collectors.summingInt(Score::getPoints)));

    List<Map.Entry<Long, Integer>> sorted = totals.entrySet().stream()
        .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
        .toList();

    int rank = 1;
    for (var entry : sorted) {
      if (entry.getKey().equals(reg.getId())) break;
      rank++;
    }

    List<ScoreResponse> scoreResponses = myScores.stream()
        .map(s -> new ScoreResponse(s.getId(), s.getRunNumber(), s.getPoints(), s.getNote()))
        .toList();

    return ResponseEntity.ok(new StandingResponse(
        reg.getTeamName(), reg.getStartNumber(),
        totalPoints, rank, totals.size(), scoreResponses));
  }

  public record RacerRegistrationResponse(Long id, String firstName, String lastName, String email, String vehicleDescription) {}
  public record ScoreResponse(Long id, Integer runNumber, Integer points, String note) {}
  public record ScheduleItemResponse(String time, String label, String description) {}
  public record StandingResponse(String teamName, Integer startNumber, int totalPoints, int rank, int totalRacers, List<ScoreResponse> scores) {}
}
