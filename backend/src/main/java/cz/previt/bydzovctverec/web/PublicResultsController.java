package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.Score;
import cz.previt.bydzovctverec.domain.ScoreRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/results")
public class PublicResultsController {

  private final ScoreRepository scoreRepository;
  private final EditionRepository editionRepository;

  public PublicResultsController(ScoreRepository scoreRepository, EditionRepository editionRepository) {
    this.scoreRepository = scoreRepository;
    this.editionRepository = editionRepository;
  }

  @GetMapping("/{year}")
  public ResponseEntity<?> results(@PathVariable Integer year) {
    if (editionRepository.findByEditionYear(year).isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Ročník nenalezen"));
    }

    List<Score> scores = scoreRepository.findByEditionYearWithRacer(year);

    Map<RacerRegistration, List<Score>> grouped = scores.stream()
        .collect(Collectors.groupingBy(Score::getRacerRegistration));

    List<ResultRow> rows = new ArrayList<>();
    for (var entry : grouped.entrySet()) {
      RacerRegistration r = entry.getKey();
      List<Score> racerScores = entry.getValue();
      int total = racerScores.stream().mapToInt(Score::getPoints).sum();
      List<RunScore> runs = racerScores.stream()
          .map(s -> new RunScore(s.getRunNumber(), s.getPoints()))
          .toList();
      rows.add(new ResultRow(0, r.getStartNumber(), r.getTeamName(),
          r.getVehicleCategory(), r.getVehiclePlate(), total, runs));
    }

    rows.sort(Comparator.comparingInt(ResultRow::totalPoints).reversed());

    List<ResultRow> ranked = new ArrayList<>();
    for (int i = 0; i < rows.size(); i++) {
      ResultRow rr = rows.get(i);
      ranked.add(new ResultRow(i + 1, rr.startNumber(), rr.teamName(),
          rr.vehicleCategory(), rr.vehiclePlate(), rr.totalPoints(), rr.runs()));
    }

    return ResponseEntity.ok(Map.of("year", year, "results", ranked));
  }

  public record ResultRow(int rank, Integer startNumber, String teamName,
      String vehicleCategory, String vehiclePlate, int totalPoints, List<RunScore> runs) {}

  public record RunScore(int runNumber, int points) {}
}
