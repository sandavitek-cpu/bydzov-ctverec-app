package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.Route;
import cz.previt.bydzovctverec.domain.RoutePoint;
import java.util.List;

public record RouteResponse(
    long id, String variant, String name,
    double totalDistance, boolean published,
    int avgSpeedKmph,
    List<RoutePointResponse> points) {

  public static RouteResponse from(Route route, List<RoutePoint> points) {
    var pts = points.stream()
        .map(p -> new RoutePointResponse(
            p.getId(), p.getSortOrder(), p.getLat(), p.getLng(), p.getDistanceFromStart()))
        .toList();
    return new RouteResponse(route.getId(), route.getVariant(), route.getName(),
        route.getTotalDistance(), Boolean.TRUE.equals(route.getPublished()),
        route.getAvgSpeedKmph() != null ? route.getAvgSpeedKmph() : 30, pts);
  }
}
