package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.Route;
import cz.previt.bydzovctverec.domain.RoutePointRepository;
import cz.previt.bydzovctverec.domain.RouteRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/routes")
public class PublicRouteController {

  private final EditionRepository editionRepository;
  private final RouteRepository routeRepository;
  private final RoutePointRepository routePointRepository;

  public PublicRouteController(EditionRepository editionRepository,
      RouteRepository routeRepository,
      RoutePointRepository routePointRepository) {
    this.editionRepository = editionRepository;
    this.routeRepository = routeRepository;
    this.routePointRepository = routePointRepository;
  }

  @GetMapping("/{year}")
  public ResponseEntity<?> getPublishedRoutes(@PathVariable Integer year) {
    Edition edition = editionRepository.findByEditionYear(year).orElse(null);
    if (edition == null) {
      return ResponseEntity.ok(List.of());
    }
    var routes = routeRepository.findByEditionAndPublishedTrue(edition);
    var result = routes.stream().map(r -> {
      var points = routePointRepository.findByRouteOrderBySortOrder(r);
      return RouteResponse.from(r, points);
    }).toList();
    return ResponseEntity.ok(result);
  }

  @GetMapping("/{year}/variant/{variant}")
  public ResponseEntity<?> getRouteByVariant(@PathVariable Integer year,
      @PathVariable String variant) {
    Edition edition = editionRepository.findByEditionYear(year).orElse(null);
    if (edition == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Ročník nenalezen"));
    }
    var route = routeRepository.findByEditionAndVariant(edition, variant);
    if (route.isEmpty() || !Boolean.TRUE.equals(route.get().getPublished())) {
      return ResponseEntity.ok(Map.of());
    }
    var points = routePointRepository.findByRouteOrderBySortOrder(route.get());
    return ResponseEntity.ok(RouteResponse.from(route.get(), points));
  }
}
