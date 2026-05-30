package cz.previt.bydzovctverec.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayRepairConfig {

    @Bean
    FlywayMigrationInitializer flywayMigrationInitializer(Flyway flyway) {
        flyway.repair();
        return new FlywayMigrationInitializer(flyway);
    }
}
