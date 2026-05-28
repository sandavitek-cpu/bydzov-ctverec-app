package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.config.EmailService;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.UserRepository;
import cz.previt.bydzovctverec.domain.VariantConfigRepository;
import cz.previt.bydzovctverec.service.EditionService;
import cz.previt.bydzovctverec.service.RegistrationService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/registrations")
public class RegistrationController {

  private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

  private final EditionRepository editionRepository;
  private final EditionService editionService;
  private final RacerRegistrationRepository racerRegistrationRepository;
  private final VariantConfigRepository variantConfigRepository;
  private final RegistrationService registrationService;
  private final EmailService emailService;

  public RegistrationController(EditionRepository editionRepository,
      EditionService editionService,
      RacerRegistrationRepository racerRegistrationRepository,
      VariantConfigRepository variantConfigRepository,
      RegistrationService registrationService,
      EmailService emailService) {
    this.editionRepository = editionRepository;
    this.editionService = editionService;
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.variantConfigRepository = variantConfigRepository;
    this.registrationService = registrationService;
    this.emailService = emailService;
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequest request) {
    Edition edition = editionService.getOrCreateCurrentEdition();

    var variant = variantConfigRepository.findByEditionAndVariantCode(edition, request.variant()).orElse(null);
    if (variant == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Vybraná varianta závodu neexistuje"));
    }
    if (!variant.isEnabled()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Přihlášky pro tuto variantu jsou uzavřeny"));
    }
    LocalDate today = LocalDate.now(ZoneId.of("Europe/Prague"));
    if (variant.getRegistrationDeadline() != null && today.isAfter(variant.getRegistrationDeadline())) {
      if (variant.getRegistrationReopenedUntil() == null || Instant.now().isAfter(variant.getRegistrationReopenedUntil())) {
        return ResponseEntity.badRequest().body(ApiResponse.error(
            "Uzávěrka přihlášek pro tuto variantu již proběhla (" + variant.getRegistrationDeadline() + ")"));
      }
    }

    int startFee = registrationService.calculateFee(request.variant(), request.vehicleYear(), request.crewCount());

    String firstName = request.firstName() != null ? request.firstName().trim() : "";
    String lastName = request.lastName() != null ? request.lastName().trim() : "";
    String email = request.email() != null ? request.email().trim() : "";

    java.util.Set<String> allEmails = new java.util.HashSet<>();
    allEmails.add(email);
    if (request.crewMembers() != null) {
      for (var cm : request.crewMembers()) {
        String cmEmail = cm.email().trim().toLowerCase();
        if (!allEmails.add(cmEmail)) {
          return ResponseEntity.badRequest().body(ApiResponse.error(
              "Email " + cm.email() + " je již použit v této přihlášce. Každý účastník musí mít vlastní email."));
        }
      }
    }

    RacerRegistration reg = new RacerRegistration(
        edition, request.teamName(), email, request.phone(),
        request.vehicleCategory(), request.vehicleMake() != null ? request.vehicleMake() : "",
        request.vehiclePlate(), request.vehicleYear(),
        request.crewCount(), 0, startFee,
        request.variant(), firstName, lastName,
        request.firstTime() != null ? request.firstTime() : false,
        request.gender(), request.driverAge(),
        request.club(), request.address(),
        request.youngestAge(), request.youngestName(),
        request.engineDisplacement(), request.power(), request.maxSpeed(),
        request.vehicleNotes(), request.vehicleStory(), request.notes(),
        false, false, false,
        request.consent() != null ? request.consent() : false,
        true, Instant.now());

    racerRegistrationRepository.save(reg);

    int paymentRef = registrationService.generatePaymentReference(edition);
    reg.setPaymentReference(paymentRef);
    racerRegistrationRepository.save(reg);

    var driverResult = registrationService.createUser(email, firstName, lastName, reg,
        request.driverAge(), request.gender(), request.address(),
        request.club(), request.firstTime());
    emailService.sendCredentials(email, firstName + " " + lastName, email,
        driverResult.rawPassword(), paymentRef, startFee);

    if (request.crewMembers() != null) {
      for (var cm : request.crewMembers()) {
        String cmEmail = cm.email().trim();
        String cmClub = Boolean.TRUE.equals(cm.clubMember())
            ? (cm.clubName() != null && !cm.clubName().isBlank() ? cm.clubName().trim() : "ano")
            : "";
        var crewResult = registrationService.createUser(cmEmail, cm.firstName().trim(), cm.lastName().trim(), reg,
            cm.driverAge(), cm.gender(), cm.address(), cmClub, cm.firstTime());
        emailService.sendCredentials(cmEmail, cm.firstName() + " " + cm.lastName(), cmEmail,
            crewResult.rawPassword(), paymentRef, startFee);
      }
    }

    log.info("Registration {} created with {} crew members", reg.getId(),
        1 + (request.crewMembers() != null ? request.crewMembers().size() : 0));

    String summary = buildSummaryEmail(request, startFee, paymentRef);
    emailService.send(email, "Novobydžovský čtverec – potvrzení přihlášky", summary);

    return ResponseEntity.ok(new RegistrationResponse(
        reg.getId(), reg.getTeamName(), email, reg.getPhone(),
        reg.getVehicleCategory(), reg.getVehiclePlate(), reg.getVehicleYear(),
        reg.getCrewCount(), reg.getStartNumber(), reg.getStartFee(),
        reg.getStatus(), reg.getVariant(), paymentRef));
  }

