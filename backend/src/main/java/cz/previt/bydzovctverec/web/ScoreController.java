package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.Score;
import cz.previt.bydzovctverec.domain.ScoreRepository;
import cz.previt.bydzovctverec.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

  private final ScoreRepository scoreRepository;
  private final RacerRegistrationRepository racerRegistrationRepository;
  private final SimpMessagingTemplate messagingTemplate;

  public ScoreController(ScoreRepository scoreRepository, RacerRegistrationRepository racerRegistrationRepository, SimpMessagingTemplate messagingTemplate) {
    this.scoreRepository = scoreRepository;
    this.racerRegistrationRepository = racerRegistrationRepository;
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

    Score score = new Score(racer, judge, request.runNumber(), request.points(), request.note(), Instant.now());
    scoreRepository.save(score);

    broadcastResults(racer.getEdition().getEditionYear());

    return ResponseEntity.ok(new ScoreResponse(score.getId(), score.getRunNumber(), score.getPoints(), score.getNote()));
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
          .map(s -> new PublicResultsController.RunScore(s.getRunNumber(), s.getPoints()))
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
      @NotNull @Min(1) @Max(99) Integer runNumber,
      @NotNull Integer points,
      String note) {}

  public record ScoreResponse(Long id, Integer runNumber, Integer points, String note) {}
  public record ErrorResponse(String message) {}
}
