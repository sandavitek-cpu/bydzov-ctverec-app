package cz.previt.bydzovctverec.config;

import cz.previt.bydzovctverec.domain.Edition;
import cz.previt.bydzovctverec.domain.EditionRepository;
import cz.previt.bydzovctverec.domain.RacerRegistration;
import cz.previt.bydzovctverec.domain.RacerRegistrationRepository;
import cz.previt.bydzovctverec.web.RegistrationController;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class RegistrationSeeder {

  private static final Logger log = LoggerFactory.getLogger(RegistrationSeeder.class);

  @Bean
  @Order(5)
  CommandLineRunner seedRegistrations(EditionRepository editionRepository, RacerRegistrationRepository repo) {
    return args -> {
      try {
        if (repo.count() > 0) {
          log.info("Registrations already seeded (count={})", repo.count());
          return;
        }
        Edition edition = editionRepository.findTopByOrderByEditionYearDesc().orElse(null);
        if (edition == null) return;

        int sn = 0;

        // ===== JEDNODENNÍ ===== (28 posádek / 48 členů)
        List<Object[]> jednodenni = List.of(
            // team, email, phone, cat, make, plate, year, crew, variant, firstName, lastName, firstTime, gender, driverAge, club, youngestAge, youngestName, ccm, hp, speed, notes, contacted, reg, arrived, consent
            args("Jawa 250 Pioniér", "jawa@pionier.cz", "777111222", "MOTOCYKL", "Jawa 250 Pioniér", "5H1 0001", 1958, 1, "JEDNODENNI", "Jiří", "Svoboda", false, "M", 42, "Jawa Tábor", null, null, 250, 12, 95, "", true, true, true, true),
            args("Škoda 1000 MB", "skodovka@seznam.cz", "777111223", "OSOBNI", "Škoda 1000 MB", "5H2 0002", 1967, 2, "JEDNODENNI", "Petr", "Novák", false, "M", 55, "Škoda klub Hradec", 8, "Anna Nováková", 1000, 42, 120, "Sedadla nově čalouněna", true, true, true, true),
            args("Tatra 603", "tatra603@email.cz", "777111224", "OSOBNI", "Tatra 603", "5H3 0003", 1963, 2, "JEDNODENNI", "Milan", "Černý", false, "M", 60, "Tatra klub", null, null, 2500, 72, 150, "", true, true, true, true),
            args("Aero 50", "aero50@volny.cz", "777111225", "MOTOCYKL", "Aero 50", "5H4 0004", 1941, 1, "JEDNODENNI", "Josef", "Procházka", false, "M", 72, "Veterán klub Hradec", null, null, 250, 10, 80, "Po kompletní renovaci", true, true, true, true),
            args("Praga V3S", "v3s@armada.cz", "777111226", "NAKLADNI", "Praga V3S", "5H5 0005", 1960, 3, "JEDNODENNI", "Stanislav", "Dvořák", false, "M", 48, "Praga klub", 10, "Tomáš Dvořák", 7400, 78, 60, "", false, true, false, true),
            args("Jawa 350 Californian", "californian@email.cz", "777111227", "MOTOCYKL", "Jawa 350 Californian", "5H6 0006", 1965, 1, "JEDNODENNI", "Václav", "Horák", false, "M", 38, "Jawa klub", null, null, 350, 16, 110, "", false, true, true, true),
            args("BMW 2002", "bmw2002@seznam.cz", "777111228", "OSOBNI", "BMW 2002", "5H7 0007", 1972, 2, "JEDNODENNI", "Karel", "Bartoš", false, "M", 50, "BMW klub", 6, "Eliška Bartošová", 2000, 75, 160, "", true, true, true, true),
            args("Škoda 110 R", "110r@skoda.cz", "777111229", "OSOBNI", "Škoda 110 R", "5H8 0008", 1974, 2, "JEDNODENNI", "Lukáš", "Kučera", false, "M", 45, "Škoda klub", null, null, 1100, 48, 130, "", true, false, true, true),
            args("Jawa 500 OHC", "jawa500@centrum.cz", "777111230", "MOTOCYKL", "Jawa 500 OHC", "5H9 0009", 1930, 1, "JEDNODENNI", "František", "Pospíšil", false, "M", 68, "Veterán klub Hradec", null, null, 500, 22, 120, "Vzácný kus", true, true, false, true),
            args("Ford Mustang", "mustang@ford.cz", "777111231", "OSOBNI", "Ford Mustang", "5H0 0010", 1968, 3, "JEDNODENNI", "Tomáš", "Veselý", false, "M", 35, "Ford klub", 7, "Filip Veselý", 4700, 150, 200, "", true, true, true, true),
            args("ČZ 125 Sport", "cz125@seznam.cz", "777111232", "MOTOCYKL", "ČZ 125 Sport", "5H1 0011", 1934, 1, "JEDNODENNI", "Jaroslav", "Marek", false, "M", 74, "ČZ klub", null, null, 125, 5, 75, "", false, true, false, true),
            args("Škoda Octavia", "octavia@seznam.cz", "777111233", "OSOBNI", "Škoda Octavia", "5H2 0012", 1962, 2, "JEDNODENNI", "Pavel", "Růžička", false, "M", 52, null, 9, "Marie Růžičková", 1200, 44, 125, "", false, true, true, true),
            args("Jawa 250/353", "jawa353@post.cz", "777111234", "MOTOCYKL", "Jawa 250/353", "5H3 0013", 1955, 1, "JEDNODENNI", "Zdeněk", "Kovář", false, "M", 44, "Jawa klub", null, null, 250, 10, 90, "", true, true, true, true),
            args("Trabant 601", "trabant@email.cz", "777111235", "OSOBNI", "Trabant 601", "5H4 0014", 1978, 2, "JEDNODENNI", "Martin", "Blažek", true, "M", 28, null, 4, "Natálie Blažková", 600, 22, 100, "První účast!", true, true, true, true),
            args("Jawa 350/640", "jawa640@centrum.cz", "777111236", "MOTOCYKL", "Jawa 350/640", "5H5 0015", 1972, 1, "JEDNODENNI", "Roman", "Šimek", false, "M", 36, null, null, null, 350, 16, 110, "", false, true, false, true),
            args("Škoda Favorit", "favorit@skoda.cz", "777111237", "OSOBNI", "Škoda Favorit", "5H6 0016", 1989, 2, "JEDNODENNI", "David", "Král", true, "M", 30, null, 5, "Sára Králová", 1300, 50, 140, "", false, true, true, true),
            args("Mercedes 190 SL", "mercedes@email.cz", "777111238", "OSOBNI", "Mercedes 190 SL", "5H7 0017", 1958, 2, "JEDNODENNI", "Jan", "Kolář", false, "M", 62, "Mercedes klub", null, null, 1900, 77, 170, "", true, true, true, true),
            args("Škoda 1203", "1203@seznam.cz", "777111239", "NAKLADNI", "Škoda 1203", "5H8 0018", 1976, 2, "JEDNODENNI", "Aleš", "Novák", false, "M", 47, null, null, null, 1200, 45, 100, "", false, true, false, true),
            args("Jawa 250/592", "jawa592@email.cz", "777111240", "MOTOCYKL", "Jawa 250/592", "5H9 0019", 1953, 1, "JEDNODENNI", "Miroslav", "Havel", false, "M", 56, "Jawa klub", null, null, 250, 8, 85, "", true, true, false, true),
            args("Wartburg 353", "wartburg@volny.cz", "777111241", "OSOBNI", "Wartburg 353", "5H0 0020", 1975, 2, "JEDNODENNI", "Radek", "Zeman", true, "M", 26, null, null, null, 1000, 36, 115, "", false, true, true, true),
            args("Tatra 57", "tatra57@centrum.cz", "777111242", "OSOBNI", "Tatra 57", "5H1 0021", 1936, 2, "JEDNODENNI", "Josef", "Šťastný", false, "M", 70, "Tatra klub", null, null, 1200, 18, 90, "Po kompletní renovaci", true, true, true, true),
            args("Jawa 350/354", "jawa354@post.cz", "777111243", "MOTOCYKL", "Jawa 350/354", "5H2 0022", 1957, 1, "JEDNODENNI", "Bohumil", "Fišer", false, "M", 64, null, null, null, 350, 14, 105, "", true, true, false, true),
            args("Škoda Felicia", "felicia@seznam.cz", "777111244", "OSOBNI", "Škoda Felicia", "5H3 0023", 1961, 2, "JEDNODENNI", "Vladimír", "Pokorný", false, "M", 58, null, 7, "Lenka Pokorná", 1000, 38, 115, "", false, true, true, true),
            args("BMW Isetta", "isetta@email.cz", "777111245", "OSOBNI", "BMW Isetta", "5H4 0024", 1957, 1, "JEDNODENNI", "Petr", "Valenta", false, "M", 49, "BMW klub", null, null, 300, 10, 85, "Vtipné vozítko", true, true, true, true),
            args("Zetor 25", "zetor@traktor.cz", "777111246", "NAKLADNI", "Zetor 25", "5H5 0025", 1952, 1, "JEDNODENNI", "Emil", "Müller", false, "M", 67, "Zetor klub", null, null, 2100, 22, 30, "", false, true, false, true),
            args("Škoda Rapid", "rapid@skoda.cz", "777111247", "OSOBNI", "Škoda Rapid", "5H6 0026", 1938, 2, "JEDNODENNI", "Vít", "Prokop", false, "M", 66, "Škoda klub", null, null, 1500, 38, 105, "", true, true, true, true),
            args("Jawa 50/550", "jawka@post.cz", "777111248", "MOTOCYKL", "Jawa 50/550", "5H7 0027", 1968, 1, "JEDNODENNI", "Filip", "Sýkora", true, "M", 16, null, null, null, 50, 3, 45, "První závod!", true, true, true, true),
            args("GAZ 21 Volha", "volha@email.cz", "777111249", "OSOBNI", "GAZ 21 Volha", "5H8 0028", 1964, 3, "JEDNODENNI", "Ivan", "Bednář", false, "M", 54, "GAZ klub", 9, "Ivana Bednářová", 2400, 62, 140, "", false, true, true, true)
        );

        for (Object[] a : jednodenni) {
          sn++;
          repo.save(create(edition, sn, a));
        }

        // ===== DVODENNÍ ===== (21 posádek / 55 členů)
        List<Object[]> dvoudenni = List.of(
            args("Bugatti T37", "bugatti@email.cz", "777111301", "OSOBNI", "Bugatti T37", "5H9 0029", 1926, 2, "DVODENNI", "Karel", "Šebek", false, "M", 65, "Bugatti klub", null, null, 1500, 45, 140, "Jeden z mála exemplářů v ČR", true, true, true, true),
            args("Ariel 500", "ariel@veteran.cz", "777111302", "MOTOCYKL", "Ariel 500", "5H0 0030", 1928, 1, "DVODENNI", "Tomáš", "Hruška", false, "M", 70, "Veterán klub", null, null, 500, 18, 110, "", true, true, false, true),
            args("Bugatti T35", "t35@centrum.cz", "777111303", "OSOBNI", "Bugatti T35", "5H1 0031", 1925, 2, "DVODENNI", "Eduard", "Liška", false, "M", 72, "Bugatti klub", null, null, 2000, 67, 160, "Závodní legenda", true, true, true, true),
            args("Jawa 750", "jawa750@post.cz", "777111304", "MOTOCYKL", "Jawa 750", "5H2 0032", 1938, 1, "DVODENNI", "Jaroslav", "Hodboď", false, "M", 69, "Jawa klub", null, null, 750, 25, 130, "", true, true, true, true),
            args("Škoda Popular", "popular@skoda.cz", "777111305", "OSOBNI", "Škoda Popular", "5H3 0033", 1936, 2, "DVODENNI", "Antonín", "Vlček", false, "M", 71, "Škoda klub", null, null, 1000, 28, 100, "", true, true, true, true),
            args("Indian Scout", "indian@moto.cz", "777111306", "MOTOCYKL", "Indian Scout", "5H4 0034", 1925, 1, "DVODENNI", "Rostislav", "Beneš", false, "M", 74, "Indian klub", null, null, 600, 15, 100, "Nejstarší motocykl v ročníku", true, true, false, true),
            args("Praga Piccolo", "piccolo@email.cz", "777111307", "OSOBNI", "Praga Piccolo", "5H5 0035", 1939, 3, "DVODENNI", "Miloš", "Kučera", false, "M", 68, "Praga klub", 6, "Tereza Kučerová", 900, 22, 90, "", true, true, true, true),
            args("Jawa 350/637", "jawa637@centrum.cz", "777111308", "MOTOCYKL", "Jawa 350/637", "5H6 0036", 1974, 1, "DVODENNI", "Vítězslav", "Horký", false, "M", 39, null, null, null, 350, 16, 110, "", false, true, false, true),
            args("Tatra 87", "tatra87@volny.cz", "777111309", "OSOBNI", "Tatra 87", "5H7 0037", 1940, 3, "DVODENNI", "Lumír", "Dvořák", false, "M", 73, "Tatra klub", 10, "Linda Dvořáková", 3000, 62, 150, "Aerodynamický skvost", true, true, true, true),
            args("BSA 650", "bsa@moto.cz", "777111310", "MOTOCYKL", "BSA 650 Lightning", "5H8 0038", 1965, 1, "DVODENNI", "Prokop", "Malý", false, "M", 46, "BSA klub", null, null, 650, 30, 150, "", true, true, true, true),
            args("Audi 100 Coupé", "audi100@seznam.cz", "777111311", "OSOBNI", "Audi 100 Coupé", "5H9 0039", 1971, 2, "DVODENNI", "Vlastimil", "Jelínek", false, "M", 53, null, 5, "Klára Jelínková", 1900, 78, 170, "", true, true, true, true),
            args("ČZ 250/471", "cz471@post.cz", "777111312", "MOTOCYKL", "ČZ 250/471", "5H0 0040", 1955, 1, "DVODENNI", "Bohuslav", "Štěpánek", false, "M", 61, "ČZ klub", null, null, 250, 10, 90, "", false, true, false, true),
            args("Mercedes 300SL", "300sl@gullwing.cz", "777111313", "OSOBNI", "Mercedes 300SL", "5H1 0041", 1956, 2, "DVODENNI", "Vojtěch", "Kopecký", false, "M", 59, "Mercedes klub", null, null, 3000, 155, 220, "Křídla", true, true, true, true),
            args("Sunbeam 500", "sunbeam@moto.cz", "777111314", "MOTOCYKL", "Sunbeam 500", "5H2 0042", 1930, 1, "DVODENNI", "Viktor", "Urban", false, "M", 76, "Veterán klub", null, null, 500, 16, 105, "", true, true, true, true),
            args("Rover P6", "rover@email.cz", "777111315", "OSOBNI", "Rover P6", "5H3 0043", 1968, 2, "DVODENNI", "Zbyněk", "Čermák", false, "M", 57, null, null, null, 2000, 82, 165, "", false, true, true, true),
            args("Dnepr MT-11", "dnepr@sidecar.cz", "777111316", "MOTOCYKL", "Dnepr MT-11", "5H4 0044", 1982, 3, "DVODENNI", "Radim", "Pech", false, "M", 43, "Sidecar klub", 8, "Samuel Pech", 650, 30, 100, "S postranním vozíkem", false, true, true, true),
            args("Jaguar E-Type", "etype@jaguar.cz", "777111317", "OSOBNI", "Jaguar E-Type", "5H5 0045", 1963, 2, "DVODENNI", "Richard", "Bárta", false, "M", 63, "Jaguar klub", null, null, 4200, 198, 240, "Kultovní vůz", true, true, true, true),
            args("Norton 750 Commando", "norton@moto.cz", "777111318", "MOTOCYKL", "Norton 750 Commando", "5H6 0046", 1970, 1, "DVODENNI", "Ivo", "Procházka", false, "M", 51, "Norton klub", null, null, 750, 42, 170, "", true, true, true, true),
            args("Willys MB", "willys@army.cz", "777111319", "OSOBNI", "Willys MB", "5H7 0047", 1943, 2, "DVODENNI", "Pavel", "Hrubý", false, "M", 84, "Armádní klub", null, null, 2200, 45, 95, "Nejstarší řidič ročníku", true, true, true, true),
            args("Škoda 105/120", "skoda120@seznam.cz", "777111320", "OSOBNI", "Škoda 105/120", "5H8 0048", 1979, 4, "DVODENNI", "Ondřej", "Tichý", true, "M", 25, null, 3, "Ema Tichá", 1200, 42, 130, "", false, true, true, true),
            args("Alfa Romeo Giulia", "alfetta@email.cz", "777111321", "OSOBNI", "Alfa Romeo Giulia", "5H9 0049", 1967, 3, "DVODENNI", "Svatopluk", "Červenka", false, "M", 60, "Alfa klub", 6, "Markéta Červenková", 1600, 78, 170, "", true, true, true, true)
        );

        for (Object[] a : dvoudenni) {
          sn++;
          repo.save(create(edition, sn, a));
        }

        // ===== PŘIDEJ BEZ UBYTOVÁNÍ (= null variant) jednu posádku =====
        sn++;
        repo.save(new RacerRegistration(
            edition, "Zapůjčené vozidlo", "zapujceno@test.cz", "777111401",
            "OSOBNI", "Citroën 2CV", "5H0 0050", 1968, 2, sn,
            RegistrationController.calculateFee("JEDNODENNI", 1968, 2),
            null, "Testovací", "Řidič", false, "M", 33, null, null,
            null, null, null, null, null, null, null, false, true, false, true, false, Instant.now()));

        // ===== PŘIDEJ registraci pro seed racer usera =====
        boolean foundRacer2 = false;
        for (var r : repo.findAll()) {
          if ("racer@bydzov-ctverec.cz".equals(r.getEmail())) { foundRacer2 = true; break; }
        }
        if (!foundRacer2) {
          sn++;
          repo.save(create(edition, sn, new Object[]{
              "Testovací jezdec", "racer@bydzov-ctverec.cz", "777111501",
              "OSOBNI", "Škoda 1000 MB", "5H0 0051", 1967, 2, "JEDNODENNI",
              "Testovací", "jezdec", true, "M", 35, "Test klub",
              null, null, 0, 0, 0, "",
              true, true, true, true}));
        }

        log.info("Registration data seeded (total {} registrations)", repo.count());
      } catch (Exception e) {
        log.error("seedRegistrations failed: {}", e.getMessage());
      }
    };
  }

  private static Object[] args(
      String team, String email, String phone, String cat, String make,
      String plate, int year, int crew, String variant,
      String firstName, String lastName, boolean firstTime, String gender,
      int driverAge, String club, Integer youngestAge, String youngestName,
      int ccm, int hp, int speed, String vehicleNotes,
      boolean contacted, boolean properlyRegistered, boolean arrived, boolean consent) {
    return new Object[]{team, email, phone, cat, make, plate, year, crew, variant,
        firstName, lastName, firstTime, gender, driverAge, club,
        youngestAge, youngestName, ccm, hp, speed, vehicleNotes,
        contacted, properlyRegistered, arrived, consent};
  }

  private static RacerRegistration create(Edition edition, int startNumber, Object[] a) {
    String variant = (String) a[8];
    int vehicleYear = (int) a[6];
    int crewCount = (int) a[7];
    int fee = RegistrationController.calculateFee(variant, vehicleYear, crewCount);
    return new RacerRegistration(
        edition, (String) a[0], (String) a[1], (String) a[2],
        (String) a[3], (String) a[4], (String) a[5],
        vehicleYear, crewCount, startNumber, fee,
        variant, (String) a[9], (String) a[10],
        (boolean) a[11], (String) a[12], (int) a[13],
        (String) a[14], null,
        (Integer) a[15], (String) a[16],
        (int) a[17], (int) a[18], (int) a[19],
        (String) a[20], null,
        (boolean) a[21], (boolean) a[22], (boolean) a[23], (boolean) a[24], false,
        Instant.now());
  }
}
