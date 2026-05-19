package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.service.CeremonyService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/ceremony")
public class PublicCeremonyController {

  private final CeremonyService ceremonyService;

  public PublicCeremonyController(CeremonyService ceremonyService) {
    this.ceremonyService = ceremonyService;
  }

  @GetMapping("/{year}")
  public ResponseEntity<?> ceremony(@PathVariable Integer year) {
    var data = ceremonyService.generateCeremonyData(year);
    return ResponseEntity.ok(data);
  }
}
