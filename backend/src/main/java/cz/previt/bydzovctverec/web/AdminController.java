package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.config.EmailService;
import cz.previt.bydzovctverec.config.JwtService;
import cz.previt.bydzovctverec.domain.AppRole;
import cz.previt.bydzovctverec.domain.Checkpoint;
import cz.previt.bydzovctverec.domain.CheckpointRepository;
import cz.previt.bydzovctverec.domain.CrewMember;
import cz.previt.bydzovctverec.domain.CrewMemberRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.Score;
import cz.previt.bydzovctverec.domain.ScoreRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import cz.previt.bydzovctverec.service.EditionService;
import cz.previt.bydzovctverec.service.RankingService;
import cz.previt.bydzovctverec.service.RegistrationService;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
  private final EditionService editionService;
  private final RacerRegistrationRepository racerRegistrationRepository;
  private final ScoreRepository scoreRepository;
  private final CheckpointRepository checkpointRepository;
  private final UserRepository userRepository;
  private final CrewMemberRepository crewMemberRepository;
  private final RegistrationService registrationService;
  private final RankingService rankingService;
  private final JwtService jwtService;
  private final EmailService emailService;

  public AdminController(EditionService editionService,
      RacerRegistrationRepository racerRegistrationRepository, ScoreRepository scoreRepository,
      CheckpointRepository checkpointRepository, UserRepository userRepository,
      CrewMemberRepository crewMemberRepository, RegistrationService registrationService,
      RankingService rankingService, JwtService jwtService, EmailService emailService) {
    this.editionService = editionService;
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.scoreRepository = scoreRepository;
    this.checkpointRepository = checkpointRepository;
    this.userRepository = userRepository;
    this.crewMemberRepository = crewMemberRepository;
    this.registrationService = registrationService;
    this.rankingService = rankingService;
    this.jwtService = jwtService;
    this.emailService = emailService;
  }

  @GetMapping
  public ResponseEntity<?> list() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) {
      return ResponseEntity.ok(List.of());
    }
    List<RacerRegistration> regs = racerRegistrationRepository.findByEditionOrderByStartNumber(edition);
    List<CrewMember> allCrew = crewMemberRepository.findByRegistrationIn(regs);
    Map<Long, List<CrewMember>> crewByRegId = allCrew.stream()
        .collect(Collectors.groupingBy(cm -> cm.getRegistration().getId()));
    return ResponseEntity.ok(regs.stream()
        .map(r -> AdminRegistrationResponse.from(r, crewByRegId.getOrDefault(r.getId(), List.of())))
        .toList());
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> get(@PathVariable Long id) {
    RacerRegistration reg = racerRegistrationRepository.findById(id).orElse(null);
    if (reg == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Přihláška nenalezena"));
    }
    return ResponseEntity.ok(AdminRegistrationResponse.from(reg, crewMemberRepository.findByRegistration(reg)));
  }

  @PatchMapping("/{id}/status")
  @Transactional
  public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
    String newStatus = body.get("status");
    if (newStatus == null || (!newStatus.equals("PAID") && !newStatus.equals("PENDING"))) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Neplatný stav (PAID / PENDING)"));
    }
    RacerRegistration reg = racerRegistrationRepository.findById(id).orElse(null);
    if (reg == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Přihláška nenalezena"));
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
      return ResponseEntity.badRequest().body(ApiResponse.error("Přihláška nenalezena"));
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
      int newFee = registrationService.recalculateFee(reg);
      if (newFee != oldFee) {
        reg.setStartFee(newFee);
        Integer paid = reg.getPaidAmount() != null ? reg.getPaidAmount() : 0;
        if (newFee > paid) {
          if ("PAID".equals(reg.getStatus())) reg.setPaidAt(null);
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
      return ResponseEntity.badRequest().body(ApiResponse.error("Přihláška nenalezena"));
    }
    try {
      String email = registrationService.approveRegistration(reg);
      return ResponseEntity.ok(Map.of("approved", true, "email", email));
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
  }

  @PostMapping("/{id}/cancel")
  @Transactional
  public ResponseEntity<?> cancel(@PathVariable Long id) {
    RacerRegistration reg = racerRegistrationRepository.findById(id).orElse(null);
    if (reg == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Přihláška nenalezena"));
    }
    try {
      registrationService.cancelRegistration(reg);
      return ResponseEntity.ok(AdminRegistrationResponse.from(reg, crewMemberRepository.findByRegistration(reg)));
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
  }

  @PostMapping("/{id}/resend-credentials")
  @Transactional
  public ResponseEntity<?> resendCredentials(@PathVariable Long id) {
    RacerRegistration reg = racerRegistrationRepository.findById(id).orElse(null);
    if (reg == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Přihláška nenalezena"));
    }
    try {
      registrationService.resendCredentials(reg);
      return ResponseEntity.ok(Map.of("resent", true));
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
  }

  @PostMapping("/{id}/remind")
  public ResponseEntity<?> remindPayment(@PathVariable Long id) {
    RacerRegistration reg = racerRegistrationRepository.findById(id).orElse(null);
    if (reg == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Přihláška nenalezena"));
    }
    if ("PAID".equals(reg.getStatus())) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Přihláška je již zaplacena"));
    }
    Integer fee = reg.getStartFee();
    Integer ref = reg.getPaymentReference();
    String name = reg.getTeamName();
    String body = """
Dobrý den,

zatím jsme neobdrželi platbu startovného pro tým "%s".

  Startovné: %d Kč
  Variabilní symbol: %s

Pokud jste již platbu provedli, tuto zprávu prosím ignorujte.
V případě dotazů nás kontaktujte na info@bydzov-ctverec.cz.

Děkujeme za Vaši účast.
Tým Novobydžovského čtverce
""".formatted(name, fee, ref != null ? String.valueOf(ref) : "—");
    emailService.send(reg.getEmail(), "Novobydžovský čtverec – upomínka nezaplaceného startovného", body);
    log.info("Payment reminder sent to registration {} ({})", id, reg.getEmail());
    return ResponseEntity.ok(Map.of("sent", true));
  }

  @PostMapping("/{id}/impersonate")
  public ResponseEntity<?> impersonate(@PathVariable Long id) {
    RacerRegistration reg = racerRegistrationRepository.findById(id).orElse(null);
    if (reg == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Přihláška nenalezena"));
    }
    User user = userRepository.findByEmail(reg.getEmail()).orElse(null);
    if (user == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Uživatel pro tuto přihlášku neexistuje (není schválena)"));
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
      return ResponseEntity.badRequest().body(ApiResponse.error("Přihláška nenalezena"));
    }
    try {
      int number = registrationService.assignStartNumber(reg);
      return ResponseEntity.ok(Map.of("startNumber", number));
    } catch (IllegalStateException | IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
  }

  @GetMapping("/stats")
  public ResponseEntity<?> stats() {
    Edition edition = editionService.getCurrentEdition();
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
    long vehiclesBefore1945 = all.stream().filter(r -> r.getVehicleYear() != null && r.getVehicleYear() < 1945).count();
    long cars = all.stream().filter(r -> "OSOBNI".equals(r.getVehicleCategory()) || "CLASSIC".equals(r.getVehicleCategory())).count();
    long motos = all.stream().filter(r -> "MOTOCYKL".equals(r.getVehicleCategory())).count();
    int oldestVehicle = all.stream().filter(r -> r.getVehicleYear() != null && r.getVehicleYear() > 1900).mapToInt(RacerRegistration::getVehicleYear).min().orElse(0);
    int newestVehicle = all.stream().filter(r -> r.getVehicleYear() != null).mapToInt(RacerRegistration::getVehicleYear).max().orElse(0);
    int oldestDriver = all.stream().filter(r -> r.getDriverAge() != null).mapToInt(RacerRegistration::getDriverAge).max().orElse(0);
    int youngestDriver = all.stream().filter(r -> r.getDriverAge() != null).mapToInt(RacerRegistration::getDriverAge).min().orElse(0);
    long jednodenni = all.stream().filter(r -> "JEDNODENNI".equals(r.getVariant())).count();
    long dvoudenni = all.stream().filter(r -> "DVODENNI".equals(r.getVariant())).count();
    long withoutAccommodation = all.stream().filter(r -> r.getVariant() == null || r.getVariant().isEmpty()).count();
    long approved = all.stream().filter(r -> Boolean.TRUE.equals(r.getApproved())).count();
    int jednodenniMembers = all.stream().filter(r -> "JEDNODENNI".equals(r.getVariant())).mapToInt(r -> r.getCrewCount() != null ? r.getCrewCount() : 0).sum();
    int dvoudenniMembers = all.stream().filter(r -> "DVODENNI".equals(r.getVariant())).mapToInt(r -> r.getCrewCount() != null ? r.getCrewCount() : 0).sum();

    var stats = new LinkedHashMap<String, Object>();
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
    Edition edition = editionService.getByYear(year);
    if (edition == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Ročník nenalezen"));
    }
    List<Checkpoint> checkpoints = checkpointRepository.findByEditionOrderBySortOrder(edition);
    var ranked = rankingService.computeRanking(year);

    List<Map<String, Object>> cpList = checkpoints.stream()
        .map(cp -> Map.<String, Object>of("sortOrder", cp.getSortOrder(), "name", cp.getName()))
        .toList();

    return ResponseEntity.ok(Map.of("year", year, "checkpoints", cpList, "results", ranked));
  }

  @GetMapping("/export/pdf")
  public ResponseEntity<byte[]> exportPdf() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) return ResponseEntity.badRequest().build();
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
        .append("<td class=\"center\">").append(String.valueOf(r.getStartNumber())).append("</td>")
        .append("<td>").append(escHtml(r.getTeamName())).append("</td>")
        .append("<td>").append(escHtml(r.getVehicleCategory())).append("</td>")
        .append("<td>").append(escHtml(r.getVehicleMake())).append("</td>")
        .append("<td>").append(escHtml(r.getVehiclePlate())).append("</td>")
        .append("<td class=\"center\">").append(String.valueOf(r.getVehicleYear())).append("</td>")
        .append("<td class=\"center\">").append(String.valueOf(r.getCrewCount())).append("</td>")
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

  @GetMapping("/{id}/export/pdf")
  public ResponseEntity<byte[]> exportSinglePdf(@PathVariable Long id) {
    RacerRegistration r = racerRegistrationRepository.findById(id).orElse(null);
    if (r == null) return ResponseEntity.badRequest().build();
    Edition edition = r.getEdition();
    List<CrewMember> crew = crewMemberRepository.findByRegistration(r);
    String variantLabel = r.getVariant() != null ? switch (r.getVariant()) {
      case "JEDNODENNI" -> "Jednodenní závod";
      case "DVODENNI_UZAVRENO" -> "Dvoudenní závod – UZAVŘENO";
      case "DVODENNI_BEZ_UBYTOVANI" -> "Dvoudenní závod bez ubytování";
      default -> r.getVariant();
    } : "—";
    String catLabel = "AUTO".equals(r.getVehicleCategory()) ? "Automobil" : "Motocykl";
    String stLabel = "PAID".equals(r.getStatus()) ? "Zaplaceno" : "Nezaplaceno";
    StringBuilder crewHtml = new StringBuilder();
    if (crew != null) {
      for (CrewMember cm : crew) {
        crewHtml.append("""
          <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td class="sign-col"></td></tr>
        """.formatted(
            escHtml(cm.getFirstName()), escHtml(cm.getLastName()), escHtml(cm.getEmail()),
            cm.getDriverAge() != null ? String.valueOf(cm.getDriverAge()) : "",
            "M".equals(cm.getGender()) ? "Muž" : "Z".equals(cm.getGender()) ? "Žena" : "",
            escHtml(cm.getAddress())
        ));
      }
    }
    String html = """
        <html><head><meta charset="UTF-8">
        <style>
          @page { margin: 1.8cm 1.5cm; }
          body { font-family: sans-serif; font-size: 10pt; color: #1a1a2e; }
          h1 { text-align: center; font-size: 14pt; margin: 0 0 2pt; }
          .edition { text-align: center; font-size: 9pt; color: #666; margin-bottom: 16pt; }
          .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12pt; }
          .start-number { font-size: 18pt; font-weight: bold; background: #1a1a2e; color: white; padding: 6pt 14pt; border-radius: 4pt; }
          .team-name { font-size: 13pt; font-weight: bold; }
          .section { border: 1px solid #ccc; border-radius: 4pt; padding: 10pt; margin-bottom: 10pt; }
          .section-title { font-size: 9pt; font-weight: bold; text-transform: uppercase; color: #666; margin-bottom: 6pt; letter-spacing: 0.5pt; }
          .row { display: flex; gap: 12pt; margin-bottom: 3pt; }
          .field { flex: 1; }
          .label { font-size: 7.5pt; color: #999; text-transform: uppercase; }
          .value { font-size: 10pt; font-weight: 500; }
          .status-badge { display: inline-block; padding: 2pt 8pt; border-radius: 3pt; font-size: 8pt; font-weight: bold; }
          .paid { background: #d4edda; color: #155724; }
          .unpaid { background: #fff3cd; color: #856404; }
          table { width: 100%%; border-collapse: collapse; font-size: 9pt; }
          th { background: #f0f0f0; padding: 4pt 6pt; text-align: left; font-size: 7.5pt; text-transform: uppercase; color: #666; }
          td { padding: 4pt 6pt; border-bottom: 1px solid #eee; }
          .signature { margin-top: 16pt; }
          .signature .line { border-top: 1px solid #333; width: 250pt; margin-top: 30pt; padding-top: 3pt; font-size: 8pt; color: #999; }
          .footer { text-align: center; font-size: 7.5pt; color: #999; margin-top: 20pt; }
        </style></head><body>
        <h1>Novobydžovský čtverec</h1>
        <p class="edition">%s — přihláška k prezenci</p>
        <div class="header">
          <div><span class="team-name">%s</span></div>
          <div class="start-number">#%s</div>
        </div>
        <div class="section">
          <div class="section-title">Údaje o vozidle a závodu</div>
          <div class="row">
            <div class="field"><div class="label">Varianta</div><div class="value">%s</div></div>
            <div class="field"><div class="label">Kategorie</div><div class="value">%s</div></div>
            <div class="field"><div class="label">Značka</div><div class="value">%s</div></div>
          </div>
          <div class="row">
            <div class="field"><div class="label">SPZ</div><div class="value">%s</div></div>
            <div class="field"><div class="label">Ročník</div><div class="value">%s</div></div>
            <div class="field"><div class="label">Počet osob</div><div class="value">%s</div></div>
          </div>
          <div class="row">
            <div class="field"><div class="label">Stav platby</div><div class="value"><span class="status-badge %s">%s</span></div></div>
            <div class="field"><div class="label">Startovné</div><div class="value">%s Kč</div></div>
            <div class="field"><div class="label">VS</div><div class="value">%s</div></div>
          </div>
        </div>
        <div class="section">
          <div class="section-title">Řidič</div>
          <div class="row">
            <div class="field"><div class="label">Jméno</div><div class="value">%s %s</div></div>
            <div class="field"><div class="label">Věk</div><div class="value">%s</div></div>
            <div class="field"><div class="label">Telefon</div><div class="value">%s</div></div>
          </div>
          <div class="row">
            <div class="field"><div class="label">Email</div><div class="value">%s</div></div>
            <div class="field"><div class="label">Bydliště</div><div class="value">%s</div></div>
            <div class="field"><div class="label">Klub</div><div class="value">%s</div></div>
          </div>
        </div>
        """.formatted(
            edition.getLabel(),
            escHtml(r.getTeamName()), r.getStartNumber() != null ? String.valueOf(r.getStartNumber()) : "",
            variantLabel, catLabel, escHtml(r.getVehicleMake()),
            escHtml(r.getVehiclePlate()), r.getVehicleYear() != null ? String.valueOf(r.getVehicleYear()) : "",
            String.valueOf(r.getCrewCount()),
            "PAID".equals(r.getStatus()) ? "paid" : "unpaid", stLabel,
            r.getStartFee() != null ? String.valueOf(r.getStartFee()) : "",
            r.getPaymentReference() != null ? String.valueOf(r.getPaymentReference()) : "",
            escHtml(r.getFirstName()), escHtml(r.getLastName()),
            String.valueOf(r.getDriverAge()), escHtml(r.getPhone()),
            escHtml(r.getEmail()), escHtml(r.getAddress()), escHtml(r.getClub())
        );
    if (!crewHtml.isEmpty()) {
      html += """
        <div class="section">
          <div class="section-title">Ostatní členové posádky</div>
          <table>
          <tr><th>Jméno</th><th>Příjmení</th><th>Email</th><th>Věk</th><th>Pohlaví</th><th>Bydliště</th><th>Podpis</th></tr>
          %s
          </table>
        </div>
        """.formatted(crewHtml.toString());
    }
    html += """
        <div class="signature">
          <div class="line">Podpis řidiče</div>
        </div>
        <div class="footer">Vygenerováno z aplikace Novobydžovský čtverec</div>
        </body></html>
        """;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ITextRenderer renderer = new ITextRenderer();
      renderer.setDocumentFromString(html);
      renderer.layout();
      renderer.createPDF(bos);
      String teamName = r.getTeamName() != null ? r.getTeamName().replaceAll("\\s+", "_") : "neznámý";
      String filename = "prihlaska_%d_%s.pdf".formatted(
          r.getStartNumber() != null ? r.getStartNumber() : 0, teamName);
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(filename))
          .contentType(MediaType.APPLICATION_PDF)
          .body(bos.toByteArray());
    } catch (Exception e) {
      log.error("Single PDF generation failed for registration {}", id, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/export")
  public ResponseEntity<String> exportCsv() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) return ResponseEntity.ok("Žádný aktivní ročník");
    List<RacerRegistration> regs = racerRegistrationRepository.findByEditionOrderByStartNumber(edition);
    String header = "Startovní číslo,Jméno posádky,E-mail,Telefon,Kategorie,SPZ,Ročník,Počet osob,Startovné,Stav,Schváleno\n";
    String body = regs.stream()
        .map(r -> String.join(",",
            csvEscape(r.getStartNumber() != null ? String.valueOf(r.getStartNumber()) : ""),
            csvEscape(r.getTeamName()),
            csvEscape(r.getEmail()),
            csvEscape(r.getPhone()),
            csvEscape(r.getVehicleCategory()),
            csvEscape(r.getVehiclePlate()),
            csvEscape(r.getVehicleYear() != null ? String.valueOf(r.getVehicleYear()) : ""),
            csvEscape(String.valueOf(r.getCrewCount())),
            csvEscape(r.getStartFee() != null ? String.valueOf(r.getStartFee()) : ""),
            csvEscape(statusLabel(r.getStatus())),
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
}
