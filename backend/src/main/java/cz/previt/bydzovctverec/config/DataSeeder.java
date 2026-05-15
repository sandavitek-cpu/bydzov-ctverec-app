package cz.previt.bydzovctverec.config;

import cz.previt.bydzovctverec.domain.AppRole;
import cz.previt.bydzovctverec.domain.AppRoleRepository;
import cz.previt.bydzovctverec.domain.ChangeLogEntry;
import cz.previt.bydzovctverec.domain.ChangeLogEntryRepository;
import cz.previt.bydzovctverec.domain.ArchiveEntry;
import cz.previt.bydzovctverec.domain.ArchiveEntryRepository;
import cz.previt.bydzovctverec.domain.Checkpoint;
import cz.previt.bydzovctverec.domain.CheckpointRepository;
import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.ScheduleItem;
import cz.previt.bydzovctverec.domain.ScheduleItemRepository;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import cz.previt.bydzovctverec.domain.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
  CommandLineRunner seedRoles(AppRoleRepository appRoleRepository) {
    return args -> {
      try {
        if (appRoleRepository.count() > 0) return;
        appRoleRepository.saveAll(List.of(
            new AppRole("ADMIN", "Administrátor", Instant.now()),
            new AppRole("JUDGE", "Komisař", Instant.now()),
            new AppRole("RACER", "Závodník", Instant.now())));
        log.info("Roles seeded");
      } catch (Exception e) {
        log.error("seedRoles failed: {}", e.getMessage());
      }
    };
  }

  @Bean
  CommandLineRunner seedAdmin(UserRepository userRepository, AppRoleRepository appRoleRepository, PasswordEncoder encoder) {
    return args -> {
      try {
        if (userRepository.findByEmail("admin@bydzov-ctverec.cz").isEmpty()) {
          var adminRole = appRoleRepository.findByName("ADMIN").orElse(null);
          var judgeRole = appRoleRepository.findByName("JUDGE").orElse(null);
          var racerRole = appRoleRepository.findByName("RACER").orElse(null);
          Set<AppRole> roles = new java.util.HashSet<>();
          if (adminRole != null) roles.add(adminRole);
          if (judgeRole != null) roles.add(judgeRole);
          User user = new User(
              "admin@bydzov-ctverec.cz", "admin",
              encoder.encode("admin123"),
              UserRole.ADMIN,
              "Správce", "",
              Instant.now());
          user.getAppRoles().addAll(roles);
          userRepository.save(user);
          log.info("Admin user created (admin@bydzov-ctverec.cz / admin123)");
        }
      } catch (Exception e) {
        log.error("seedAdmin failed: {}", e.getMessage());
      }
    };
  }

  @Bean
  CommandLineRunner seedJudge(UserRepository userRepository, AppRoleRepository appRoleRepository, PasswordEncoder encoder) {
    return args -> {
      try {
        if (userRepository.findByEmail("judge@bydzov-ctverec.cz").isEmpty()) {
          var judgeRole = appRoleRepository.findByName("JUDGE").orElse(null);
          Set<AppRole> roles = new java.util.HashSet<>();
          if (judgeRole != null) roles.add(judgeRole);
          User user = new User(
              "judge@bydzov-ctverec.cz", "judge",
              encoder.encode("judge123"),
              UserRole.JUDGE,
              "Komisař", "",
              Instant.now());
          user.getAppRoles().addAll(roles);
          userRepository.save(user);
          log.info("Judge user created (judge@bydzov-ctverec.cz / judge123)");
        }
      } catch (Exception e) {
        log.error("seedJudge failed: {}", e.getMessage());
      }
    };
  }

  @Bean
  CommandLineRunner seedRacer(UserRepository userRepository, AppRoleRepository appRoleRepository, PasswordEncoder encoder) {
    return args -> {
      try {
        if (userRepository.findByEmail("racer@bydzov-ctverec.cz").isEmpty()) {
          var racerRole = appRoleRepository.findByName("RACER").orElse(null);
          Set<AppRole> roles = new java.util.HashSet<>();
          if (racerRole != null) roles.add(racerRole);
          User user = new User(
              "racer@bydzov-ctverec.cz", "racer",
              encoder.encode("racer123"),
              UserRole.RACER,
              "Testovací", "jezdec",
              Instant.now());
          user.getAppRoles().addAll(roles);
          userRepository.save(user);
          log.info("Racer user created (racer@bydzov-ctverec.cz / racer123)");
        }
      } catch (Exception e) {
        log.error("seedRacer failed: {}", e.getMessage());
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
      try {
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
      } catch (Exception e) {
        log.error("seedSchedule failed: {}", e.getMessage());
      }
    };
  }

  @Bean
  @org.springframework.core.annotation.Order(3)
  CommandLineRunner seedCheckpoints(EditionRepository editionRepository, CheckpointRepository checkpointRepository) {
    return args -> {
      try {
        Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
        if (edition == null) return;
        var existing = checkpointRepository.findByEditionOrderBySortOrder(edition);
        if (!existing.isEmpty()) return;
        List<Checkpoint> cps = List.of(
            new Checkpoint(edition, "Prezence / Start", 50.2415, 15.4900, 300, 1),
            new Checkpoint(edition, "Stanoviště 1 — Chlumec", 50.2280, 15.4750, 500, 2),
            new Checkpoint(edition, "Stanoviště 2 — Hlušice", 50.2150, 15.4600, 500, 3),
            new Checkpoint(edition, "Stanoviště 3 — Sloupno", 50.2520, 15.5050, 500, 4),
            new Checkpoint(edition, "Dojezd / Cíl", 50.2415, 15.4900, 300, 5));
        cps.get(0).setTaskDescription("Prezence závodníků, vydání startovních čísel");
        cps.get(0).setMaxPoints(0);
        cps.get(1).setTaskDescription("Přesný časový úsek – měřená zkouška");
        cps.get(1).setMaxPoints(10);
        cps.get(2).setTaskDescription("Přesnost v zatáčkách – slalom mezi kužely");
        cps.get(2).setMaxPoints(15);
        cps.get(3).setTaskDescription("Brzdná zkouška – zastavení na přesnost");
        cps.get(3).setMaxPoints(10);
        cps.get(4).setTaskDescription("Cílová kontrola času");
        cps.get(4).setMaxPoints(0);
        cps.get(0).setVolunteers(List.of("Správce"));
        cps.get(1).setVolunteers(List.of("Komisař"));
        cps.get(2).setVolunteers(List.of("Komisař"));
        cps.get(3).setVolunteers(List.of("Komisař"));
        cps.get(4).setVolunteers(List.of("Správce", "Komisař"));
        checkpointRepository.saveAll(cps);
        log.info("Checkpoints for 2026 seeded");
      } catch (Exception e) {
        log.error("seedCheckpoints failed: {}", e.getMessage());
      }
    };
  }

  @Bean
  CommandLineRunner seedChangeLog(ChangeLogEntryRepository repo) {
    return args -> {
      try {
        if (repo.count() > 0) return;
        repo.saveAll(List.of(
            new ChangeLogEntry("0.1.0", "Inicializace projektu – JWT auth, role ADMIN/JUDGE/RACER, přihlášky, výsledky", Instant.parse("2026-04-01T00:00:00Z")),
            new ChangeLogEntry("0.2.0", "Username login, interaktivní mapa stanovišť, CRUD uživatelů, nastavení účtu, změna hesla, telefon", Instant.parse("2026-05-01T00:00:00Z")),
            new ChangeLogEntry("0.3.0", "Datum vstupu do klubu (memberSince), info lišta s verzí, přejmenování Rozhodčí → Komisaři", Instant.now())));
        log.info("Changelog seeded");
      } catch (Exception e) {
        log.error("seedChangeLog failed: {}", e.getMessage());
      }
    };
  }

  @Bean
  @org.springframework.core.annotation.Order(4)
  CommandLineRunner seedArchiveData(ArchiveEntryRepository archiveEntryRepository) {
    return args -> {
      try {
        if (archiveEntryRepository.count() > 0) return;
        archiveEntryRepository.saveAll(List.of(
            new ArchiveEntry(2025, 1, "Jiří Svoboda", "Škoda Fabia R5", 50),
            new ArchiveEntry(2025, 2, "Petr Novák", "Ford Fiesta Rally3", 48),
            new ArchiveEntry(2025, 3, "Martin Černý", "Mitsubishi Lancer Evo IX", 45),
            new ArchiveEntry(2025, 4, "Tomáš Procházka", "BMW M3 E30", 42),
            new ArchiveEntry(2025, 5, "Jan Krejčí", "Subaru Impreza STI", 40),
            new ArchiveEntry(2024, 1, "Petr Novák", "Ford Fiesta Rally3", 52),
            new ArchiveEntry(2024, 2, "Jiří Svoboda", "Škoda Fabia R5", 49),
            new ArchiveEntry(2024, 3, "Lukáš Horák", "Toyota Supra", 46),
            new ArchiveEntry(2024, 4, "Ondřej Mareš", "Honda Civic Type R", 43),
            new ArchiveEntry(2024, 5, "David Kolář", "Renault Clio RS", 41),
            new ArchiveEntry(2023, 1, "Martin Černý", "Mitsubishi Lancer Evo IX", 47),
            new ArchiveEntry(2023, 2, "Petr Novák", "Ford Fiesta Rally3", 45),
            new ArchiveEntry(2023, 3, "Jiří Svoboda", "Škoda Fabia R5", 44),
            new ArchiveEntry(2023, 4, "Tomáš Procházka", "BMW M3 E30", 40),
            new ArchiveEntry(2023, 5, "Lukáš Horák", "Toyota Supra", 38)));
        log.info("Archive data seeded (3 years)");
      } catch (Exception e) {
        log.error("seedArchiveData failed: {}", e.getMessage());
      }
    };
  }
}