  private String buildSummaryEmail(RegistrationRequest r, int startFee, int paymentRef) {
    var sb = new StringBuilder();
    sb.append("Dobrý den,\n\n");
    sb.append("děkujeme za Vaši přihlášku na Novobydžovský čtverec 2026.\n\n");
    sb.append("Rekapitulace přihlášky:\n");
    sb.append("  Název týmu: ").append(r.teamName()).append("\n");
    String variantLabel = switch (r.variant()) {
      case "JEDNODENNI" -> "Jednodenní závod";
      case "DVODENNI_UZAVRENO" -> "Dvoudenní závod – UZAVŘENO";
      case "DVODENNI_BEZ_UBYTOVANI" -> "Dvoudenní závod bez ubytování";
      default -> r.variant();
    };
    sb.append("  Varianta: ").append(variantLabel).append("\n");
    String catLabel = "AUTO".equals(r.vehicleCategory()) ? "Automobil" : "Motocykl";
    sb.append("  Vozidlo: ").append(catLabel);
    if (r.vehicleMake() != null && !r.vehicleMake().isBlank()) {
      sb.append(" ").append(r.vehicleMake());
    }
    sb.append(" (").append(r.vehiclePlate()).append(", ").append(r.vehicleYear()).append(")\n");
    sb.append("  Počet členů posádky: ").append(r.crewCount()).append("\n\n");

    sb.append("Řidič:\n");
    sb.append("  Jméno: ").append(r.firstName()).append(" ").append(r.lastName()).append("\n");
    if (r.driverAge() != null) sb.append("  Věk: ").append(r.driverAge()).append(" let\n");
    if (r.gender() != null) sb.append("  Pohlaví: ").append("M".equals(r.gender()) ? "Muž" : "Žena").append("\n");
    if (r.address() != null && !r.address().isBlank()) sb.append("  Bydliště: ").append(r.address()).append("\n");
    if (r.club() != null && !r.club().isBlank()) sb.append("  Klub: ").append(r.club()).append("\n\n");

    if (r.crewMembers() != null && !r.crewMembers().isEmpty()) {
      sb.append("Ostatní členové posádky:\n");
      for (int i = 0; i < r.crewMembers().size(); i++) {
        var cm = r.crewMembers().get(i);
        sb.append("  ").append(i + 1).append(". ").append(cm.firstName()).append(" ").append(cm.lastName());
        sb.append(" (").append(cm.email()).append(")\n");
      }
      sb.append("\n");
    }

    sb.append("Startovné: ").append(startFee).append(" Kč\n");
    sb.append("Variabilní symbol: ").append(paymentRef).append("\n\n");
    sb.append("Po přihlášení uvidíte itinerář, mapu a stav Vaší přihlášky:\n");
    sb.append("https://app.bydzov-ctverec.cz\n\n");
    sb.append("S pozdravem\n");
    sb.append("Tým Novobydžovského čtverce\n");
    return sb.toString();
  }

  @GetMapping("/lookup-user")
  public ResponseEntity<?> lookupUserByEmail(@RequestParam String email) {
    if (email == null || email.isBlank()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Email is required"));
    }
    return ResponseEntity.ok(Map.of("found", true));
  }

  @GetMapping("/lookup/{startNumber}")
  public ResponseEntity<?> lookupByStartNumber(@PathVariable Integer startNumber) {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Žádný aktivní ročník"));
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
}
