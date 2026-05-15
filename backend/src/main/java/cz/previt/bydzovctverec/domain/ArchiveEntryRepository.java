package cz.previt.bydzovctverec.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchiveEntryRepository extends JpaRepository<ArchiveEntry, Long> {

  List<ArchiveEntry> findAllByOrderByEditionYearDescRankAsc();

  List<ArchiveEntry> findByEditionYearOrderByRankAsc(Integer editionYear);
}
