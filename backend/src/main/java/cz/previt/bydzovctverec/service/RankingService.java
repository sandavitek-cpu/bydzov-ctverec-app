package cz.previt.bydzovctverec.service;

import cz.previt.bydzovctverec.domain.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RankingService {

  private final ScoreRepository scoreRepository;

  public RankingService(ScoreRepository scoreRepository) {
    this.scoreRepository = scoreRepository;
  }

  public record RankedRow(int rank, Integer startNumber, String teamName,
      String vehicleCategory, String vehiclePlate, int totalPoints,
      List<RunScore> runs) {}

  public record RunScore(Integer sortOrder, String name, int points) {}

  public List<RankedRow> computeRanking(Integer year) {
    List<Score> scores = scoreRepository.findByEditionYearWithRacer(year);
    Map<RacerRegistration, List<Score>> grouped = scores.stream()
        .collect(Collectors.groupingBy(Score::getRacerRegistration));

    List<RankedRow> rows = new ArrayList<>();
    for (var entry : grouped.entrySet()) {
      RacerRegistration r = entry.getKey();
      List<Score> racerScores = entry.getValue();
      int total = racerScores.stream().mapToInt(Score::getPoints).sum();
      List<RunScore> runs = racerScores.stream()
          .map(s -> new RunScore(s.getCheckpoint().getSortOrder(), s.getCheckpoint().getName(), s.getPoints()))
          .toList();
      rows.add(new RankedRow(0, r.getStartNumber(),
          r.getTeamName() != null ? r.getTeamName() : (r.getFirstName() + " " + r.getLastName()),
          r.getVehicleCategory(), r.getVehiclePlate(), total, runs));
    }

    rows.sort(Comparator.comparingInt(RankedRow::totalPoints).reversed());

    List<RankedRow> ranked = new ArrayList<>();
    for (int i = 0; i < rows.size(); i++) {
      RankedRow rr = rows.get(i);
      ranked.add(new RankedRow(i + 1, rr.startNumber(), rr.teamName(),
          rr.vehicleCategory(), rr.vehiclePlate(), rr.totalPoints(), rr.runs()));
    }
    return ranked;
  }

  public int computeRacerRank(Long racerRegistrationId, Integer year) {
    List<Score> allScores = scoreRepository.findByEditionYearWithRacer(year);
    Map<Long, Integer> totals = allScores.stream()
        .collect(Collectors.groupingBy(s -> s.getRacerRegistration().getId(),
            Collectors.summingInt(Score::getPoints)));
    List<Map.Entry<Long, Integer>> sorted = totals.entrySet().stream()
        .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
        .toList();
    int rank = 1;
    for (var entry : sorted) {
      if (entry.getKey().equals(racerRegistrationId)) break;
      rank++;
    }
    return rank;
  }
}
