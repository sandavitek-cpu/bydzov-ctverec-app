package cz.previt.bydzovctverec.service;

import cz.previt.bydzovctverec.domain.Checkpoint;
import cz.previt.bydzovctverec.domain.CheckpointRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.RaceCategory;
import cz.previt.bydzovctverec.domain.RaceCategoryRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.Score;
import cz.previt.bydzovctverec.domain.ScoreRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CeremonyService {

  private static final Logger log = LoggerFactory.getLogger(CeremonyService.class);

  private final EditionRepository editionRepository;
  private final RaceCategoryRepository raceCategoryRepository;
  private final ScoreRepository scoreRepository;
  private final RacerRegistrationRepository racerRegistrationRepository;
  private final CheckpointRepository checkpointRepository;

  public CeremonyService(EditionRepository editionRepository,
      RaceCategoryRepository raceCategoryRepository,
      ScoreRepository scoreRepository,
      RacerRegistrationRepository racerRegistrationRepository,
      CheckpointRepository checkpointRepository) {
    this.editionRepository = editionRepository;
    this.raceCategoryRepository = raceCategoryRepository;
    this.scoreRepository = scoreRepository;
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.checkpointRepository = checkpointRepository;
  }

  @Transactional
  public int computeCategoryWinners() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) return 0;

    List<RaceCategory> cats = raceCategoryRepository.findByEditionOrderBySortOrder(edition);
    Integer year = edition.getEditionYear();

    List<Score> allScores = scoreRepository.findByEditionYearWithRacer(year);
    Map<Long, Integer> pointsByRacerId = allScores.stream()
        .collect(Collectors.groupingBy(
            s -> s.getRacerRegistration().getId(),
            Collectors.summingInt(Score::getPoints)));

    List<RacerRegistration> allRegs = racerRegistrationRepository.findByEditionId(edition.getId());

    for (RaceCategory cat : cats) {
      List<RacerRegistration> pool = allRegs;

      String variant = cat.getVariant();
      if (variant != null && !variant.isBlank()) {
        pool = pool.stream()
            .filter(r -> variant.equals(r.getVariant()))
            .toList();
      }

      String code = cat.getCode();
      if (code != null && !code.isBlank()) {
        pool = pool.stream()
            .filter(r -> code.equalsIgnoreCase(r.getVehicleCategory())
                || r.getVehicleCategory() != null
                && r.getVehicleCategory().toLowerCase().contains(code.toLowerCase()))
            .toList();
      }

      RacerRegistration winner = switch (cat.getDetermination()) {
        case "RANKING_TOP" -> pool.stream()
            .filter(r -> r.getCancelledAt() == null)
            .max(Comparator.comparingInt((RacerRegistration r) -> pointsByRacerId.getOrDefault(r.getId(), 0))
                .thenComparingLong(RacerRegistration::getId))
            .orElse(null);
        case "RANKING_LAST" -> pool.stream()
            .filter(r -> r.getCancelledAt() == null)
            .min(Comparator.comparingInt((RacerRegistration r) -> pointsByRacerId.getOrDefault(r.getId(), 0))
                .thenComparingLong(RacerRegistration::getId))
            .orElse(null);
        case "OLDEST_VEHICLE" -> pool.stream()
            .filter(r -> r.getVehicleYear() != null && r.getCancelledAt() == null)
            .min(Comparator.comparingInt(RacerRegistration::getVehicleYear)
                .thenComparingLong(RacerRegistration::getId))
            .orElse(null);
        case "YOUNGEST_DRIVER" -> pool.stream()
            .filter(r -> r.getDriverAge() != null && r.getCancelledAt() == null)
            .min(Comparator.comparingInt(RacerRegistration::getDriverAge)
                .thenComparingLong(RacerRegistration::getId))
            .orElse(null);
        case "OLDEST_DRIVER" -> pool.stream()
            .filter(r -> r.getDriverAge() != null && r.getCancelledAt() == null)
            .max(Comparator.comparingInt(RacerRegistration::getDriverAge)
                .thenComparingLong(RacerRegistration::getId))
            .orElse(null);
        default -> null;
      };

      if (winner != null) {
        cat.setWinnerRegistrationId(winner.getId());
        cat.setWinnerName(winner.getTeamName() != null ? winner.getTeamName() :
            (winner.getFirstName() + " " + winner.getLastName()).trim());
        cat.setWinnerTeam(winner.getTeamName());
        cat.setWinnerNumber(winner.getStartNumber());
        cat.setWinnerPoints(pointsByRacerId.getOrDefault(winner.getId(), 0));
      } else {
        cat.setWinnerRegistrationId(null);
        cat.setWinnerName(null);
        cat.setWinnerTeam(null);
        cat.setWinnerNumber(null);
        cat.setWinnerPoints(null);
      }
      raceCategoryRepository.save(cat);
    }
    return cats.size();
  }

  public boolean areAllCheckpointsComplete() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) return false;

    List<Checkpoint> checkpoints = checkpointRepository.findByEditionOrderBySortOrder(edition);
    if (checkpoints.isEmpty()) return false;

    List<RacerRegistration> activeRacers = racerRegistrationRepository
        .findByEditionOrderByStartNumber(edition).stream()
        .filter(r -> "PAID".equals(r.getStatus()))
        .toList();
    if (activeRacers.isEmpty()) return false;

    List<Score> allScores = scoreRepository.findByEditionYearWithRacer(edition.getEditionYear());
    Map<Long, Set<Long>> scoredPerCheckpoint = allScores.stream()
        .collect(Collectors.groupingBy(
            s -> s.getCheckpoint().getId(),
            Collectors.mapping(s -> s.getRacerRegistration().getId(), Collectors.toSet())));

    Set<Long> activeRacerIds = activeRacers.stream().map(RacerRegistration::getId).collect(Collectors.toSet());

    for (Checkpoint cp : checkpoints) {
      Set<Long> scoredIds = scoredPerCheckpoint.getOrDefault(cp.getId(), Set.of());
      if (!activeRacerIds.stream().allMatch(scoredIds::contains)) {
        return false;
      }
    }
    return true;
  }

  public Map<String, Object> generateCeremonyData(Integer year) {
    Edition edition = editionRepository.findByEditionYear(year).orElse(null);
    if (edition == null) {
      return Map.of("year", year, "overall", List.of(), "categories", List.of());
    }

    List<Score> allScores = scoreRepository.findByEditionYearWithRacer(year);
    Map<RacerRegistration, List<Score>> grouped = allScores.stream()
        .collect(Collectors.groupingBy(Score::getRacerRegistration));

    List<Map<String, Object>> overallResults = new ArrayList<>();
    for (var entry : grouped.entrySet()) {
      RacerRegistration r = entry.getKey();
      if (r.getCancelledAt() != null) continue;
      List<Score> racerScores = entry.getValue();
      int totalPoints = racerScores.stream().mapToInt(Score::getPoints).sum();
      overallResults.add(Map.of(
          "startNumber", r.getStartNumber(),
          "teamName", r.getTeamName() != null ? r.getTeamName() : (r.getFirstName() + " " + r.getLastName()),
          "vehicleCategory", r.getVehicleCategory(),
          "vehiclePlate", r.getVehiclePlate(),
          "totalPoints", totalPoints));
    }

    overallResults.sort(Comparator.comparingInt((Map<String, Object> r) -> -(int) r.get("totalPoints")));
    List<Map<String, Object>> ranked = new ArrayList<>();
    for (int i = 0; i < overallResults.size(); i++) {
      Map<String, Object> row = new LinkedHashMap<>(overallResults.get(i));
      row.put("rank", i + 1);
      ranked.add(row);
    }
    List<Map<String, Object>> top3 = ranked.stream().limit(3).toList();

    List<RaceCategory> cats = raceCategoryRepository.findByEditionOrderBySortOrder(edition);
    List<Map<String, Object>> categoryList = cats.stream().map(cat -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", cat.getId());
      m.put("name", cat.getName());
      m.put("sortOrder", cat.getSortOrder());
      m.put("winnerName", cat.getWinnerName());
      m.put("winnerTeam", cat.getWinnerTeam());
      m.put("winnerNumber", cat.getWinnerNumber());
      m.put("winnerPoints", cat.getWinnerPoints());
      return m;
    }).toList();

    return Map.of("year", year, "overall", top3, "categories", categoryList);
  }
}
