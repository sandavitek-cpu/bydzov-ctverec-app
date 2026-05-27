package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Checkpoint;
import cz.previt.bydzovctverec.domain.CheckpointRepository;
import cz.previt.bydzovctverec.domain.CrewMemberRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.domain.Route;
import cz.previt.bydzovctverec.domain.RoutePoint;
import cz.previt.bydzovctverec.domain.RoutePointRepository;
import cz.previt.bydzovctverec.domain.RouteRepository;
import cz.previt.bydzovctverec.domain.ScheduleItem;
import cz.previt.bydzovctverec.domain.ScheduleItemRepository;
import cz.previt.bydzovctverec.domain.Score;
import cz.previt.bydzovctverec.domain.ScoreRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.service.EditionService;
import cz.previt.bydzovctverec.service.RankingService;
import cz.previt.bydzovctverec.service.RegistrationService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/racer")
public class RacerController {

  private final RacerRegistrationRepository racerRegistrationRepository;
  private final ScoreRepository scoreRepository;
  private final EditionService editionService;
  private final ScheduleItemRepository scheduleItemRepository;
  private final CheckpointRepository checkpointRepository;
  private final RouteRepository routeRepository;
  private final RoutePointRepository routePointRepository;
  private final CrewMemberRepository crewMemberRepository;
  private final RankingService rankingService;
  private final RegistrationService registrationService;

  public RacerController(RacerRegistrationRepository racerRegistrationRepository,
      ScoreRepository scoreRepository, EditionService editionService,
      ScheduleItemRepository scheduleItemRepository, CheckpointRepository checkpointRepository,
      RouteRepository routeRepository, RoutePointRepository routePointRepository,
      CrewMemberRepository crewMemberRepository, RankingService rankingService,
      RegistrationService registrationService) {
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.scoreRepository = scoreRepository;
    this.editionService = editionService;
    this.scheduleItemRepository = scheduleItemRepository;
    this.checkpointRepository = checkpointRepository;
    this.routeRepository = routeRepository;
    this.routePointRepository = routePointRepository;
    this.crewMemberRepository = crewMemberRepository;
    this.rankingService = rankingService;
    this.registrationService = registrationService;
  }

  @GetMapping("/profile")
  public ResponseEntity<?> myProfile(Authentication auth) {
    User user = (User) auth.getPrincipal();
    var m = new LinkedHashMap<String, Object>();
    m.put("firstName", user.getFirstName());
    m.put("lastName", user.getLastName());
    m.put("email", user.getEmail());
    m.put("phone", user.getPhone() != null ? user.getPhone() : "");
    return ResponseEntity.ok(m);
  }

  @GetMapping("/registration")
  @Transactional(readOnly = true)
  public ResponseEntity<?> myRegistration(Authentication auth) {
    User user = (User) auth.getPrincipal();
    var crewMember = crewMemberRepository.findByUser(user).orElse(null);
    if (crewMember == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Nejste přihlášen k závodu"));
    }
    RacerRegistration reg = crewMember.getRegistration();
    List<Score> scores = scoreRepository.findByRacerRegistrationIdWithCheckpoint(reg.getId());
    int totalPoints = scores.stream().mapToInt(Score::getPoints).sum();
    Edition edition = reg.getEdition();
    int rank = rankingService.computeRacerRank(reg.getId(), edition.getEditionYear());
    int totalRacers = (int) scoreRepository.findByEditionYearWithRacer(edition.getEditionYear()).stream()
        .collect(Collectors.groupingBy(s -> s.getRacerRegistration().getId())).size();

    List<ScoreResponse> scoreList = scores.stream()
        .map(s -> new ScoreResponse(s.getId(), s.getCheckpoint().getName(), s.getCheckpoint().getSortOrder(), s.getPoints(), s.getNote()))
        .toList();

    return ResponseEntity.ok(new StandingResponse(
        reg.getTeamName(), reg.getStartNumber(), totalPoints,
        rank, totalRacers, scoreList,
        reg.getEmail(), reg.getVehiclePlate(), reg.getVehicleCategory()));
  }

