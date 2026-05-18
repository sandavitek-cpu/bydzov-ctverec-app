package cz.previt.bydzovctverec.web;

import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.Vehicle;
import cz.previt.bydzovctverec.domain.VehicleRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/racer/vehicles")
public class VehicleController {

  private final VehicleRepository vehicleRepository;

  public VehicleController(VehicleRepository vehicleRepository) {
    this.vehicleRepository = vehicleRepository;
  }

  @GetMapping
  public ResponseEntity<?> list(Authentication auth) {
    User user = (User) auth.getPrincipal();
    List<Vehicle> vehicles = vehicleRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    return ResponseEntity.ok(vehicles.stream().map(VehicleController::toMap).toList());
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> create(Authentication auth, @RequestBody Map<String, Object> body) {
    User user = (User) auth.getPrincipal();
    Vehicle v = new Vehicle(user,
        (String) body.get("vehicleMake"),
        (String) body.get("vehiclePlate"),
        toInt(body.get("vehicleYear")),
        (String) body.get("vehicleCategory"),
        toInt(body.get("engineDisplacement")),
        toInt(body.get("power")),
        toInt(body.get("maxSpeed")),
        (String) body.get("vehicleNotes"),
        Instant.now());
    vehicleRepository.save(v);
    return ResponseEntity.ok(toMap(v));
  }

  @PutMapping("/{id}")
  @Transactional
  public ResponseEntity<?> update(Authentication auth, @PathVariable Long id,
      @RequestBody Map<String, Object> body) {
    User user = (User) auth.getPrincipal();
    Vehicle v = vehicleRepository.findById(id).orElse(null);
    if (v == null || !v.getUser().getId().equals(user.getId())) {
      return ResponseEntity.badRequest().body(Map.of("error", "Vozidlo nenalezeno"));
    }
    if (body.containsKey("vehicleMake")) v.setVehicleMake((String) body.get("vehicleMake"));
    if (body.containsKey("vehiclePlate")) v.setVehiclePlate((String) body.get("vehiclePlate"));
    if (body.containsKey("vehicleYear")) v.setVehicleYear(toInt(body.get("vehicleYear")));
    if (body.containsKey("vehicleCategory")) v.setVehicleCategory((String) body.get("vehicleCategory"));
    if (body.containsKey("engineDisplacement")) v.setEngineDisplacement(toInt(body.get("engineDisplacement")));
    if (body.containsKey("power")) v.setPower(toInt(body.get("power")));
    if (body.containsKey("maxSpeed")) v.setMaxSpeed(toInt(body.get("maxSpeed")));
    if (body.containsKey("vehicleNotes")) v.setVehicleNotes((String) body.get("vehicleNotes"));
    vehicleRepository.save(v);
    return ResponseEntity.ok(toMap(v));
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
    User user = (User) auth.getPrincipal();
    Vehicle v = vehicleRepository.findById(id).orElse(null);
    if (v == null || !v.getUser().getId().equals(user.getId())) {
      return ResponseEntity.badRequest().body(Map.of("error", "Vozidlo nenalezeno"));
    }
    vehicleRepository.delete(v);
    return ResponseEntity.ok(Map.of("deleted", true));
  }

  private static Map<String, Object> toMap(Vehicle v) {
    var m = new LinkedHashMap<String, Object>();
    m.put("id", v.getId());
    m.put("vehicleMake", v.getVehicleMake());
    m.put("vehiclePlate", v.getVehiclePlate());
    m.put("vehicleYear", v.getVehicleYear());
    m.put("vehicleCategory", v.getVehicleCategory());
    m.put("engineDisplacement", v.getEngineDisplacement());
    m.put("power", v.getPower());
    m.put("maxSpeed", v.getMaxSpeed());
    m.put("vehicleNotes", v.getVehicleNotes());
    m.put("createdAt", v.getCreatedAt());
    return m;
  }

  private static Integer toInt(Object v) {
    if (v == null) return null;
    if (v instanceof Number n) return n.intValue();
    if (v instanceof String s) {
      if (s.isBlank()) return null;
      try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }
    return null;
  }
}
