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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scores")
public class ScoreController {

  private final ScoreRepository scoreRepository;
  private final RacerRegistrationRepository racerRegistrationRepository;

  public ScoreController(ScoreRepository scoreRepository, RacerRegistrationRepository racerRegistrationRepository) {
    this.scoreRepository = scoreRepository;
    this.racerRegistrationRepository = racerRegistrationRepository;
  }

  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody CreateScoreRequest request, Authentication auth) {
    User judge = (User) auth.getPrincipal();
    RacerRegistration racer = racerRegistrationRepository.findById(request.racerRegistrationId()).orElse(null);
    if (racer == null) {
      return ResponseEntity.badRequest().body(new ErrorResponse("Racer registration not found"));
    }

    Score score = new Score(racer, judge, request.runNumber(), request.points(), request.note(), Instant.now());
    scoreRepository.save(score);

    return ResponseEntity.ok(new ScoreResponse(score.getId(), score.getRunNumber(), score.getPoints(), score.getNote()));
  }

  public record CreateScoreRequest(
      @NotNull Long racerRegistrationId,
      @NotNull @Min(1) @Max(99) Integer runNumber,
      @NotNull Integer points,
      String note) {}

  public record ScoreResponse(Long id, Integer runNumber, Integer points, String note) {}
  public record ErrorResponse(String message) {}
}