  @PutMapping("/registration")
  @Transactional
  public ResponseEntity<?> updateRegistration(Authentication auth, @RequestBody Map<String, Object> body) {
    User user = (User) auth.getPrincipal();
    var crewMember = crewMemberRepository.findByUser(user).orElse(null);
    if (crewMember == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Nejste přihlášen k závodu"));
    }
    RacerRegistration reg = crewMember.getRegistration();

    Integer oldFee = reg.getStartFee();
    if (body.containsKey("variant")) reg.setVariant(toString(body.get("variant")));
    if (body.containsKey("vehicleMake")) reg.setVehicleMake(toString(body.get("vehicleMake")));
    if (body.containsKey("vehicleYear")) reg.setVehicleYear(toInt(body.get("vehicleYear")));
    if (body.containsKey("crewCount")) reg.setCrewCount(toInt(body.get("crewCount")));
    if (body.containsKey("firstTime")) reg.setFirstTime(toBoolean(body.get("firstTime")));
    if (body.containsKey("gender")) reg.setGender(toString(body.get("gender")));
    if (body.containsKey("driverAge")) reg.setDriverAge(toInt(body.get("driverAge")));
    if (body.containsKey("club")) reg.setClub(toString(body.get("club")));
    if (body.containsKey("address")) reg.setAddress(toString(body.get("address")));
    if (body.containsKey("youngestAge")) reg.setYoungestAge(toInt(body.get("youngestAge")));
    if (body.containsKey("youngestName")) reg.setYoungestName(toString(body.get("youngestName")));
    if (body.containsKey("engineDisplacement")) reg.setEngineDisplacement(toInt(body.get("engineDisplacement")));
    if (body.containsKey("power")) reg.setPower(toInt(body.get("power")));
    if (body.containsKey("maxSpeed")) reg.setMaxSpeed(toInt(body.get("maxSpeed")));
    if (body.containsKey("vehicleNotes")) reg.setVehicleNotes(toString(body.get("vehicleNotes")));

    int newFee = registrationService.recalculateFee(reg);
    String message = null;
    boolean feeChanged = false;

    if (newFee != oldFee) {
      feeChanged = true;
      reg.setStartFee(newFee);
      Integer paid = reg.getPaidAmount() != null ? reg.getPaidAmount() : 0;
      if (newFee > paid) {
        if ("PAID".equals(reg.getStatus())) reg.setPaidAt(null);
        reg.setStatus("PENDING");
        message = "Vaše přihláška byla pozastavena, protože jste provedl změnu údajů a je potřeba doplatit "
            + (newFee - paid) + " Kč.";
      } else if (newFee < paid) {
        message = "Cena byla snížena. Kontaktujte organizátora pro vrácení přeplatku "
            + (paid - newFee) + " Kč.";
      }
    }

    racerRegistrationRepository.save(reg);

    var m = new LinkedHashMap<String, Object>();
    m.put("startFee", reg.getStartFee());
    m.put("paidAmount", reg.getPaidAmount());
    m.put("status", reg.getStatus());
    m.put("variant", reg.getVariant());
    m.put("vehicleMake", reg.getVehicleMake());
    m.put("vehicleYear", reg.getVehicleYear());
    m.put("crewCount", reg.getCrewCount());
    m.put("feeChanged", feeChanged);
    m.put("message", message);
    return ResponseEntity.ok(m);
  }

