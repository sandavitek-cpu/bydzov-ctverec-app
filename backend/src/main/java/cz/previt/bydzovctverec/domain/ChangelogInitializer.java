package cz.previt.bydzovctverec.domain;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
public class ChangelogInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(ChangelogInitializer.class);

  private final ChangeLogEntryRepository repository;

  public ChangelogInitializer(ChangeLogEntryRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    var resource = new ClassPathResource("git-commits.properties");
    if (!resource.exists()) {
      log.info("git-commits.properties not found, skipping changelog initialization");
      return;
    }

    try (var reader = new BufferedReader(
        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) continue;

        if (line.startsWith("current=")) continue;

        var parts = line.split("\\|\\|\\|", 2);
        if (parts.length < 2) continue;

        var fullHash = parts[0].trim();
        var message = parts[1].trim();
        var shortHash = fullHash.length() >= 7 ? fullHash.substring(0, 7) : fullHash;

        if (repository.existsByVersion(shortHash)) continue;

        var entry = new ChangeLogEntry(shortHash, message, Instant.now());
        repository.save(entry);
        log.info("Changelog entry added: {} - {}", shortHash, message);
      }
    }
    log.info("Changelog initialization complete");
  }
}
