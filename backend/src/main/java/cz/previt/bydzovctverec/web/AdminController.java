package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/registrations")
public class AdminController {

  private final EditionRepository editionRepository;
  private final RacerRegistrationRepository racerRegistrationRepository;

  public AdminController(EditionRepository editionRepository, RacerRegistrationRepository racerRegistrationRepository) {
    this.editionRepository = editionRepository;
    this.racerRegistrationRepository = racerRegistrationRepository;
  }

  @GetMapping
  public ResponseEntity<?> list() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      return ResponseEntity.ok(List.of());
    }
    List<RacerRegistration> regs = racerRegistrationRepository.findByEditionOrderByStartNumber(edition);
    return ResponseEntity.ok(regs.stream().map(this::toResponse).toList());
  }

  @PatchMapping("/{id}/status")
  @Transactional
  public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
    String newStatus = body.get("status");
    if (newStatus == null || (!newStatus.equals("PAID") && !newStatus.equals("PENDING"))) {
      return ResponseEntity.badRequest().body(Map.of("error", "Neplatný stav (PAID / PENDING)"));
    }
    RacerRegistration reg = racerRegistrationRepository.findById(id).orElse(null);
    if (reg == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Přihláška nenalezena"));
    }
    racerRegistrationRepository.updateStatus(id, newStatus);
    return ResponseEntity.ok(Map.of("status", newStatus));
  }

  @GetMapping("/export")
  public ResponseEntity<String> exportCsv() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      return ResponseEntity.ok("Žádný aktivní ročník");
    }
    List<RacerRegistration> regs = racerRegistrationRepository.findByEditionOrderByStartNumber(edition);
    String header = "Startovní číslo,Jméno posádky,E-mail,Telefon,Kategorie,SPZ,Ročník,Počet osob,Startovné,Stav\n";
    String body = regs.stream()
        .map(r -> String.join(",",
            String.valueOf(r.getStartNumber()),
            csvEscape(r.getTeamName()),
            csvEscape(r.getEmail()),
            csvEscape(r.getPhone()),
            csvEscape(r.getVehicleCategory()),
            csvEscape(r.getVehiclePlate()),
            String.valueOf(r.getVehicleYear()),
            String.valueOf(r.getCrewCount()),
            String.valueOf(r.getStartFee()),
            statusLabel(r.getStatus())))
        .collect(Collectors.joining("\n"));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=prihlasky.csv")
        .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
        .body(header + body);
  }

  private RegistrationResponse toResponse(RacerRegistration r) {
    return new RegistrationResponse(
        r.getId(), r.getTeamName(), r.getEmail(), r.getPhone(),
        r.getVehicleCategory(), r.getVehiclePlate(), r.getVehicleYear(),
        r.getCrewCount(), r.getStartNumber(), r.getStartFee(), r.getStatus());
  }

  private String csvEscape(String s) {
    if (s == null) return "";
    if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
      return "\"" + s.replace("\"", "\"\"") + "\"";
    }
    return s;
  }

  private String statusLabel(String status) {
    if (status == null) return "Neznámý";
    return switch (status) {
      case "PAID" -> "Zaplaceno";
      case "PENDING" -> "Čeká na platbu";
      default -> status;
    };
  }
}
