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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/racer")
public class RacerController {

  private final RacerRegistrationRepository racerRegistrationRepository;
  private final ScoreRepository scoreRepository;
  private final EditionRepository editionRepository;
  private final ScheduleItemRepository scheduleItemRepository;
  private final CheckpointRepository checkpointRepository;
  private final RouteRepository routeRepository;
  private final RoutePointRepository routePointRepository;
  private final CrewMemberRepository crewMemberRepository;

  public RacerController(RacerRegistrationRepository racerRegistrationRepository, ScoreRepository scoreRepository, EditionRepository editionRepository, ScheduleItemRepository scheduleItemRepository, CheckpointRepository checkpointRepository, RouteRepository routeRepository, RoutePointRepository routePointRepository, CrewMemberRepository crewMemberRepository) {
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.scoreRepository = scoreRepository;
    this.editionRepository = editionRepository;
    this.scheduleItemRepository = scheduleItemRepository;
    this.checkpointRepository = checkpointRepository;
    this.routeRepository = routeRepository;
    this.routePointRepository = routePointRepository;
    this.crewMemberRepository = crewMemberRepository;
  }

  @GetMapping("/registration")
  @Transactional(readOnly = true)
  public ResponseEntity<?> myRegistration(Authentication auth) {
    User user = (User) auth.getPrincipal();
    var crewMember = crewMemberRepository.findByUser(user).orElse(null);
    if (crewMember == null) {
      return ResponseEntity.ok(Map.of("error", "Nejste přihlášen k závodu"));
    }
    RacerRegistration reg = crewMember.getRegistration();

    List<Score> scores = scoreRepository.findByRacerRegistrationIdWithCheckpoint(reg.getId());
    int totalPoints = scores.stream().mapToInt(Score::getPoints).sum();
    Edition edition = reg.getEdition();
    List<Score> allScores = scoreRepository.findByEditionYearWithRacer(edition.getEditionYear());
    Map<Long, Integer> totals = allScores.stream()
        .collect(Collectors.groupingBy(s -> s.getRacerRegistration().getId(), Collectors.summingInt(Score::getPoints)));
    List<Map.Entry<Long, Integer>> sorted = totals.entrySet().stream()
        .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed()).toList();
    int rank = 1;
    for (var entry : sorted) { if (entry.getKey().equals(reg.getId())) break; rank++; }

    List<ScoreResponse> scoreList = scores.stream()
        .map(s -> new ScoreResponse(s.getId(), s.getCheckpoint().getName(), s.getCheckpoint().getSortOrder(), s.getPoints(), s.getNote()))
        .toList();

