package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.config.EmailService;
import cz.previt.bydzovctverec.config.JwtService;
import cz.previt.bydzovctverec.domain.AppRole;
import cz.previt.bydzovctverec.domain.AppRoleRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.Score;
import cz.previt.bydzovctverec.domain.ScoreRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import cz.previt.bydzovctverec.domain.UserRole;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/registrations")
public class AdminController {

  private static final Logger log = LoggerFactory.getLogger(AdminController.class);
  private final EditionRepository editionRepository;
  private final RacerRegistrationRepository racerRegistrationRepository;
  private final ScoreRepository scoreRepository;
  private final UserRepository userRepository;
  private final AppRoleRepository appRoleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final EmailService emailService;

  public AdminController(EditionRepository editionRepository, RacerRegistrationRepository racerRegistrationRepository, ScoreRepository scoreRepository, UserRepository userRepository, AppRoleRepository appRoleRepository, PasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService) {
    this.editionRepository = editionRepository;
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.scoreRepository = scoreRepository;
    this.userRepository = userRepository;
    this.appRoleRepository = appRoleRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.emailService = emailService;
  }

  @GetMapping
  public ResponseEntity<?> list() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      return ResponseEntity.ok(List.of());
    }
    List<RacerRegistration> regs = racerRegistrationRepository.findByEditionOrderByStartNumber(edition);
    return ResponseEntity.ok(regs.stream().map(AdminRegistrationResponse::from).toList());
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> get(@PathVariable Long id) {
    RacerRegistration reg = racerRegistrationRepository.findById(id).orElse(null);
    if (reg == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Přihláška nenalezena"));
    }
    return ResponseEntity.ok(AdminRegistrationResponse.from(reg));
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

  @PutMapping("/{id}")
  @Transactional
  public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    RacerRegistration reg = racerRegistrationRepository.findById(id).orElse(null);
    if (reg == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Přihláška nenalezena"));
    }
    if (body.containsKey("variant")) reg.setVariant((String) body.get("variant"));
    if (body.containsKey("vehicleMake")) reg.setVehicleMake((String) body.get("vehicleMake"));
    if (body.containsKey("firstTime")) reg.setFirstTime((Boolean) body.get("firstTime"));
    if (body.containsKey("gender")) reg.setGender((String) body.get("gender"));
    if (body.containsKey("driverAge")) reg.setDriverAge(toInt(body.get("driverAge")));
    if (body.containsKey("club")) reg.setClub((String) body.get("club"));
    if (body.containsKey("address")) reg.setAddress((String) body.get("address"));
    if (body.containsKey("youngestAge")) reg.setYoungestAge(toInt(body.get("youngestAge")));
    if (body.containsKey("youngestName")) reg.setYoungestName((String) body.get("youngestName"));
    if (body.containsKey("engineDisplacement")) reg.setEngineDisplacement(toInt(body.get("engineDisplacement")));
    if (body.containsKey("power")) reg.setPower(toInt(body.get("power")));
    if (body.containsKey("maxSpeed")) reg.setMaxSpeed(toInt(body.get("maxSpeed")));
    if (body.containsKey("vehicleNotes")) reg.setVehicleNotes((String) body.get("vehicleNotes"));
    if (body.containsKey("notes")) reg.setNotes((String) body.get("notes"));
    if (body.containsKey("contacted")) reg.setContacted((Boolean) body.get("contacted"));
    if (body.containsKey("properlyRegistered")) reg.setProperlyRegistered((Boolean) body.get("properlyRegistered"));
    if (body.containsKey("arrived")) reg.setArrived((Boolean) body.get("arrived"));
    if (body.containsKey("consent")) reg.setConsent((Boolean) body.get("consent"));
    racerRegistrationRepository.save(reg);
    return ResponseEntity.ok(AdminRegistrationResponse.from(reg));
  }

  @PostMapping("/{id}/approve")
  @Transactional
  public ResponseEntity<?> approve(@PathVariable Long id) {
    RacerRegistration reg = racerRegistrationRepository.findById(id).orElse(null);
    if (reg == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Přihláška nenalezena"));
    }
    if (Boolean.TRUE.equals(reg.getApproved())) {
      return ResponseEntity.badRequest().body(Map.of("error", "Přihláška již schválena"));
    }

    String email = reg.getEmail();
    if (userRepository.findByEmail(email).isPresent()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Uživatel s tímto emailem již existuje"));
    }

    String rawPassword = generatePassword();
    var racerRole = appRoleRepository.findByName("RACER").orElse(null);
    var roles = new HashSet<AppRole>();
    if (racerRole != null) roles.add(racerRole);

    String firstName = reg.getFirstName() != null && !reg.getFirstName().isBlank()
        ? reg.getFirstName()
        : reg.getTeamName();
    String lastName = reg.getLastName() != null && !reg.getLastName().isBlank()
        ? reg.getLastName()
        : "";
    String personName = (firstName + " " + lastName).trim();
    if (personName.isEmpty()) {
        personName = reg.getTeamName();
    }
    User user = new User(email, email, passwordEncoder.encode(rawPassword),
        UserRole.RACER, firstName, lastName, Instant.now());
    user.getAppRoles().addAll(roles);
    userRepository.save(user);

    reg.setApproved(true);
    racerRegistrationRepository.save(reg);

    emailService.sendCredentials(email, personName, email, rawPassword,
        reg.getStartNumber(), reg.getStartFee());

    log.info("Registration {} approved, user {} created", id, email);
    return ResponseEntity.ok(Map.of("approved", true, "email", email));
  }

  @PostMapping("/{id}/impersonate")
  public ResponseEntity<?> impersonate(@PathVariable Long id) {
    RacerRegistration reg = racerRegistrationRepository.findById(id).orElse(null);
    if (reg == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Přihláška nenalezena"));
    }
    User user = userRepository.findByEmail(reg.getEmail()).orElse(null);
    if (user == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Uživatel pro tuto přihlášku neexistuje (není schválena)"));
    }
    String accessToken = jwtService.generateAccessToken(user);
    String roleStr = user.getAppRoles().isEmpty()
        ? user.getRole().name()
        : user.getAppRoles().stream().map(AppRole::getName).collect(java.util.stream.Collectors.joining(","));
    return ResponseEntity.ok(Map.of("accessToken", accessToken, "username", user.getUsername(), "name", user.getName(), "role", roleStr));
  }

  @GetMapping("/stats")
  public ResponseEntity<?> stats() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      return ResponseEntity.ok(Map.of());
    }
    List<RacerRegistration> all = racerRegistrationRepository.findByEditionOrderByStartNumber(edition);

    int totalCrews = all.size();
    int totalMembers = all.stream().mapToInt(r -> r.getCrewCount() != null ? r.getCrewCount() : 0).sum();
    long paid = all.stream().filter(r -> "PAID".equals(r.getStatus())).count();
    long contacted = all.stream().filter(r -> Boolean.TRUE.equals(r.getContacted())).count();
    long arrived = all.stream().filter(r -> Boolean.TRUE.equals(r.getArrived())).count();
    long firstTimers = all.stream().filter(r -> Boolean.TRUE.equals(r.getFirstTime())).count();
    long women = all.stream().filter(r -> "Z".equals(r.getGender()) || "z".equals(r.getGender())).count();
    long kidsUnder10 = all.stream().filter(r -> r.getYoungestAge() != null && r.getYoungestAge() < 10).count();

    long vehiclesBefore1945 = all.stream()
        .filter(r -> r.getVehicleYear() != null && r.getVehicleYear() < 1945).count();
    long cars = all.stream().filter(r -> "OSOBNI".equals(r.getVehicleCategory()) || "CLASSIC".equals(r.getVehicleCategory())).count();
    long motos = all.stream().filter(r -> "MOTOCYKL".equals(r.getVehicleCategory())).count();

    int oldestVehicle = all.stream()
        .filter(r -> r.getVehicleYear() != null && r.getVehicleYear() > 1900)
        .mapToInt(RacerRegistration::getVehicleYear).min().orElse(0);
    int newestVehicle = all.stream()
        .filter(r -> r.getVehicleYear() != null)
        .mapToInt(RacerRegistration::getVehicleYear).max().orElse(0);
    int oldestDriver = all.stream()
        .filter(r -> r.getDriverAge() != null)
        .mapToInt(RacerRegistration::getDriverAge).max().orElse(0);
    int youngestDriver = all.stream()
        .filter(r -> r.getDriverAge() != null)
        .mapToInt(RacerRegistration::getDriverAge).min().orElse(0);

    long jednodenni = all.stream().filter(r -> "JEDNODENNI".equals(r.getVariant())).count();
    long dvoudenni = all.stream().filter(r -> "DVODENNI".equals(r.getVariant())).count();
    long withoutAccommodation = all.stream().filter(r -> r.getVariant() == null || r.getVariant().isEmpty()).count();
    long approved = all.stream().filter(r -> Boolean.TRUE.equals(r.getApproved())).count();

    int jednodenniMembers = all.stream()
        .filter(r -> "JEDNODENNI".equals(r.getVariant()))
        .mapToInt(r -> r.getCrewCount() != null ? r.getCrewCount() : 0).sum();
    int dvoudenniMembers = all.stream()
        .filter(r -> "DVODENNI".equals(r.getVariant()))
        .mapToInt(r -> r.getCrewCount() != null ? r.getCrewCount() : 0).sum();

    var stats = new java.util.LinkedHashMap<String, Object>();
    stats.put("totalCrews", totalCrews);
    stats.put("totalMembers", totalMembers);
    stats.put("paid", paid);
    stats.put("contacted", contacted);
    stats.put("arrived", arrived);
    stats.put("firstTimers", firstTimers);
    stats.put("women", women);
    stats.put("kidsUnder10", kidsUnder10);
    stats.put("vehiclesBefore1945", vehiclesBefore1945);
    stats.put("cars", cars);
    stats.put("motos", motos);
    stats.put("oldestVehicle", oldestVehicle);
    stats.put("newestVehicle", newestVehicle);
    stats.put("oldestDriver", oldestDriver);
    stats.put("youngestDriver", youngestDriver);
    stats.put("jednodenni", jednodenni);
    stats.put("dvoudenni", dvoudenni);
    stats.put("withoutAccommodation", withoutAccommodation);
    stats.put("jednodenniMembers", jednodenniMembers);
    stats.put("dvoudenniMembers", dvoudenniMembers);
    stats.put("approved", approved);
    return ResponseEntity.ok(stats);
  }

  @GetMapping("/results/{year}")
  public ResponseEntity<?> results(@PathVariable Integer year) {
    Edition edition = editionRepository.findByEditionYear(year).orElse(null);
    if (edition == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Ročník nenalezen"));
    }

    List<RacerRegistration> regs = racerRegistrationRepository.findByEditionOrderByStartNumber(edition);
    List<Score> scores = scoreRepository.findByEditionYearWithRacer(year);

    Map<Long, List<Score>> scoresByRacer = scores.stream()
        .collect(Collectors.groupingBy(s -> s.getRacerRegistration().getId()));

    List<Map<String, Object>> results = new ArrayList<>();
    for (var reg : regs) {
      List<Score> racerScores = scoresByRacer.getOrDefault(reg.getId(), List.of());
      int totalPoints = racerScores.stream().mapToInt(Score::getPoints).sum();

      Map<String, Object> kBody = new java.util.LinkedHashMap<>();
      for (var score : racerScores) {
        kBody.put("K" + score.getRunNumber(), score.getPoints());
      }

      results.add(Map.of(
          "startNumber", reg.getStartNumber(),
          "teamName", reg.getTeamName() != null ? reg.getTeamName() : (reg.getFirstName() + " " + reg.getLastName()),
          "vehicleCategory", reg.getVehicleCategory(),
          "vehicleMake", reg.getVehicleMake() != null ? reg.getVehicleMake() : "",
          "vehiclePlate", reg.getVehiclePlate(),
          "vehicleYear", reg.getVehicleYear(),
          "variant", reg.getVariant() != null ? reg.getVariant() : "",
          "totalPoints", totalPoints,
          "kBody", kBody));
    }

    results.sort(Comparator.comparingInt(r -> -(int) r.get("totalPoints")));

    List<Map<String, Object>> ranked = new ArrayList<>();
    for (int i = 0; i < results.size(); i++) {
      Map<String, Object> row = new java.util.LinkedHashMap<>(results.get(i));
      row.put("rank", i + 1);
      ranked.add(row);
    }

    return ResponseEntity.ok(Map.of("year", year, "results", ranked));
  }

  @GetMapping("/export")
  public ResponseEntity<String> exportCsv() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      return ResponseEntity.ok("Žádný aktivní ročník");
    }
    List<RacerRegistration> regs = racerRegistrationRepository.findByEditionOrderByStartNumber(edition);
    String header = "Startovní číslo,Jméno posádky,E-mail,Telefon,Kategorie,SPZ,Ročník,Počet osob,Startovné,Stav,Schváleno\n";
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
            statusLabel(r.getStatus()),
            Boolean.TRUE.equals(r.getApproved()) ? "Ano" : "Ne"))
        .collect(Collectors.joining("\n"));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=prihlasky.csv")
        .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
        .body(header + body);
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

  private Integer toInt(Object v) {
    if (v == null) return null;
    if (v instanceof Number n) return n.intValue();
    try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
  }

  private String generatePassword() {
    String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 10; i++) {
      sb.append(chars.charAt((int) (Math.random() * chars.length())));
    }
    return sb.toString();
  }
}
