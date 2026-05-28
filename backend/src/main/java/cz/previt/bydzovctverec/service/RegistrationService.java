package cz.previt.bydzovctverec.service;

import cz.previt.bydzovctverec.config.EmailService;
import cz.previt.bydzovctverec.domain.*;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

  private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);
  private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
  private static final SecureRandom RANDOM = new SecureRandom();

  private static final Map<String, FeeConfig> FEE_MAP = Map.of(
      "JEDNODENNI", new FeeConfig(500, 1000, 500),
      "DVODENNI", new FeeConfig(1000, 1200, 1000),
      "DVODENNI_UZAVRENO", new FeeConfig(1000, 1200, 1000),
      "DVODENNI_BEZ_UBYTOVANI", new FeeConfig(600, 900, 600));

  private final RacerRegistrationRepository racerRegistrationRepository;
  private final UserRepository userRepository;
  private final AppRoleRepository appRoleRepository;
  private final CrewMemberRepository crewMemberRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  public RegistrationService(RacerRegistrationRepository racerRegistrationRepository,
      UserRepository userRepository, AppRoleRepository appRoleRepository,
      CrewMemberRepository crewMemberRepository, PasswordEncoder passwordEncoder,
      EmailService emailService) {
    this.racerRegistrationRepository = racerRegistrationRepository;
    this.userRepository = userRepository;
    this.appRoleRepository = appRoleRepository;
    this.crewMemberRepository = crewMemberRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailService = emailService;
  }

  public int calculateFee(String variant, int vehicleYear, int crewCount) {
    FeeConfig cfg = FEE_MAP.getOrDefault(variant, new FeeConfig(0, 0, 0));
    int baseFee = vehicleYear < 1945 ? cfg.baseDo1945 : cfg.baseOd1946;
    return baseFee + cfg.extraPerson * Math.max(0, crewCount - 1);
  }

  public String generatePassword() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 10; i++) {
      sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
    }
    return sb.toString();
  }

  public record CreatedUser(User user, String rawPassword) {}

  public CreatedUser createUser(String email, String firstName, String lastName, RacerRegistration reg,
      Integer driverAge, String gender, String address, String club, Boolean firstTime) {
    String rawPassword = generatePassword();
    var racerRole = appRoleRepository.findByName("RACER").orElse(null);
    User user = new User(email, email, passwordEncoder.encode(rawPassword), UserRole.RACER,
        firstName, lastName, Instant.now());
    if (racerRole != null) user.getAppRoles().add(racerRole);
    String phone = reg.getPhone() != null ? reg.getPhone().trim() : "";
    user.setPhone(phone);
    userRepository.save(user);
    crewMemberRepository.save(new CrewMember(reg, user, firstName, lastName, email,
        driverAge, gender, address,
        club != null && !club.isBlank(),
        firstTime, club));
    return new CreatedUser(user, rawPassword);
  }

  @Transactional
  public String approveRegistration(RacerRegistration reg) {
    if (Boolean.TRUE.equals(reg.getApproved())) {
      throw new IllegalStateException("Přihláška již schválena");
    }
    String email = reg.getEmail();
    if (userRepository.findByEmail(email).isPresent()) {
      throw new IllegalStateException("Uživatel s tímto emailem již existuje");
    }
    String rawPassword = generatePassword();
    var racerRole = appRoleRepository.findByName("RACER").orElse(null);
    var roles = new HashSet<AppRole>();
    if (racerRole != null) roles.add(racerRole);

    String firstName = reg.getFirstName() != null && !reg.getFirstName().isBlank()
        ? reg.getFirstName() : reg.getTeamName();
    String lastName = reg.getLastName() != null && !reg.getLastName().isBlank()
        ? reg.getLastName() : "";
    String personName = (firstName + " " + lastName).trim();
    if (personName.isEmpty()) personName = reg.getTeamName();

    User user = new User(email, email, passwordEncoder.encode(rawPassword),
        UserRole.RACER, firstName, lastName, Instant.now());
    user.setPhone(reg.getPhone());
    user.getAppRoles().addAll(roles);
    userRepository.save(user);

    reg.setApproved(true);
    racerRegistrationRepository.save(reg);

    emailService.sendCredentials(email, personName, email, rawPassword,
        reg.getPaymentReference() != null ? reg.getPaymentReference() : 0,
        reg.getStartFee(), reg.getStartNumber());

    log.info("Registration {} approved, user {} created", reg.getId(), email);
    return email;
  }

  @Transactional
  public void cancelRegistration(RacerRegistration reg) {
    if (reg.getCancelledAt() != null) {
      throw new IllegalStateException("Přihláška již byla stornována");
    }
    Edition edition = reg.getEdition();
    Instant now = Instant.now();
    reg.setCancelledAt(now);
    reg.setStatus("CANCELLED");
    reg.setApproved(false);

    Integer paid = reg.getPaidAmount() != null ? reg.getPaidAmount() : 0;
    if (paid > 0) {
      Instant deadline = edition.getCancellationDeadline();
      if (deadline != null && now.isBefore(deadline)) {
        reg.setRefundAmount(paid);
      } else {
        reg.setRefundAmount(paid * 75 / 100);
      }
    }
    reg.setPaidAt(null);
    reg.setPaidAmount(0);
    reg.setStartNumber(null);
    reg.setPaymentReference(null);
    racerRegistrationRepository.save(reg);
    log.info("Registration {} cancelled, refund={}", reg.getId(), reg.getRefundAmount());
  }

  public List<RacerRegistration> getActiveRacers(Edition edition) {
    return racerRegistrationRepository.findByEditionOrderByStartNumber(edition).stream()
        .filter(r -> "PAID".equals(r.getStatus()))
        .toList();
  }

  @Transactional
  public synchronized int assignStartNumber(RacerRegistration reg) {
    String variant = reg.getVariant();
    if (variant == null) throw new IllegalArgumentException("Přihláška nemá variantu");
    int rangeStart = "JEDNODENNI".equals(variant) ? 1 : 101;
    int rangeEnd = "JEDNODENNI".equals(variant) ? 99 : 130;
    Edition edition = reg.getEdition();
    List<RacerRegistration> all = racerRegistrationRepository.findByEditionOrderByStartNumber(edition);
    var taken = all.stream()
        .map(RacerRegistration::getStartNumber)
        .filter(n -> n != null && n >= rangeStart && n <= rangeEnd)
        .collect(java.util.stream.Collectors.toSet());
    for (int i = rangeStart; i <= rangeEnd; i++) {
      if (!taken.contains(i)) {
        reg.setStartNumber(i);
        racerRegistrationRepository.save(reg);
        log.info("Start number {} assigned to registration {}", i, reg.getId());
        return i;
      }
    }
    throw new IllegalStateException("Vyčerpána kapacita startovních čísel v rozsahu " + rangeStart + "–" + rangeEnd);
  }

  public int generatePaymentReference(Edition edition) {
    return racerRegistrationRepository
        .findTopByEditionOrderByPaymentReferenceDesc(edition)
        .map(r -> {
          Integer ref = r.getPaymentReference();
          return ref != null ? ref + 1 : edition.getEditionYear() * 1000 + 1;
        })
        .orElse(edition.getEditionYear() * 1000 + 1);
  }

  public void resendCredentials(RacerRegistration reg) {
    List<CrewMember> crew = crewMemberRepository.findByRegistration(reg);
    if (crew.isEmpty()) {
      throw new IllegalStateException("K této přihlášce nejsou vytvořeny uživatelské účty");
    }
    int sent = 0;
    for (CrewMember cm : crew) {
      User u = cm.getUser();
      if (u == null) continue;
      String rawPassword = generatePassword();
      u.setPassword(passwordEncoder.encode(rawPassword));
      userRepository.save(u);
      String personName = cm.getFirstName() + " " + cm.getLastName();
      emailService.sendCredentials(cm.getEmail(), personName, cm.getEmail(), rawPassword,
          reg.getPaymentReference(), reg.getStartFee(), reg.getStartNumber());
      sent++;
    }
    log.info("Credentials resent for registration {} ({} users)", reg.getId(), sent);
  }

  public Integer recalculateFee(RacerRegistration reg) {
    String variant = reg.getVariant();
    int year = reg.getVehicleYear() != null ? reg.getVehicleYear() : 0;
    int crew = reg.getCrewCount() != null ? reg.getCrewCount() : 1;
    return calculateFee(variant, year, crew);
  }

  private record FeeConfig(int baseDo1945, int baseOd1946, int extraPerson) {}
}
