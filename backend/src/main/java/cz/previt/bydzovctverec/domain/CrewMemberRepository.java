package cz.previt.bydzovctverec.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {

  List<CrewMember> findByRegistration(RacerRegistration registration);

  List<CrewMember> findByRegistrationIn(List<RacerRegistration> registrations);

  Optional<CrewMember> findByUser(User user);
}
