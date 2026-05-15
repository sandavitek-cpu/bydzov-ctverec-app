package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.ArchiveEntry;
import cz.previt.bydzovctverec.domain.ArchiveEntryRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/archive")
public class PublicArchiveController {

  private final ArchiveEntryRepository archiveEntryRepository;

  public PublicArchiveController(ArchiveEntryRepository archiveEntryRepository) {
    this.archiveEntryRepository = archiveEntryRepository;
  }

  @GetMapping
  public ResponseEntity<?> list(
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String vehicle) {
    List<ArchiveEntry> entries;
    if (year != null) {
      entries = archiveEntryRepository.findByEditionYearOrderByRankAsc(year);
    } else {
      entries = archiveEntryRepository.findAllByOrderByEditionYearDescRankAsc();
    }

    var stream = entries.stream();
    if (name != null && !name.isBlank()) {
      stream = stream.filter(e -> e.getRacerName().toLowerCase().contains(name.toLowerCase()));
    }
    if (vehicle != null && !vehicle.isBlank()) {
      stream = stream.filter(e -> e.getVehicle() != null
          && e.getVehicle().toLowerCase().contains(vehicle.toLowerCase()));
    }

    List<ArchiveRow> rows = stream
        .map(e -> new ArchiveRow(e.getEditionYear(), e.getRank(),
            e.getRacerName(), e.getVehicle(), e.getPoints()))
        .toList();

    return ResponseEntity.ok(Map.of("results", rows));
  }

  @GetMapping("/{year}")
  public ResponseEntity<?> byYear(@PathVariable Integer year) {
    List<ArchiveEntry> entries = archiveEntryRepository.findByEditionYearOrderByRankAsc(year);
    List<ArchiveRow> rows = entries.stream()
        .map(e -> new ArchiveRow(e.getEditionYear(), e.getRank(),
            e.getRacerName(), e.getVehicle(), e.getPoints()))
        .toList();
    return ResponseEntity.ok(Map.of("year", year, "results", rows));
  }

  public record ArchiveRow(int editionYear, int rank, String racerName, String vehicle, int points) {}
}
