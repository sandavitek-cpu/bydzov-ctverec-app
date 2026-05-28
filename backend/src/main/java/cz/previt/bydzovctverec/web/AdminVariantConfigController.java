package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.VariantConfig;
import cz.previt.bydzovctverec.domain.VariantConfigRepository;
import cz.previt.bydzovctverec.service.EditionService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/variants")
public class AdminVariantConfigController {

  private final VariantConfigRepository variantConfigRepository;
  private final EditionService editionService;

  public AdminVariantConfigController(VariantConfigRepository variantConfigRepository, EditionService editionService) {
    this.variantConfigRepository = variantConfigRepository;
    this.editionService = editionService;
  }

  @GetMapping
  public ResponseEntity<?> list() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) {
      return ResponseEntity.ok(List.of());
    }
    List<Map<String, Object>> result = variantConfigRepository.findByEdition(edition).stream()
        .map(this::toMap)
        .toList();
    return ResponseEntity.ok(result);
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Žádný aktivní ročník"));
    }
    String variantCode = WebUtils.toString(body.get("variantCode"));
    String label = WebUtils.toString(body.get("label"));
    if (variantCode == null || variantCode.isBlank() || label == null || label.isBlank()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("variantCode a label jsou povinné"));
    }
    VariantConfig vc = new VariantConfig(edition, variantCode, label);
    vc.setRegistrationDeadline(parseDate(body.get("registrationDeadline")));
    vc.setRaceDate(parseDate(body.get("raceDate")));
    vc.setEnabled(body.containsKey("enabled") ? Boolean.TRUE.equals(body.get("enabled")) : true);
    variantConfigRepository.save(vc);
    return ResponseEntity.ok(toMap(vc));
  }

  @PutMapping("/{id}")
  @Transactional
  public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    VariantConfig vc = variantConfigRepository.findById(id).orElse(null);
    if (vc == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Varianta nenalezena"));
    }
    if (body.containsKey("variantCode")) vc.setVariantCode(WebUtils.toString(body.get("variantCode")));
    if (body.containsKey("label")) vc.setLabel(WebUtils.toString(body.get("label")));
    if (body.containsKey("registrationDeadline")) vc.setRegistrationDeadline(parseDate(body.get("registrationDeadline")));
    if (body.containsKey("raceDate")) vc.setRaceDate(parseDate(body.get("raceDate")));
    if (body.containsKey("enabled")) vc.setEnabled(Boolean.TRUE.equals(body.get("enabled")));
    variantConfigRepository.save(vc);
    return ResponseEntity.ok(toMap(vc));
  }

  @PostMapping("/{id}/reopen")
  @Transactional
  public ResponseEntity<?> reopen(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    VariantConfig vc = variantConfigRepository.findById(id).orElse(null);
    if (vc == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Varianta nenalezena"));
    }
    Instant until;
    if (Boolean.TRUE.equals(body.get("untilMidnight"))) {
      until = LocalDate.now(ZoneId.of("Europe/Prague")).plusDays(1).atStartOfDay(ZoneId.of("Europe/Prague")).toInstant();
    } else {
      Object duration = body.get("durationMinutes");
      if (duration == null) {
        return ResponseEntity.badRequest().body(ApiResponse.error("durationMinutes nebo untilMidnight je vyžadováno"));
      }
      long minutes;
      try {
        minutes = Long.parseLong(duration.toString());
      } catch (NumberFormatException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("durationMinutes musí být číslo"));
      }
      until = Instant.now().plusSeconds(minutes * 60);
    }
    vc.setRegistrationReopenedUntil(until);
    variantConfigRepository.save(vc);
    return ResponseEntity.ok(toMap(vc));
  }

  @DeleteMapping("/{id}/reopen")
  @Transactional
  public ResponseEntity<?> closeReopen(@PathVariable Long id) {
    VariantConfig vc = variantConfigRepository.findById(id).orElse(null);
    if (vc == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Varianta nenalezena"));
    }
    vc.setRegistrationReopenedUntil(null);
    variantConfigRepository.save(vc);
    return ResponseEntity.ok(toMap(vc));
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> delete(@PathVariable Long id) {
    VariantConfig vc = variantConfigRepository.findById(id).orElse(null);
    if (vc == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Varianta nenalezena"));
    }
    variantConfigRepository.delete(vc);
    return ResponseEntity.ok(Map.of("deleted", true));
  }

  private Map<String, Object> toMap(VariantConfig vc) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", vc.getId());
    m.put("variantCode", vc.getVariantCode());
    m.put("label", vc.getLabel());
    m.put("registrationDeadline", vc.getRegistrationDeadline() != null ? vc.getRegistrationDeadline().toString() : null);
    m.put("registrationReopenedUntil", vc.getRegistrationReopenedUntil() != null ? vc.getRegistrationReopenedUntil().toString() : null);
    m.put("raceDate", vc.getRaceDate() != null ? vc.getRaceDate().toString() : null);
    m.put("enabled", vc.isEnabled());
    return m;
  }

  private LocalDate parseDate(Object value) {
    if (value == null) return null;
    String s = value.toString();
    if (s.isBlank()) return null;
    try { return LocalDate.parse(s); } catch (Exception e) { return null; }
  }
}
