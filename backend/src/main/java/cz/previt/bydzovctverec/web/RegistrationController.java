package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/registrations")
public class RegistrationController {

  private final EditionRepository editionRepository;
  private final RacerRegistrationRepository racerRegistrationRepository;

  private record FeeConfig(int baseDo1945, int baseOd1946, int extraPerson) {}

  private static final Map<String, FeeConfig> FEE = Map.of(
      "JEDNODENNI", new FeeConfig(500, 800, 500),
      "DVODENNI_UZAVRENO", new FeeConfig(1000, 1200, 1000),
      "DVODENNI_BEZ_UBYTOVANI", new FeeConfig(600, 900, 600));

  public RegistrationController(EditionRepository editionRepository, RacerRegistrationRepository racerRegistrationRepository) {
    this.editionRepository = editionRepository;
    this.racerRegistrationRepository = racerRegistrationRepository;
  }

  @PostMapping
  public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequest request) {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      edition = editionRepository.save(new Edition(2026, "30. ročník Novobydžovského čtverce – Memoriál Elišky Junkové"));
    }

    int startNumber = generateStartNumber(edition);
    int startFee = calculateFee(request.variant(), request.vehicleYear(), request.crewCount());

     String firstName = request.firstName() != null ? request.firstName().trim() : "";
     String lastName = request.lastName() != null ? request.lastName().trim() : "";
     RacerRegistration reg = new RacerRegistration(
         edition, request.teamName(), request.email(), request.phone(),
         request.vehicleCategory(), request.vehicleMake() != null ? request.vehicleMake() : "",
         request.vehiclePlate(), request.vehicleYear(),
         request.crewCount(), startNumber, startFee,
         request.variant(), firstName, lastName,
         request.firstTime() != null ? request.firstTime() : false,
         request.gender(), request.driverAge(),
         request.club(), request.address(),
         request.youngestAge(), request.youngestName(),
         request.engineDisplacement(), request.power(), request.maxSpeed(),
         request.vehicleNotes(), request.notes(),
         false, false, false,
         request.consent() != null ? request.consent() : false,
         false,
         Instant.now());

    racerRegistrationRepository.save(reg);

    return ResponseEntity.ok(new RegistrationResponse(
        reg.getId(), reg.getTeamName(), reg.getEmail(), reg.getPhone(),
        reg.getVehicleCategory(), reg.getVehiclePlate(), reg.getVehicleYear(),
        reg.getCrewCount(), reg.getStartNumber(), reg.getStartFee(),
        reg.getStatus(), reg.getVariant()));
  }

  @GetMapping("/lookup/{startNumber}")
  public ResponseEntity<?> lookupByStartNumber(@PathVariable Integer startNumber) {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Žádný aktivní ročník"));
    }
    return racerRegistrationRepository
        .findByEditionAndStartNumber(edition, startNumber)
        .map(r -> ResponseEntity.ok(Map.of(
            "id", r.getId(),
            "teamName", r.getTeamName(),
            "startNumber", r.getStartNumber(),
            "vehicleCategory", r.getVehicleCategory(),
            "vehiclePlate", r.getVehiclePlate())))
        .orElse(ResponseEntity.notFound().build());
  }

  public static int calculateFee(String variant, int vehicleYear, int crewCount) {
    FeeConfig cfg = FEE.getOrDefault(variant, new FeeConfig(0, 0, 0));
    int baseFee = vehicleYear < 1945 ? cfg.baseDo1945 : cfg.baseOd1946;
    return baseFee + cfg.extraPerson * Math.max(0, crewCount - 1);
  }

  private int generateStartNumber(Edition edition) {
    return racerRegistrationRepository
        .findTopByEditionOrderByStartNumberDesc(edition)
        .map(r -> r.getStartNumber() + 1)
        .orElse(1);
  }
}
