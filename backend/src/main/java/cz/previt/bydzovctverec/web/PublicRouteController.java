package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.Route;
import cz.previt.bydzovctverec.domain.RoutePointRepository;
import cz.previt.bydzovctverec.domain.RouteRepository;
import cz.previt.bydzovctverec.service.EditionService;
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

  private final EditionService editionService;
  private final RouteRepository routeRepository;
  private final RoutePointRepository routePointRepository;

  public PublicRouteController(EditionService editionService,
      RouteRepository routeRepository,
      RoutePointRepository routePointRepository) {
    this.editionService = editionService;
    this.routeRepository = routeRepository;
    this.routePointRepository = routePointRepository;
  }

  @GetMapping("/{year}")
  public ResponseEntity<?> getPublishedRoutes(@PathVariable Integer year) {
    Edition edition = editionService.getByYear(year);
    if (edition == null) {
      return ResponseEntity.ok(ApiResponse.ok(List.of()));
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
    Edition edition = editionService.getByYear(year);
    if (edition == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Ročník nenalezen"));
    }
    var route = routeRepository.findByEditionAndVariant(edition, variant);
    if (route.isEmpty() || !Boolean.TRUE.equals(route.get().getPublished())) {
      return ResponseEntity.ok(Map.of());
    }
    var points = routePointRepository.findByRouteOrderBySortOrder(route.get());
    return ResponseEntity.ok(RouteResponse.from(route.get(), points));
  }
}
