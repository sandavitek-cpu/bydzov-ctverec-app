package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.config.EmailService;
import cz.previt.bydzovctverec.config.JwtService;
import cz.previt.bydzovctverec.domain.AppRole;
import cz.previt.bydzovctverec.domain.AppRoleRepository;
import cz.previt.bydzovctverec.domain.Checkpoint;
import cz.previt.bydzovctverec.domain.CheckpointRepository;
import cz.previt.bydzovctverec.domain.CrewMember;
import cz.previt.bydzovctverec.domain.CrewMemberRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.Score;
import cz.previt.bydzovctverec.domain.ScoreRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import cz.previt.bydzovctverec.domain.UserRole;
import java.io.ByteArrayOutputStream;
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
import org.xhtmlrenderer.pdf.ITextRenderer;

@RestController
@RequestMapping("/api/admin/registrations")
public class AdminController {

  private static final Logger log = LoggerFactory.getLogger(AdminController.class);
  private final EditionRepository editionRepository;
  private final RacerRegistrationRepository racerRegistrationRepository;
  private final ScoreRepository scoreRepository;
  private final CheckpointRepository checkpointRepository;
  private final UserRepository userRepository;
  private final AppRoleRepository appRoleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final EmailService emailService;
  private final CrewMemberRepository crewMemberRepository;

  public AdminController(EditionRepository editionRepository, RacerRegistrationRepository racerRegistrationRepository, ScoreRepository scoreRepository, CheckpointRepository checkpointRepository, UserRepository userRepository, AppRoleRepository appRoleRepository, PasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService, CrewMemberRepository crewMemberRepository) {
    this.editionRepository = editionRepository;
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.scoreRepository = scoreRepository;
    this.checkpointRepository = checkpointRepository;
    this.userRepository = userRepository;
    this.appRoleRepository = appRoleRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.emailService = emailService;
    this.crewMemberRepository = crewMemberRepository;
  }