    return ResponseEntity.ok(new StandingResponse(
        reg.getTeamName(), reg.getStartNumber(), totalPoints,
        rank, totals.size(), scoreList,
        reg.getEmail(), reg.getVehiclePlate(), reg.getVehicleCategory()));
  }

  @GetMapping("/status")
  public ResponseEntity<?> myStatus(Authentication auth) {
    User user = (User) auth.getPrincipal();
    var crewMember = crewMemberRepository.findByUser(user).orElse(null);
    if (crewMember == null) {
      return ResponseEntity.ok(Map.of("error", "Nejste přihlášen k závodu"));
    }
    RacerRegistration reg = crewMember.getRegistration();
    var m = new java.util.LinkedHashMap<String, Object>();
    m.put("id", reg.getId());
    m.put("paymentReference", reg.getPaymentReference());
    m.put("teamName", reg.getTeamName());
    m.put("startNumber", reg.getStartNumber());
    m.put("startFee", reg.getStartFee());
    m.put("status", reg.getStatus());
    m.put("variant", reg.getVariant() != null ? reg.getVariant() : "");
    m.put("vehicleCategory", reg.getVehicleCategory());
    m.put("vehiclePlate", reg.getVehiclePlate());
    m.put("vehicleYear", reg.getVehicleYear());
    m.put("vehicleMake", reg.getVehicleMake() != null ? reg.getVehicleMake() : "");
    m.put("crewCount", reg.getCrewCount());
    m.put("approved", reg.getApproved() != null ? reg.getApproved() : false);
    return ResponseEntity.ok(m);
  }

  @GetMapping("/schedule")
  public ResponseEntity<?> mySchedule() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      return ResponseEntity.ok(List.of());
    }
    List<ScheduleItem> items = scheduleItemRepository.findByEditionOrderBySortOrder(edition);
    return ResponseEntity.ok(items.stream().map(i -> new ScheduleItemResponse(i.getTime(), i.getLabel(), i.getDescription())).toList());
  }

  @GetMapping("/checkpoints")
  @Transactional(readOnly = true)
  public ResponseEntity<?> myCheckpoints() {
    Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
    if (edition == null) {
      return ResponseEntity.ok(List.of());
    }
    List<Checkpoint> items = checkpointRepository.findByEditionOrderBySortOrder(edition);
    return ResponseEntity.ok(items.stream().map(c -> new CheckpointResponse(
        c.getId(), c.getName(), c.getLat(), c.getLng(), c.getRadius(),
        c.getSortOrder(), c.getTaskDescription(), c.getMaxPoints(), c.getVolunteers()
    )).toList());
  }

  @GetMapping("/map")
  @Transactional(readOnly = true)
  public ResponseEntity<?> myMap(Authentication auth) {
    User user = (User) auth.getPrincipal();
    var crewMember = crewMemberRepository.findByUser(user).orElse(null);
    if (crewMember == null) {
      return ResponseEntity.ok(Map.of("error", "Nejste přihlášen k závodu"));
    }
    RacerRegistration reg = crewMember.getRegistration();

    Edition edition = reg.getEdition();
    String variant = reg.getVariant();

    Route route = null;
    if (variant != null) {
      route = routeRepository.findByEditionAndVariant(edition, variant)
          .filter(Route::getPublished)
          .orElse(null);
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

    int totalScore = scores.stream().mapToInt(Score::getPoints).sum();

    List<Score> allScores = scoreRepository.findByEditionYearWithRacer(edition.getEditionYear());
    Map<Long, Integer> totals = allScores.stream()
        .collect(Collectors.groupingBy(s -> s.getRacerRegistration().getId(), Collectors.summingInt(Score::getPoints)));
    List<Map.Entry<Long, Integer>> sorted = totals.entrySet().stream()
        .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed()).toList();
    int rank = 1;
    for (var entry : sorted) {
      if (entry.getKey().equals(reg.getId())) break;
      rank++;
    }

    return ResponseEntity.ok(new RacerMapResponse(
        routePoints, checkpointData, totalDistance,
        totalScore, rank, sorted.size(),
        reg.getTeamName(), reg.getStartNumber()));
  }

  public record ScoreResponse(Long id, String checkpointName, Integer checkpointOrder, Integer points, String note) {}
  public record ScheduleItemResponse(String time, String label, String description) {}
  public record StandingResponse(String teamName, Integer startNumber, int totalPoints, int rank, int totalRacers, List<ScoreResponse> scores, String email, String vehiclePlate, String vehicleCategory) {}
  public record CheckpointResponse(Long id, String name, Double lat, Double lng, Integer radius, Integer sortOrder, String taskDescription, Integer maxPoints, List<String> volunteers) {}
  public record RoutePointData(double lat, double lng, Double distanceFromStart) {}
  public record CheckpointScoreData(String name, Double lat, Double lng, Integer radius, Integer sortOrder, String taskDescription, Integer maxPoints, Integer scorePoints) {}
  public record RacerMapResponse(List<RoutePointData> routePoints, List<CheckpointScoreData> checkpoints, double totalDistance, int totalScore, int rank, int totalRacers, String teamName, Integer startNumber) {}
}
