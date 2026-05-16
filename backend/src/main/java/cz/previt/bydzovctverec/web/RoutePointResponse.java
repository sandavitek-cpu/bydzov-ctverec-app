package cz.previt.bydzovctverec.web;

public record RoutePointResponse(Long id, int sortOrder, double lat, double lng, double distanceFromStart) {}
