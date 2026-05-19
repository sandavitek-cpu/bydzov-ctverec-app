package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.AppRoleRepository;
import cz.previt.bydzovctverec.domain.Checkpoint;
import cz.previt.bydzovctverec.domain.CheckpointRepository;
import cz.previt.bydzovctverec.domain.Notification;
import cz.previt.bydzovctverec.domain.NotificationRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.Score;
import cz.previt.bydzovctverec.domain.ScoreRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scores")
public class ScoreController {

  private static final Logger log = LoggerFactory.getLogger(ScoreController.class);

  private final ScoreRepository scoreRepository;
  private final RacerRegistrationRepository racerRegistrationRepository;
  private final CheckpointRepository checkpointRepository;
  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final AppRoleRepository appRoleRepository;
  private final SimpMessagingTemplate messagingTemplate;

  public ScoreController(ScoreRepository scoreRepository,
      RacerRegistrationRepository racerRegistrationRepository,
      CheckpointRepository checkpointRepository,
      NotificationRepository notificationRepository,
      UserRepository userRepository,
      AppRoleRepository appRoleRepository,
      SimpMessagingTemplate messagingTemplate) {
    this.scoreRepository = scoreRepository;
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.checkpointRepository = checkpointRepository;
    this.notificationRepository = notificationRepository;
    this.userRepository = userRepository;
    this.appRoleRepository = appRoleRepository;
    this.messagingTemplate = messagingTemplate;
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> create(@Valid @RequestBody CreateScoreRequest request, Authentication auth) {
    User judge = (User) auth.getPrincipal();
    RacerRegistration racer = racerRegistrationRepository.findById(request.racerRegistrationId()).orElse(null);
    if (racer == null) {
      return ResponseEntity.badRequest().body(new ErrorResponse("Racer registration not found"));
    }
    Checkpoint cp = checkpointRepository.findById(request.checkpointId()).orElse(null);
    if (cp == null) {
      return ResponseEntity.badRequest().body(new ErrorResponse("Checkpoint not found"));
    }

    Score score = new Score(racer, judge, cp, request.points(), request.note(), Instant.now());
    scoreRepository.save(score);

    checkCheckpointCompletion(cp, racer.getEdition().getId());

    broadcastResults(racer.getEdition().getEditionYear());

    return ResponseEntity.ok(new ScoreResponse(score.getId(), cp.getName(), cp.getSortOrder(), score.getPoints(), score.getNote()));
  }

  private void checkCheckpointCompletion(Checkpoint cp, Long editionId) {
    List<RacerRegistration> activeRacers = racerRegistrationRepository.findByEditionOrderByStartNumber(
        cp.getEdition()).stream()
        .filter(r -> "PAID".equals(r.getStatus()))
        .toList();

    List<Score> existingScores = scoreRepository.findByCheckpointId(cp.getId());
    Set<Long> scoredRacerIds = existingScores.stream()
        .map(s -> s.getRacerRegistration().getId())
        .collect(Collectors.toSet());

    boolean allScored = activeRacers.stream().allMatch(r -> scoredRacerIds.contains(r.getId()));

    if (allScored && !activeRacers.isEmpty()) {
      var adminRole = appRoleRepository.findByName("ADMIN");
      if (adminRole.isPresent()) {
        Long adminRoleId = adminRole.get().getId();
        List<User> admins = userRepository.findAll().stream()
            .filter(u -> u.getAppRoles().stream().anyMatch(r -> r.getId().equals(adminRoleId)))
            .toList();
        List<Notification> notifications = new ArrayList<>();
        for (User admin : admins) {
          notifications.add(new Notification(admin,
              "Stanoviště kompletní",
              "Kontrola " + cp.getName() + " je kompletní – všechny posádky obodovány.",
              "SUCCESS",
              "/admin/bodovani"));
        }
        if (!notifications.isEmpty()) {
          notificationRepository.saveAll(notifications);
          log.info("Checkpoint {} fully scored – notified {} admins", cp.getName(), admins.size());
        }
      }
    }
  }

  private void broadcastResults(Integer year) {
    List<Score> scores = scoreRepository.findByEditionYearWithRacer(year);
    Map<RacerRegistration, List<Score>> grouped = scores.stream()
        .collect(Collectors.groupingBy(Score::getRacerRegistration));

    List<PublicResultsController.ResultRow> rows = new ArrayList<>();
    for (var entry : grouped.entrySet()) {
      RacerRegistration r = entry.getKey();
      List<Score> racerScores = entry.getValue();
      int total = racerScores.stream().mapToInt(Score::getPoints).sum();
      List<PublicResultsController.RunScore> runs = racerScores.stream()
          .map(s -> new PublicResultsController.RunScore(s.getCheckpoint().getSortOrder(), s.getCheckpoint().getName(), s.getPoints()))
          .toList();
      rows.add(new PublicResultsController.ResultRow(0, r.getStartNumber(), r.getTeamName(),
          r.getVehicleCategory(), r.getVehiclePlate(), total, runs));
    }

    rows.sort(Comparator.comparingInt(PublicResultsController.ResultRow::totalPoints).reversed());

    List<PublicResultsController.ResultRow> ranked = new ArrayList<>();
    for (int i = 0; i < rows.size(); i++) {
      PublicResultsController.ResultRow rr = rows.get(i);
      ranked.add(new PublicResultsController.ResultRow(i + 1, rr.startNumber(), rr.teamName(),
          rr.vehicleCategory(), rr.vehiclePlate(), rr.totalPoints(), rr.runs()));
    }

    messagingTemplate.convertAndSend("/topic/results", Map.of("year", year, "results", ranked));
  }

  public record CreateScoreRequest(
      @NotNull Long racerRegistrationId,
      @NotNull Long checkpointId,
      @NotNull Integer points,
      String note) {}

  public record ScoreResponse(Long id, String checkpointName, Integer checkpointOrder, Integer points, String note) {}
  public record ErrorResponse(String message) {}
}
