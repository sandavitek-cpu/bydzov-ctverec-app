package cz.previt.bydzovctverec.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

  List<Vehicle> findByUserIdOrderByCreatedAtDesc(Long userId);

  void deleteByIdAndUserId(Long id, Long userId);
}
