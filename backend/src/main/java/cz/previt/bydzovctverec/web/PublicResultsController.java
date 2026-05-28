package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.service.RankingService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/results")
public class PublicResultsController {

  private final RankingService rankingService;

  public PublicResultsController(RankingService rankingService) {
    this.rankingService = rankingService;
  }

  @GetMapping("/{year}")
  public ResponseEntity<?> getResults(@PathVariable Integer year) {
    List<RankingService.RankedRow> ranked = rankingService.computeRanking(year);
    List<PublicRankedRow> sanitized = ranked.stream()
        .map(r -> new PublicRankedRow(r.rank(), r.startNumber(), r.teamName(),
            r.vehicleCategory(), r.totalPoints(), r.runs()))
        .toList();
    return ResponseEntity.ok(java.util.Map.of("year", year, "results", sanitized));
  }

  public record PublicRankedRow(int rank, Integer startNumber, String teamName,
      String vehicleCategory, int totalPoints,
      List<RankingService.RunScore> runs) {}
}