  @PostMapping("/registration/cancel")
  @Transactional
  public ResponseEntity<?> cancelRegistration(Authentication auth) {
    User user = (User) auth.getPrincipal();
    var crewMember = crewMemberRepository.findByUser(user).orElse(null);
    if (crewMember == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Nejste přihlášen k závodu"));
    }
    RacerRegistration reg = crewMember.getRegistration();
    try {
      registrationService.cancelRegistration(reg);
      var m = new LinkedHashMap<String, Object>();
      m.put("status", "CANCELLED");
      m.put("cancelledAt", reg.getCancelledAt());
      m.put("refundAmount", reg.getRefundAmount());
      return ResponseEntity.ok(m);
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
  }

  @GetMapping("/status")
  public ResponseEntity<?> myStatus(Authentication auth) {
    User user = (User) auth.getPrincipal();
    var crewMember = crewMemberRepository.findByUser(user).orElse(null);
    if (crewMember == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Nejste přihlášen k závodu"));
    }
    RacerRegistration reg = crewMember.getRegistration();
    var m = new LinkedHashMap<String, Object>();
    m.put("id", reg.getId());
    m.put("paymentReference", reg.getPaymentReference());
    m.put("teamName", reg.getTeamName());
    m.put("startNumber", reg.getStartNumber());
    m.put("startFee", reg.getStartFee());
    m.put("paidAmount", reg.getPaidAmount());
    m.put("status", reg.getStatus());
    m.put("variant", reg.getVariant() != null ? reg.getVariant() : "");
    m.put("vehicleCategory", reg.getVehicleCategory());
    m.put("vehiclePlate", reg.getVehiclePlate());
    m.put("vehicleYear", reg.getVehicleYear());
    m.put("vehicleMake", reg.getVehicleMake() != null ? reg.getVehicleMake() : "");
    m.put("crewCount", reg.getCrewCount());
    m.put("approved", reg.getApproved() != null ? reg.getApproved() : false);
    m.put("cancelledAt", reg.getCancelledAt());
    m.put("refundAmount", reg.getRefundAmount());
    return ResponseEntity.ok(m);
  }

  @GetMapping("/schedule")
  public ResponseEntity<?> mySchedule() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) return ResponseEntity.ok(List.of());
    List<ScheduleItem> items = scheduleItemRepository.findByEditionOrderBySortOrder(edition);
    return ResponseEntity.ok(items.stream()
        .map(i -> new ScheduleItemResponse(i.getId(), i.getTime(), i.getLabel(), i.getDescription(), i.getSortOrder()))
        .toList());
  }

  @GetMapping("/checkpoints")
  @Transactional(readOnly = true)
  public ResponseEntity<?> myCheckpoints() {
    Edition edition = editionService.getCurrentEdition();
    if (edition == null) return ResponseEntity.ok(List.of());
    List<Checkpoint> items = checkpointRepository.findByEditionOrderBySortOrder(edition);
    return ResponseEntity.ok(items.stream().map(c -> new CheckpointResponse(
        c.getId(), c.getName(), c.getLat(), c.getLng(), c.getRadius(),
        c.getSortOrder(), c.getTaskDescription(), c.getMaxPoints(), c.getVolunteers()
    )).toList());
  }

  @GetMapping("/itinerary")
  @Transactional(readOnly = true)
  public ResponseEntity<?> myItinerary(Authentication auth) {
    User user = (User) auth.getPrincipal();
    var crewMember = crewMemberRepository.findByUser(user).orElse(null);
    if (crewMember == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Nejste přihlášen k závodu"));
    }
    RacerRegistration reg = crewMember.getRegistration();
    Edition edition = reg.getEdition();

    List<ScheduleItem> scheduleItems = scheduleItemRepository.findByEditionOrderBySortOrder(edition);
    List<ScheduleItemResponse> schedule = scheduleItems.stream()
        .map(i -> new ScheduleItemResponse(i.getId(), i.getTime(), i.getLabel(), i.getDescription(), i.getSortOrder()))
        .toList();

    List<Score> scores = scoreRepository.findByRacerRegistrationIdWithCheckpoint(reg.getId());
    Map<Integer, Integer> scoreMap = scores.stream()
        .collect(Collectors.groupingBy(s -> s.getCheckpoint().getSortOrder(), Collectors.summingInt(Score::getPoints)));

    List<Checkpoint> allCheckpoints = checkpointRepository.findByEditionOrderBySortOrder(edition);
    int passedCount = 0;
    List<ItineraryCheckpointData> checkpointData = new ArrayList<>();
    for (Checkpoint cp : allCheckpoints) {
      int order = cp.getSortOrder() != null ? cp.getSortOrder() : 0;
      Integer pts = scoreMap.get(order);
      if (pts != null) passedCount++;
      checkpointData.add(new ItineraryCheckpointData(
          cp.getName(), order, cp.getMaxPoints(), pts, cp.getPhone(), cp.getTaskDescription(), cp.getVolunteers()));
    }

    String variant = reg.getVariant();
    ItineraryRouteData routeData = null;
    if (variant != null) {
      Route route = routeRepository.findByEditionAndVariant(edition, variant)
          .filter(Route::getPublished).orElse(null);
      if (route != null) {
        int pointCount = routePointRepository.findByRouteOrderBySortOrder(route).size();
        routeData = new ItineraryRouteData(route.getName(),
            route.getTotalDistance() != null ? route.getTotalDistance() : 0, pointCount);
      }
    }

    ItineraryContactData contactData = null;
    if (edition.getTowPhone() != null || edition.getTowNote() != null) {
      contactData = new ItineraryContactData(edition.getTowPhone(), edition.getTowNote());
    }

    int remainingCount = allCheckpoints.size() - passedCount;
    return ResponseEntity.ok(new ItineraryResponse(
        reg.getTeamName(), reg.getStartNumber(), schedule, checkpointData,
        passedCount, remainingCount, routeData, contactData));
  }

  @GetMapping("/map")
  @Transactional(readOnly = true)
  public ResponseEntity<?> myMap(Authentication auth) {
    User user = (User) auth.getPrincipal();
    var crewMember = crewMemberRepository.findByUser(user).orElse(null);
    if (crewMember == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Nejste přihlášen k závodu"));
    }
    RacerRegistration reg = crewMember.getRegistration();
    Edition edition = reg.getEdition();
    String variant = reg.getVariant();

    Route route = null;
    if (variant != null) {
      route = routeRepository.findByEditionAndVariant(edition, variant)
          .filter(Route::getPublished).orElse(null);
    }

    List<RoutePointData> routePoints = Collections.emptyList();
    double totalDistance = 0;
    if (route != null) {
      routePoints = routePointRepository.findByRouteOrderBySortOrder(route).stream()
          .map(rp -> new RoutePointData(rp.getLat(), rp.getLng(), rp.getDistanceFromStart()))
          .toList();
      totalDistance = route.getTotalDistance() != null ? route.getTotalDistance() : 0;
    }

    List<Checkpoint> checkpoints = checkpointRepository.findByEditionOrderBySortOrder(edition);
    List<Score> scores = scoreRepository.findByRacerRegistrationIdWithCheckpoint(reg.getId());
    Map<Integer, Integer> scoreMap = scores.stream()
        .collect(Collectors.groupingBy(s -> s.getCheckpoint().getSortOrder(), Collectors.summingInt(Score::getPoints)));

    List<CheckpointScoreData> checkpointData = new ArrayList<>();
    for (Checkpoint cp : checkpoints) {
      int runNum = cp.getSortOrder() != null ? cp.getSortOrder() : 0;
      Integer pts = scoreMap.get(runNum);
      checkpointData.add(new CheckpointScoreData(
          cp.getName(), cp.getLat(), cp.getLng(), cp.getRadius(),
          cp.getSortOrder(), cp.getTaskDescription(), cp.getMaxPoints(), pts));
    }

    List<CheckpointScoreData> visibleCheckpoints = checkpointData.stream()
        .filter(cd -> cd.scorePoints() != null).toList();

    List<RoutePointData> visibleRoutePoints;
    if (!visibleCheckpoints.isEmpty() && !routePoints.isEmpty()) {
      CheckpointScoreData last = visibleCheckpoints.get(visibleCheckpoints.size() - 1);
      int bestIdx = 0;
      double bestDist = Double.MAX_VALUE;
      for (int i = 0; i < routePoints.size(); i++) {
        RoutePointData rp = routePoints.get(i);
        double d = Math.pow(rp.lat() - last.lat(), 2) + Math.pow(rp.lng() - last.lng(), 2);
        if (d < bestDist) { bestDist = d; bestIdx = i; }
      }
      visibleRoutePoints = routePoints.subList(0, bestIdx + 1);
    } else {
      visibleRoutePoints = List.of();
    }

    int totalScore = scores.stream().mapToInt(Score::getPoints).sum();
    int rank = rankingService.computeRacerRank(reg.getId(), edition.getEditionYear());
    int totalRacers = (int) scoreRepository.findByEditionYearWithRacer(edition.getEditionYear()).stream()
        .collect(Collectors.groupingBy(s -> s.getRacerRegistration().getId())).size();

    return ResponseEntity.ok(new RacerMapResponse(
        visibleRoutePoints, visibleCheckpoints, totalDistance,
        totalScore, rank, totalRacers,
        reg.getTeamName(), reg.getStartNumber()));
  }

  private Integer toInt(Object v) {
    if (v == null) return null;
    if (v instanceof Number n) return n.intValue();
    try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
  }

  private String toString(Object v) {
    if (v == null) return null;
    if (v instanceof String s) return s;
    return v.toString();
  }

  private Boolean toBoolean(Object v) {
    if (v == null) return null;
    if (v instanceof Boolean b) return b;
    return "true".equalsIgnoreCase(v.toString());
  }

  public record ScoreResponse(Long id, String checkpointName, Integer checkpointOrder, Integer points, String note) {}
  public record ScheduleItemResponse(Long id, String time, String label, String description, Integer sortOrder) {}
  public record StandingResponse(String teamName, Integer startNumber, int totalPoints, int rank, int totalRacers, List<ScoreResponse> scores, String email, String vehiclePlate, String vehicleCategory) {}
  public record CheckpointResponse(Long id, String name, Double lat, Double lng, Integer radius, Integer sortOrder, String taskDescription, Integer maxPoints, List<String> volunteers) {}
  public record RoutePointData(double lat, double lng, Double distanceFromStart) {}
  public record CheckpointScoreData(String name, Double lat, Double lng, Integer radius, Integer sortOrder, String taskDescription, Integer maxPoints, Integer scorePoints) {}
  public record RacerMapResponse(List<RoutePointData> routePoints, List<CheckpointScoreData> checkpoints, double totalDistance, int totalScore, int rank, int totalRacers, String teamName, Integer startNumber) {}
  public record ItineraryCheckpointData(String name, Integer sortOrder, Integer maxPoints, Integer scorePoints, String phone, String taskDescription, List<String> volunteers) {}
  public record ItineraryRouteData(String name, double totalDistance, int pointCount) {}
  public record ItineraryContactData(String towPhone, String towNote) {}
  public record ItineraryResponse(String teamName, Integer startNumber, List<ScheduleItemResponse> schedule, List<ItineraryCheckpointData> checkpoints, int passedCount, int remainingCount, ItineraryRouteData route, ItineraryContactData contact) {}
}
