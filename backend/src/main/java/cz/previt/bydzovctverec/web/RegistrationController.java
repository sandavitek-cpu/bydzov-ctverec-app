package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.config.EmailService;
import cz.previt.bydzovctverec.domain.AppRole;
import cz.previt.bydzovctverec.domain.AppRoleRepository;
import cz.previt.bydzovctverec.domain.CrewMember;
import cz.previt.bydzovctverec.domain.CrewMemberRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import cz.previt.bydzovctverec.domain.UserRole;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/registrations")
public class RegistrationController {

  private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);
  private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

  private final EditionRepository editionRepository;
  private final RacerRegistrationRepository racerRegistrationRepository;
  private final UserRepository userRepository;
  private final AppRoleRepository appRoleRepository;
  private final CrewMemberRepository crewMemberRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  private record FeeConfig(int baseDo1945, int baseOd1946, int extraPerson) {}

  private static final Map<String, FeeConfig> FEE = Map.of(
      "JEDNODENNI", new FeeConfig(500, 800, 500),
      "DVODENNI", new FeeConfig(1000, 1200, 1000),
      "DVODENNI_UZAVRENO", new FeeConfig(1000, 1200, 1000),
      "DVODENNI_BEZ_UBYTOVANI", new FeeConfig(600, 900, 600));

  public RegistrationController(EditionRepository editionRepository,
      RacerRegistrationRepository racerRegistrationRepository, UserRepository userRepository,
      AppRoleRepository appRoleRepository, CrewMemberRepository crewMemberRepository,
      PasswordEncoder passwordEncoder, EmailService emailService) {
    this.editionRepository = editionRepository;
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.userRepository = userRepository;
    this.appRoleRepository = appRoleRepository;
    this.crewMemberRepository = crewMemberRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailService = emailService;
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequest request) {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      edition = editionRepository.save(new Edition(2026, "30. ročník Novobydžovského čtverce – Memoriál Elišky Junkové"));
    }

    int startFee = calculateFee(request.variant(), request.vehicleYear(), request.crewCount());

    String firstName = request.firstName() != null ? request.firstName().trim() : "";
    String lastName = request.lastName() != null ? request.lastName().trim() : "";
    String email = request.email() != null ? request.email().trim() : "";

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
        request.vehicleNotes(), request.notes(),
        false, false, false,
        request.consent() != null ? request.consent() : false,
        true, Instant.now());

    racerRegistrationRepository.save(reg);

    int paymentRef = generatePaymentReference(edition);
    reg.setPaymentReference(paymentRef);
    racerRegistrationRepository.save(reg);

    var racerRole = appRoleRepository.findByName("RACER").orElse(null);

    String driverPwd = createUser(email, firstName, lastName, racerRole, reg,
        request.driverAge(), request.gender(), request.address(),
        request.club(), request.firstTime());
    emailService.sendCredentials(email, firstName + " " + lastName, email, driverPwd,
        paymentRef, startFee);

    if (request.crewMembers() != null) {
      for (var cm : request.crewMembers()) {
        String cmEmail = cm.email().trim();
        String cmClub = Boolean.TRUE.equals(cm.clubMember())
            ? (cm.clubName() != null && !cm.clubName().isBlank() ? cm.clubName().trim() : "ano")
            : "";
        String cmPwd = createUser(cmEmail, cm.firstName().trim(), cm.lastName().trim(), racerRole, reg,
            cm.driverAge(), cm.gender(), cm.address(), cmClub, cm.firstTime());
        emailService.sendCredentials(cmEmail, cm.firstName() + " " + cm.lastName(), cmEmail,
            cmPwd, paymentRef, startFee);
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

  private String createUser(String email, String firstName, String lastName, AppRole racerRole,
      RacerRegistration reg, Integer driverAge, String gender, String address,
      String club, Boolean firstTime) {
    String rawPassword = generatePassword();
    User user = new User(email, email, passwordEncoder.encode(rawPassword), UserRole.RACER,
        firstName, lastName, Instant.now());
    if (racerRole != null) user.getAppRoles().add(racerRole);
    userRepository.save(user);
    crewMemberRepository.save(new CrewMember(reg, user, firstName, lastName, email,
        driverAge, gender, address,
        club != null && !club.isBlank(),
        firstTime,
        club));
    return rawPassword;
  }

  @GetMapping("/lookup-user")
  public ResponseEntity<?> lookupUserByEmail(String email) {
    if (email == null || email.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
    }
    return userRepository.findByEmail(email.trim())
        .map(u -> ResponseEntity.ok(Map.of(
            "firstName", u.getFirstName(),
            "lastName", u.getLastName(),
            "phone", u.getPhone() != null ? u.getPhone() : "")))
        .orElse(ResponseEntity.ok(Map.of()));
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

  private int generatePaymentReference(Edition edition) {
    return racerRegistrationRepository
        .findTopByEditionOrderByPaymentReferenceDesc(edition)
        .map(r -> {
          Integer ref = r.getPaymentReference();
          return ref != null ? ref + 1 : edition.getEditionYear() * 1000 + 1;
        })
        .orElse(edition.getEditionYear() * 1000 + 1);
  }

  public static int calculateFee(String variant, int vehicleYear, int crewCount) {
    FeeConfig cfg = FEE.getOrDefault(variant, new FeeConfig(0, 0, 0));
    int baseFee = vehicleYear < 1945 ? cfg.baseDo1945 : cfg.baseOd1946;
    return baseFee + cfg.extraPerson * Math.max(0, crewCount - 1);
  }

  private static String generatePassword() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 10; i++) {
      sb.append(CHARS.charAt((int) (Math.random() * CHARS.length())));
    }
    return sb.toString();
  }
}
