package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Checkpoint;
import cz.previt.bydzovctverec.domain.CheckpointRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.Score;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.service.ScoringService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scores")
public class ScoreController {

  private final ScoringService scoringService;
  private final RacerRegistrationRepository racerRegistrationRepository;
  private final CheckpointRepository checkpointRepository;

  public ScoreController(ScoringService scoringService,
      RacerRegistrationRepository racerRegistrationRepository,
      CheckpointRepository checkpointRepository) {
    this.scoringService = scoringService;
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.checkpointRepository = checkpointRepository;
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> create(@Valid @RequestBody CreateScoreRequest request, Authentication auth) {
    User judge = (User) auth.getPrincipal();
    RacerRegistration racer = racerRegistrationRepository.findById(request.racerRegistrationId()).orElse(null);
    if (racer == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Racer registration not found"));
    }
    Checkpoint cp = checkpointRepository.findById(request.checkpointId()).orElse(null);
    if (cp == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Checkpoint not found"));
    }
    boolean isAdmin = auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    if (!isAdmin) {
      String judgeName = judge.getUsername() != null ? judge.getUsername() : judge.getFirstName();
      List<String> volunteers = cp.getVolunteers();
      boolean isAssigned = volunteers != null && volunteers.stream()
          .anyMatch(v -> v != null && judgeName.equalsIgnoreCase(v.trim()));
      if (!isAssigned) {
        return ResponseEntity.status(403).body(ApiResponse.error("Nejste přiřazen k tomuto stanovišti"));
      }
    }

    try {
      Score score = scoringService.createScore(racer, judge, cp, request.points(), request.note());
      return ResponseEntity.ok(new ScoreResponse(score.getId(), cp.getName(), cp.getSortOrder(), score.getPoints(), score.getNote()));
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
  }

  public record CreateScoreRequest(
      @NotNull Long racerRegistrationId,
      @NotNull Long checkpointId,
      @NotNull Integer points,
      String note) {}

  public record ScoreResponse(Long id, String checkpointName, Integer checkpointOrder, Integer points, String note) {}
}
