package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.Route;
import cz.previt.bydzovctverec.domain.RoutePoint;
import cz.previt.bydzovctverec.domain.RoutePointRepository;
import cz.previt.bydzovctverec.domain.RouteRepository;
import java.time.Instant;
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
@RequestMapping("/api/admin/routes")
public class AdminRouteController {

  private final EditionRepository editionRepository;
  private final RouteRepository routeRepository;
  private final RoutePointRepository routePointRepository;

  public AdminRouteController(EditionRepository editionRepository,
      RouteRepository routeRepository,
      RoutePointRepository routePointRepository) {
    this.editionRepository = editionRepository;
    this.routeRepository = routeRepository;
    this.routePointRepository = routePointRepository;
  }

  private Edition currentEdition() {
    return editionRepository.findTopByOrderByEditionYearDesc()
        .orElseGet(() -> editionRepository.save(new Edition(2026, "30. ročník Novobydžovského čtverce – Memoriál Elišky Junkové")));
  }

  @GetMapping
  public ResponseEntity<?> list() {
    Edition edition = currentEdition();
    List<Route> routes = routeRepository.findByEditionOrderByName(edition);
    var result = routes.stream().map(r -> {
      var points = routePointRepository.findByRouteOrderBySortOrder(r);
      return RouteResponse.from(r, points);
    }).toList();
    return ResponseEntity.ok(result);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> get(@PathVariable Long id) {
    Route route = routeRepository.findById(id).orElse(null);
    if (route == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Trasa nenalezena"));
    }
    var points = routePointRepository.findByRouteOrderBySortOrder(route);
    return ResponseEntity.ok(RouteResponse.from(route, points));
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
    String variant = body.get("variant");
    String name = body.get("name");
    if (variant == null || variant.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Varianta je povinná"));
    }
    if (name == null) name = switch (variant) {
      case "JEDNODENNI" -> "Jednodenní trasa";
      case "DVODENNI" -> "Dvoudenní trasa";
      default -> "Trasa";
    };
    Edition edition = currentEdition();
    var existing = routeRepository.findByEditionAndVariant(edition, variant);
    if (existing.isPresent()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Trasa pro tuto variantu již existuje"));
    }
    Route route = new Route(edition, variant, name, Instant.now());
    routeRepository.save(route);
    return ResponseEntity.ok(RouteResponse.from(route, List.of()));
  }

  @PutMapping("/{id}")
  @Transactional
  public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
    Route route = routeRepository.findById(id).orElse(null);
    if (route == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Trasa nenalezena"));
    }
    if (body.containsKey("name")) route.setName(body.get("name"));
    if (body.containsKey("avgSpeedKmph")) {
      try { route.setAvgSpeedKmph(Integer.parseInt(body.get("avgSpeedKmph"))); } catch (NumberFormatException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Neplatná hodnota pro průměrnou rychlost"));
      }
    }
    routeRepository.save(route);
    var points = routePointRepository.findByRouteOrderBySortOrder(route);
    return ResponseEntity.ok(RouteResponse.from(route, points));
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> delete(@PathVariable Long id) {
    Route route = routeRepository.findById(id).orElse(null);
    if (route == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Trasa nenalezena"));
    }
    var points = routePointRepository.findByRouteOrderBySortOrder(route);
    routePointRepository.deleteAll(points);
    routeRepository.delete(route);
    return ResponseEntity.ok(Map.of("deleted", true));
  }

  @PostMapping("/{id}/publish")
  @Transactional
  public ResponseEntity<?> publish(@PathVariable Long id) {
    Route route = routeRepository.findById(id).orElse(null);
    if (route == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Trasa nenalezena"));
    }
    route.setPublished(true);
    routeRepository.save(route);
    var points = routePointRepository.findByRouteOrderBySortOrder(route);
    return ResponseEntity.ok(RouteResponse.from(route, points));
  }

  @PostMapping("/{id}/points")
  @Transactional
  public ResponseEntity<?> addPoint(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    Route route = routeRepository.findById(id).orElse(null);
    if (route == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Trasa nenalezena"));
    }
    double lat = toDouble(body.get("lat"));
    double lng = toDouble(body.get("lng"));
    if (lat == 0 && lng == 0) {
      return ResponseEntity.badRequest().body(Map.of("error", "Neplatné souřadnice"));
    }
    var existing = routePointRepository.findByRouteOrderBySortOrder(route);
    int nextOrder = existing.size() + 1;
    RoutePoint point = new RoutePoint(route, nextOrder, lat, lng);

    if (!existing.isEmpty()) {
      RoutePoint last = existing.get(existing.size() - 1);
      double dist = haversine(last.getLat(), last.getLng(), lat, lng);
      point.setDistanceFromStart(last.getDistanceFromStart() + dist);
    }

    routePointRepository.save(point);

    recalculateDistances(route);

    var all = routePointRepository.findByRouteOrderBySortOrder(route);
    return ResponseEntity.ok(RouteResponse.from(route, all));
  }

  @PutMapping("/{id}/points/{pointId}")
  @Transactional
  public ResponseEntity<?> updatePoint(@PathVariable Long id, @PathVariable Long pointId,
      @RequestBody Map<String, Object> body) {
    Route route = routeRepository.findById(id).orElse(null);
    if (route == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Trasa nenalezena"));
    }
    RoutePoint point = routePointRepository.findById(pointId).orElse(null);
    if (point == null || !point.getRoute().getId().equals(route.getId())) {
      return ResponseEntity.badRequest().body(Map.of("error", "Bod nenalezen"));
    }
    if (body.containsKey("lat")) point.setLat(toDouble(body.get("lat")));
    if (body.containsKey("lng")) point.setLng(toDouble(body.get("lng")));
    if (body.containsKey("sortOrder")) point.setSortOrder((Integer) body.get("sortOrder"));
    routePointRepository.save(point);

    recalculateDistances(route);

    var all = routePointRepository.findByRouteOrderBySortOrder(route);
    return ResponseEntity.ok(RouteResponse.from(route, all));
  }

  @DeleteMapping("/{id}/points/{pointId}")
  @Transactional
  public ResponseEntity<?> removePoint(@PathVariable Long id, @PathVariable Long pointId) {
    Route route = routeRepository.findById(id).orElse(null);
    if (route == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Trasa nenalezena"));
    }
    routePointRepository.deleteById(pointId);

    var remaining = routePointRepository.findByRouteOrderBySortOrder(route);
    for (int i = 0; i < remaining.size(); i++) {
      remaining.get(i).setSortOrder(i + 1);
    }
    routePointRepository.saveAll(remaining);

    recalculateDistances(route);

    var all = routePointRepository.findByRouteOrderBySortOrder(route);
    return ResponseEntity.ok(RouteResponse.from(route, all));
  }

  private void recalculateDistances(Route route) {
    var points = routePointRepository.findByRouteOrderBySortOrder(route);
    double cumulative = 0;
    for (int i = 0; i < points.size(); i++) {
      points.get(i).setDistanceFromStart(cumulative);
      if (i < points.size() - 1) {
        RoutePoint cur = points.get(i);
        RoutePoint next = points.get(i + 1);
        cumulative += haversine(cur.getLat(), cur.getLng(), next.getLat(), next.getLng());
      }
    }
    routePointRepository.saveAll(points);
    route.setTotalDistance(cumulative);
    routeRepository.save(route);
  }

  private double haversine(double lat1, double lon1, double lat2, double lon2) {
    double R = 6371e3;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  private double toDouble(Object v) {
    if (v == null) return 0;
    if (v instanceof Number n) return n.doubleValue();
    try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0; }
  }
}
