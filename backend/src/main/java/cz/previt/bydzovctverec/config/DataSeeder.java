package cz.previt.bydzovctverec.config;

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
  CommandLineRunner seedSchedule(EditionRepository editionRepository, ScheduleItemRepository scheduleItemRepository) {
    return args -> {
      Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
      if (edition == null) return;
      if (!scheduleItemRepository.findByEditionOrderBySortOrder(edition).isEmpty()) return;

      List<ScheduleItem> items = List.of(
          new ScheduleItem(edition, "08:00", "Prezence", "Kontrola dokladů, převzetí startovního balíčku", 1),
          new ScheduleItem(edition, "09:00", "Briefing", "Povinná schůzka jezdců", 2),
          new ScheduleItem(edition, "09:15", "1. kolo", "Start prvního měřeného úseku", 3),
          new ScheduleItem(edition, "12:00", "Oběd", "Přestávka na občerstvení", 4),
          new ScheduleItem(edition, "13:00", "2. kolo", "Start druhého měřeného úseku", 5),
          new ScheduleItem(edition, "16:00", "Dojezd", "Ukončení závodu, odevzdání karet", 6),
          new ScheduleItem(edition, "17:00", "Vyhlášení výsledků", "Slavnostní ceremoniál", 7));
      scheduleItemRepository.saveAll(items);
      log.info("Schedule for 2026 seeded ({} items)", items.size());
    };
  }
}
