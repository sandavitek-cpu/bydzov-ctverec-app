package cz.previt.bydzovctverec.config;

import cz.previt.bydzovctverec.domain.Role;
import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import java.time.Instant;
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
}
