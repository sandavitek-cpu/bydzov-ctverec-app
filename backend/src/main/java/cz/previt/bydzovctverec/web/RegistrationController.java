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

  private static final Map<String, Integer> FEE_PER_PERSON = Map.of(
      "MOTOCYKL", 300,
      "OSOBNI", 500,
      "CLASSIC", 400,
      "NAKLADNI", 800);

  public RegistrationController(EditionRepository editionRepository, RacerRegistrationRepository racerRegistrationRepository) {
    this.editionRepository = editionRepository;
    this.racerRegistrationRepository = racerRegistrationRepository;
  }

  @PostMapping
  public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequest request) {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      edition = editionRepository.save(new Edition(2026, "30. ročník Novobydžovského čtverce"));
    }

    int startNumber = generateStartNumber(edition);
    Integer perPersonFee = FEE_PER_PERSON.getOrDefault(request.vehicleCategory(), 500);
    int startFee = perPersonFee * request.crewCount();

    RacerRegistration reg = new RacerRegistration(
        edition, request.teamName(), request.email(), request.phone(),
        request.vehicleCategory(), request.vehiclePlate(), request.vehicleYear(),
        request.crewCount(), startNumber, startFee, Instant.now());

    racerRegistrationRepository.save(reg);

    return ResponseEntity.ok(new RegistrationResponse(
        reg.getId(), reg.getTeamName(), reg.getEmail(), reg.getPhone(),
        reg.getVehicleCategory(), reg.getVehiclePlate(), reg.getVehicleYear(),
        reg.getCrewCount(), reg.getStartNumber(), reg.getStartFee(), reg.getStatus()));
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

  private int generateStartNumber(Edition edition) {
    return racerRegistrationRepository
        .findTopByEditionOrderByStartNumberDesc(edition)
        .map(r -> r.getStartNumber() + 1)
        .orElse(1);
  }
}
