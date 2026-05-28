package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Checkpoint;
import cz.previt.bydzovctverec.domain.CheckpointRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.Score;
import cz.previt.bydzovctverec.domain.ScoreRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.service.EditionService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/judge")
public class JudgeController {

  private final EditionService editionService;
  private final RacerRegistrationRepository racerRegistrationRepository;
  private final CheckpointRepository checkpointRepository;
  private final ScoreRepository scoreRepository;

  public JudgeController(EditionService editionService,
      RacerRegistrationRepository racerRegistrationRepository,
      CheckpointRepository checkpointRepository,
      ScoreRepository scoreRepository) {
    this.editionService = editionService;
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.checkpointRepository = checkpointRepository;
    this.scoreRepository = scoreRepository;
  }

  @GetMapping("/overview")
  @Transactional(readOnly = true)
  public ResponseEntity<?> overview(@AuthenticationPrincipal User user) {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Žádný aktivní ročník"));
    }

    String judgeName = user.getUsername() != null ? user.getUsername() : user.getFirstName();

    List<Checkpoint> allCheckpoints = checkpointRepository.findByEditionOrderBySortOrder(edition);
    List<Checkpoint> assignedCheckpoints = allCheckpoints.stream()
        .filter(cp -> cp.getVolunteers() != null && cp.getVolunteers().stream().anyMatch(v -> v != null && judgeName.equalsIgnoreCase(v.trim())))
        .toList();

    Set<Long> assignedCheckpointIds = new HashSet<>();
    for (Checkpoint cp : assignedCheckpoints) {
      assignedCheckpointIds.add(cp.getId());
    }

    List<CheckpointData> checkpointData = assignedCheckpoints.stream()
        .map(cp -> new CheckpointData(cp.getId(), cp.getName(), cp.getSortOrder()))
        .toList();

    List<RacerRegistration> racers = racerRegistrationRepository.findByEditionOrderByStartNumber(edition).stream()
        .filter(r -> "PAID".equals(r.getStatus()))
        .toList();

    List<Score> judgeScores;
    if (!assignedCheckpointIds.isEmpty()) {
      judgeScores = scoreRepository.findByJudgeIdAndCheckpointIdIn(
          user.getId(), List.copyOf(assignedCheckpointIds));
    } else {
      judgeScores = List.of();
    }

    Set<Long> scoredRacerIds = new HashSet<>();
    for (Score s : judgeScores) {
      scoredRacerIds.add(s.getRacerRegistration().getId());
    }

    List<RacerOverviewData> racerData = new ArrayList<>();
    for (RacerRegistration r : racers) {
      boolean scored = scoredRacerIds.contains(r.getId());
      racerData.add(new RacerOverviewData(
          r.getId(), r.getStartNumber(), r.getTeamName(),
          r.getVehiclePlate(), r.getVehicleCategory(), scored));
    }

    int total = racers.size();
    int scored = (int) racerData.stream().filter(RacerOverviewData::scored).count();
    int remaining = total - scored;

    return ResponseEntity.ok(Map.of(
        "checkpoints", checkpointData,
        "racers", racerData,
        "stats", new StatsData(total, scored, remaining)));
  }

  public record CheckpointData(Long id, String name, int sortOrder) {}
  public record RacerOverviewData(Long id, Integer startNumber, String teamName, String vehiclePlate, String vehicleCategory, boolean scored) {}
  public record StatsData(int total, int scored, int remaining) {}
}
