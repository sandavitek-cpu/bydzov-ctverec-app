package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.service.RankingService;
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
    var ranked = rankingService.computeRanking(year);
    return ResponseEntity.ok(java.util.Map.of("year", year, "results", ranked));
  }
}
