package cz.previt.bydzovctverec.service;

import cz.previt.bydzovctverec.domain.Checkpoint;
import cz.previt.bydzovctverec.domain.CheckpointRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.Score;
import cz.previt.bydzovctverec.domain.ScoreRepository;
import cz.previt.bydzovctverec.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoringService {

  private static final Logger log = LoggerFactory.getLogger(ScoringService.class);

  private final ScoreRepository scoreRepository;
  private final RacerRegistrationRepository racerRegistrationRepository;
  private final CheckpointRepository checkpointRepository;
  private final NotificationService notificationService;
  private final RankingService rankingService;
  private final CeremonyService ceremonyService;
  private final SimpMessagingTemplate messagingTemplate;

  public ScoringService(ScoreRepository scoreRepository,
      RacerRegistrationRepository racerRegistrationRepository,
      CheckpointRepository checkpointRepository,
      NotificationService notificationService,
      RankingService rankingService,
      CeremonyService ceremonyService,
      SimpMessagingTemplate messagingTemplate) {
    this.scoreRepository = scoreRepository;
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.checkpointRepository = checkpointRepository;
    this.notificationService = notificationService;
    this.rankingService = rankingService;
    this.ceremonyService = ceremonyService;
    this.messagingTemplate = messagingTemplate;
  }

  @Transactional
  public Score createScore(RacerRegistration racer, User judge, Checkpoint checkpoint,
      Integer points, String note) {
    boolean exists = scoreRepository.findByCheckpointId(checkpoint.getId()).stream()
        .anyMatch(s -> s.getRacerRegistration().getId().equals(racer.getId()));
    if (exists) {
      throw new IllegalStateException("Posádka již byla na tomto stanovišti obodována");
    }

    Score score = new Score(racer, judge, checkpoint, points, note, Instant.now());
    scoreRepository.save(score);
    log.info("Score created: racer={}, checkpoint={}, points={}", racer.getId(), checkpoint.getId(), points);

    checkCheckpointCompletion(checkpoint, racer.getEdition().getId());
    broadcastResults(racer.getEdition().getEditionYear());

    return score;
  }

  private void checkCheckpointCompletion(Checkpoint cp, Long editionId) {
    List<RacerRegistration> activeRacers = racerRegistrationRepository
        .findByEditionOrderByStartNumber(cp.getEdition()).stream()
        .filter(r -> "PAID".equals(r.getStatus()))
        .toList();

    List<Score> existingScores = scoreRepository.findByCheckpointId(cp.getId());
    Set<Long> scoredRacerIds = existingScores.stream()
        .map(s -> s.getRacerRegistration().getId())
        .collect(Collectors.toSet());

    boolean allScored = activeRacers.stream().allMatch(r -> scoredRacerIds.contains(r.getId()));

    if (allScored && !activeRacers.isEmpty()) {
      notificationService.notifyAdmins("Stanoviště kompletní",
          "Kontrola " + cp.getName() + " je kompletní – všechny posádky obodovány.",
          "/admin/bodovani");

      if (ceremonyService.areAllCheckpointsComplete()) {
        int count = ceremonyService.computeCategoryWinners();
        notificationService.notifyAdmins("Vyhodnocení dokončeno",
            "Všechna stanoviště jsou kompletně obodována. Vyhodnoceno " + count + " kategorií.",
            "/admin/ceremonie");
      }
    }
  }

  private void broadcastResults(Integer year) {
    var ranked = rankingService.computeRanking(year);
    messagingTemplate.convertAndSend("/topic/results", java.util.Map.of("year", year, "results", ranked));
  }
}