  @GetMapping
  public ResponseEntity<?> list() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      return ResponseEntity.ok(List.of());
    }
    List<RacerRegistration> regs = racerRegistrationRepository.findByEditionOrderByStartNumber(edition);
    return ResponseEntity.ok(regs.stream().map(r -> AdminRegistrationResponse.from(r, crewMemberRepository.findByRegistration(r))).toList());
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> get(@PathVariable Long id) {
    RacerRegistration reg = racerRegistrationRepository.findById(id).orElse(null);
    if (reg == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Přihláška nenalezena"));
    }
    return ResponseEntity.ok(AdminRegistrationResponse.from(reg, crewMemberRepository.findByRegistration(reg)));
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
    reg.setStatus(newStatus);
    if ("PAID".equals(newStatus) && reg.getPaidAt() == null) {
      reg.setPaidAt(Instant.now());
      if (reg.getPaidAmount() == null) {
        reg.setPaidAmount(reg.getStartFee());
      }
    } else if ("PENDING".equals(newStatus)) {
      reg.setPaidAt(null);
    }
    racerRegistrationRepository.save(reg);
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
    if (body.containsKey("teamName")) reg.setTeamName((String) body.get("teamName"));
    if (body.containsKey("phone")) reg.setPhone((String) body.get("phone"));
    if (body.containsKey("vehicleCategory")) reg.setVehicleCategory((String) body.get("vehicleCategory"));
    if (body.containsKey("vehicleMake")) reg.setVehicleMake((String) body.get("vehicleMake"));
    if (body.containsKey("vehiclePlate")) reg.setVehiclePlate((String) body.get("vehiclePlate"));
    if (body.containsKey("vehicleYear")) reg.setVehicleYear(toInt(body.get("vehicleYear")));
    if (body.containsKey("crewCount")) reg.setCrewCount(toInt(body.get("crewCount")));
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

    boolean feeAffected = body.containsKey("variant") || body.containsKey("vehicleYear") || body.containsKey("crewCount");
    String message = null;
    if (feeAffected) {
      Integer oldFee = reg.getStartFee();
      String variant = reg.getVariant();
      int year = reg.getVehicleYear() != null ? reg.getVehicleYear() : 0;
      int crew = reg.getCrewCount() != null ? reg.getCrewCount() : 1;
      int newFee = RegistrationController.calculateFee(variant, year, crew);
      if (newFee != oldFee) {
        reg.setStartFee(newFee);
        Integer paid = reg.getPaidAmount() != null ? reg.getPaidAmount() : 0;
        if (newFee > paid) {
          if ("PAID".equals(reg.getStatus())) {
            reg.setPaidAt(null);
          }
          reg.setStatus("PENDING");
          message = "Cena změněna na " + newFee + " Kč. Doplatek: " + (newFee - paid) + " Kč.";
        } else if (newFee < paid) {
          message = "Cena snížena na " + newFee + " Kč. Přeplatek: " + (paid - newFee) + " Kč.";
        }
      }
    }

    racerRegistrationRepository.save(reg);
    var response = AdminRegistrationResponse.from(reg, crewMemberRepository.findByRegistration(reg));
    if (message != null) {
      return ResponseEntity.ok(Map.of("registration", response, "message", message));
    }
    return ResponseEntity.ok(response);
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
        reg.getPaymentReference(), reg.getStartFee(), reg.getStartNumber());

    log.info("Registration {} approved, user {} created", id, email);
    return ResponseEntity.ok(Map.of("approved", true, "email", email));
  }

  @PostMapping("/{id}/cancel")
  @Transactional
  public ResponseEntity<?> cancel(@PathVariable Long id) {
    RacerRegistration reg = racerRegistrationRepository.findById(id).orElse(null);
    if (reg == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Přihláška nenalezena"));
    }
    if (reg.getCancelledAt() != null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Přihláška již byla stornována"));
    }

    Edition edition = reg.getEdition();
    Instant now = Instant.now();
    reg.setCancelledAt(now);
    reg.setStatus("CANCELLED");
    reg.setApproved(false);

    Integer paid = reg.getPaidAmount() != null ? reg.getPaidAmount() : 0;
    if (paid > 0) {
      Instant deadline = edition.getCancellationDeadline();
      if (deadline != null && now.isBefore(deadline)) {
        reg.setRefundAmount(paid);
      } else {
        reg.setRefundAmount(paid * 75 / 100);
      }
    }

    reg.setPaidAt(null);
    reg.setPaidAmount(0);
    reg.setStartNumber(null);
    reg.setPaymentReference(null);

    racerRegistrationRepository.save(reg);
    log.info("Registration {} cancelled, refund={}", id, reg.getRefundAmount());
    return ResponseEntity.ok(AdminRegistrationResponse.from(reg, crewMemberRepository.findByRegistration(reg)));
  }

  @PostMapping("/{id}/resend-credentials")
  @Transactional
  public ResponseEntity<?> resendCredentials(@PathVariable Long id) {
    RacerRegistration reg = racerRegistrationRepository.findById(id).orElse(null);
    if (reg == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Přihláška nenalezena"));
    }
    List<CrewMember> crew = crewMemberRepository.findByRegistration(reg);
    if (crew.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "K této přihlášce nejsou vytvořeny uživatelské účty"));
    }
    int sent = 0;
    for (CrewMember cm : crew) {
      User u = cm.getUser();
      if (u == null) continue;
      var racerRole = appRoleRepository.findByName("RACER").orElse(null);
      String rawPassword = generatePassword();
      u.setPassword(passwordEncoder.encode(rawPassword));
      userRepository.save(u);
      String personName = cm.getFirstName() + " " + cm.getLastName();
      emailService.sendCredentials(cm.getEmail(), personName, cm.getEmail(), rawPassword,
          reg.getPaymentReference(), reg.getStartFee(), reg.getStartNumber());
      sent++;
    }
    log.info("Credentials resent for registration {} ({} users)", id, sent);
    return ResponseEntity.ok(Map.of("resent", sent));
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

  @PostMapping("/{id}/assign-start-number")
  @Transactional
  public ResponseEntity<?> assignStartNumber(@PathVariable Long id) {
    RacerRegistration reg = racerRegistrationRepository.findById(id).orElse(null);
    if (reg == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Přihláška nenalezena"));
    }
    String variant = reg.getVariant();
    if (variant == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Přihláška nemá variantu"));
    }
    int rangeStart, rangeEnd;
    if ("JEDNODENNI".equals(variant)) {
      rangeStart = 1;
      rangeEnd = 99;
    } else {
      rangeStart = 101;
      rangeEnd = 130;
    }
    Edition edition = reg.getEdition();
    List<RacerRegistration> all = racerRegistrationRepository.findByEditionOrderByStartNumber(edition);
    var taken = all.stream()
        .map(RacerRegistration::getStartNumber)
        .filter(n -> n != null && n >= rangeStart && n <= rangeEnd)
        .collect(java.util.stream.Collectors.toSet());
    int assigned = 0;
    for (int i = rangeStart; i <= rangeEnd; i++) {
      if (!taken.contains(i)) { assigned = i; break; }
    }
    if (assigned == 0) {
      return ResponseEntity.badRequest().body(Map.of("error", "Vyčerpána kapacita startovních čísel v rozsahu " + rangeStart + "–" + rangeEnd));
    }
    reg.setStartNumber(assigned);
    log.info("Start number {} assigned to registration {}", assigned, id);
    return ResponseEntity.ok(Map.of("startNumber", assigned));
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

    List<Checkpoint> checkpoints = checkpointRepository.findByEditionOrderBySortOrder(edition);
    List<Score> scores = scoreRepository.findByEditionYearWithRacer(year);

    Map<RacerRegistration, List<Score>> grouped = scores.stream()
        .collect(Collectors.groupingBy(Score::getRacerRegistration));

    List<Map<String, Object>> results = new ArrayList<>();
    for (var entry : grouped.entrySet()) {
      RacerRegistration r = entry.getKey();
      List<Score> racerScores = entry.getValue();
      int totalPoints = racerScores.stream().mapToInt(Score::getPoints).sum();
      List<Map<String, Object>> scoresList = racerScores.stream()
          .map(s -> Map.<String, Object>of(
              "checkpointOrder", s.getCheckpoint().getSortOrder(),
              "checkpointName", s.getCheckpoint().getName(),
              "points", s.getPoints()))
          .toList();

      results.add(Map.of(
          "startNumber", r.getStartNumber(),
          "teamName", r.getTeamName() != null ? r.getTeamName() : (r.getFirstName() + " " + r.getLastName()),
          "vehicleCategory", r.getVehicleCategory(),
          "vehicleMake", r.getVehicleMake() != null ? r.getVehicleMake() : "",
          "vehiclePlate", r.getVehiclePlate(),
          "vehicleYear", r.getVehicleYear(),
          "variant", r.getVariant() != null ? r.getVariant() : "",
          "totalPoints", totalPoints,
          "scores", scoresList));
    }

    results.sort(Comparator.comparingInt(r -> -(int) r.get("totalPoints")));

    List<Map<String, Object>> ranked = new ArrayList<>();
    for (int i = 0; i < results.size(); i++) {
      Map<String, Object> row = new java.util.LinkedHashMap<>(results.get(i));
      row.put("rank", i + 1);
      ranked.add(row);
    }

    List<Map<String, Object>> cpList = checkpoints.stream()
        .map(cp -> Map.<String, Object>of(
            "sortOrder", cp.getSortOrder(),
            "name", cp.getName()))
        .toList();

    return ResponseEntity.ok(Map.of("year", year, "checkpoints", cpList, "results", ranked));
  }

  @GetMapping("/export/pdf")
  public ResponseEntity<byte[]> exportPdf() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      return ResponseEntity.badRequest().build();
    }
    List<RacerRegistration> regs = racerRegistrationRepository.findByEditionOrderByStartNumber(edition);
    StringBuilder html = new StringBuilder("""
        <html><head><meta charset="UTF-8">
        <style>
          body { font-family: sans-serif; font-size: 10pt; margin: 2cm; }
          h1 { text-align: center; font-size: 16pt; margin-bottom: 4pt; }
          .subtitle { text-align: center; font-size: 9pt; color: #666; margin-bottom: 20pt; }
          table { width: 100%; border-collapse: collapse; }
          th { background: #1a1a2e; color: white; padding: 6pt 8pt; text-align: left; font-size: 9pt; }
          td { padding: 4pt 8pt; border-bottom: 1px solid #ddd; font-size: 9pt; }
          tr:nth-child(even) td { background: #f8f8f8; }
          .right { text-align: right; }
          .center { text-align: center; }
        </style></head><body>
        <h1>Novobydžovský čtverec</h1>
        <p class="subtitle">""");
    html.append(edition.getLabel()).append(" — startovní listina</p>");
    html.append("""
        <table>
        <tr><th>#</th><th>Posádka</th><th>Kategorie</th><th>Vůz</th><th>SPZ</th><th>Ročník</th><th>Os.</th><th>Varianta</th><th>Stav</th></tr>
        """);
    for (RacerRegistration r : regs) {
      html.append("<tr>")
        .append("<td class=\"center\">").append(r.getStartNumber()).append("</td>")
        .append("<td>").append(escHtml(r.getTeamName())).append("</td>")
        .append("<td>").append(escHtml(r.getVehicleCategory())).append("</td>")
        .append("<td>").append(escHtml(r.getVehicleMake())).append("</td>")
        .append("<td>").append(escHtml(r.getVehiclePlate())).append("</td>")
        .append("<td class=\"center\">").append(r.getVehicleYear()).append("</td>")
        .append("<td class=\"center\">").append(r.getCrewCount()).append("</td>")
        .append("<td>").append(escHtml(r.getVariant())).append("</td>")
        .append("<td>").append("PAID".equals(r.getStatus()) ? "Přihlášen a zaplaceno" : "Přihlášen, nezaplaceno").append("</td>")
        .append("</tr>");
    }
    html.append("</table></body></html>");

    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ITextRenderer renderer = new ITextRenderer();
      renderer.setDocumentFromString(html.toString());
      renderer.layout();
      renderer.createPDF(bos);
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=startovni_listina.pdf")
          .contentType(MediaType.APPLICATION_PDF)
          .body(bos.toByteArray());
    } catch (Exception e) {
      log.error("PDF generation failed", e);
      return ResponseEntity.internalServerError().build();
    }
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

  private String escHtml(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
      case "PAID" -> "Přihlášen a zaplaceno";
      case "PENDING" -> "Přihlášen, nezaplaceno";
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
