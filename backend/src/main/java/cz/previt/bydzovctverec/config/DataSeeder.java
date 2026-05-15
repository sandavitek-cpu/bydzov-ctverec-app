package cz.previt.bydzovctverec.config;

import cz.previt.bydzovctverec.domain.Checkpoint;
import cz.previt.bydzovctverec.domain.CheckpointRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.Role;
import cz.previt.bydzovctverec.domain.ScheduleItem;
import cz.previt.bydzovctverec.domain.ScheduleItemRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

  private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

  @Bean
  CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder encoder) {
    return args -> {
      if (userRepository.findByEmail("admin@bydzov-ctverec.cz").isEmpty()) {
        userRepository.save(new User(
            "admin@bydzov-ctverec.cz",
            encoder.encode("admin123"),
            Role.ADMIN,
            "Správce",
            Instant.now()));
        log.info("Admin user created (admin@bydzov-ctverec.cz / admin123)");
      }
    };
  }

  @Bean
  CommandLineRunner seedJudge(UserRepository userRepository, PasswordEncoder encoder) {
    return args -> {
      if (userRepository.findByEmail("judge@bydzov-ctverec.cz").isEmpty()) {
        userRepository.save(new User(
            "judge@bydzov-ctverec.cz",
            encoder.encode("judge123"),
            Role.JUDGE,
            "Rozhodčí",
            Instant.now()));
        log.info("Judge user created (judge@bydzov-ctverec.cz / judge123)");
      }
    };
  }

  @Bean
  CommandLineRunner seedRacer(UserRepository userRepository, PasswordEncoder encoder) {
    return args -> {
      if (userRepository.findByEmail("racer@bydzov-ctverec.cz").isEmpty()) {
        userRepository.save(new User(
            "racer@bydzov-ctverec.cz",
            encoder.encode("racer123"),
            Role.RACER,
            "Testovací jezdec",
            Instant.now()));
        log.info("Racer user created (racer@bydzov-ctverec.cz / racer123)");
      }
    };
  }

  @Bean
  @org.springframework.core.annotation.Order(1)
  CommandLineRunner seedEdition(EditionRepository editionRepository) {
    return args -> {
      try {
        var existing = editionRepository.findByEditionYear(2026);
        if (existing.isEmpty()) {
          editionRepository.save(new Edition(2026, "30. ročník Novobydžovského čtverce"));
          log.info("Edition 2026 seeded");
        } else {
          log.info("Edition 2026 exists (id={})", existing.get().getId());
        }
      } catch (Exception e) {
        log.error("seedEdition failed: {}", e.getMessage());
      }
    };
  }

  @Bean
  @org.springframework.core.annotation.Order(2)
  CommandLineRunner seedSchedule(EditionRepository editionRepository, ScheduleItemRepository scheduleItemRepository) {
    return args -> {
      Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
      if (edition == null) return;
      var existing = scheduleItemRepository.findByEditionOrderBySortOrder(edition);
      if (!existing.isEmpty()) return;
      scheduleItemRepository.saveAll(List.of(
          new ScheduleItem(edition, "08:00", "Prezence", "Kontrola dokladů, převzetí startovního balíčku", 1),
          new ScheduleItem(edition, "09:00", "Briefing", "Povinná schůzka jezdců", 2),
          new ScheduleItem(edition, "09:15", "1. kolo", "Start prvního měřeného úseku", 3),
          new ScheduleItem(edition, "12:00", "Oběd", "Přestávka na občerstvení", 4),
          new ScheduleItem(edition, "13:00", "2. kolo", "Start druhého měřeného úseku", 5),
          new ScheduleItem(edition, "16:00", "Dojezd", "Ukončení závodu, odevzdání karet", 6),
          new ScheduleItem(edition, "17:00", "Vyhlášení výsledků", "Slavnostní ceremoniál", 7)));
      log.info("Schedule for 2026 seeded");
    };
  }

  @Bean
  @org.springframework.core.annotation.Order(3)
  CommandLineRunner seedCheckpoints(EditionRepository editionRepository, CheckpointRepository checkpointRepository) {
    return args -> {
      Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
      if (edition == null) return;
      var existing = checkpointRepository.findByEditionOrderBySortOrder(edition);
      if (!existing.isEmpty()) return;
      checkpointRepository.saveAll(List.of(
          new Checkpoint(edition, "Prezence / Start", 50.2415, 15.4900, 300, 1),
          new Checkpoint(edition, "Stanoviště 1 — Chlumec", 50.2280, 15.4750, 500, 2),
          new Checkpoint(edition, "Stanoviště 2 — Hlušice", 50.2150, 15.4600, 500, 3),
          new Checkpoint(edition, "Stanoviště 3 — Sloupno", 50.2520, 15.5050, 500, 4),
          new Checkpoint(edition, "Dojezd / Cíl", 50.2415, 15.4900, 300, 5)));
      log.info("Checkpoints for 2026 seeded");
    };
  }
}
