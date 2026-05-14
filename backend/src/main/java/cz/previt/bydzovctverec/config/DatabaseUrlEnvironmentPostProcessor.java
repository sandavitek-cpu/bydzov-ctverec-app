package cz.previt.bydzovctverec.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Maps Neon / Render style {@code DATABASE_URL} ({@code postgresql://user:pass@host/db?...}) to
 * Spring {@code spring.datasource.*} properties.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

  private static final String SOURCE = "convertedDatabaseUrl";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    if (environment.matchesProfiles("test")) {
      return;
    }
    String databaseUrl = resolveDatabaseUrl(environment);
    if (databaseUrl == null || databaseUrl.isBlank() || databaseUrl.startsWith("jdbc:")) {
      return;
    }
    if (databaseUrl.startsWith("postgres://")) {
      databaseUrl = "postgresql://" + databaseUrl.substring("postgres://".length());
    }
    if (!databaseUrl.startsWith("postgresql://")) {
      return;
    }

    String normalized = databaseUrl.replaceFirst("^postgresql://", "http://");
    URI uri = URI.create(normalized);

    String userInfo = uri.getRawUserInfo();
    if (userInfo == null || !userInfo.contains(":")) {
      return;
    }

    int colon = userInfo.indexOf(':');
    String user = URLDecoder.decode(userInfo.substring(0, colon), StandardCharsets.UTF_8);
    String password = URLDecoder.decode(userInfo.substring(colon + 1), StandardCharsets.UTF_8);

    String host = uri.getHost();
    if (host == null) {
      return;
    }

    int port = uri.getPort();
    String path = uri.getPath();
    String query = uri.getRawQuery();

    StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(host);
    if (port > 0) {
      jdbc.append(":").append(port);
    }
    jdbc.append(path == null || path.isEmpty() ? "/" : path);

    boolean hasSslParam = query != null && (query.contains("sslmode=") || query.contains("ssl="));
    StringBuilder queryBuilder = new StringBuilder();
    if (query != null && !query.isBlank()) {
      queryBuilder.append(query);
    }
    if (!hasSslParam) {
      if (!queryBuilder.isEmpty()) queryBuilder.append("&");
      queryBuilder.append("sslmode=require");
    }
    if (!queryBuilder.isEmpty()) {
      jdbc.append("?").append(queryBuilder);
    }

    Map<String, Object> props = new LinkedHashMap<>();
    props.put("spring.datasource.url", jdbc.toString());
    props.put("spring.datasource.username", user);
    props.put("spring.datasource.password", password);
    props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

    environment.getPropertySources().addFirst(new MapPropertySource(SOURCE, props));
  }

  /**
   * GitLab CI má často jen DATABASE_URL; na Renderu je nutné stejné jméno přidat v Environment (není
   * synchronizované z GitLabu). Zkusíme i běžné aliasy.
   */
  private static String resolveDatabaseUrl(ConfigurableEnvironment environment) {
    for (String key : List.of("DATABASE_URL", "NEON_DATABASE_URL", "POSTGRES_URL")) {
      String v = environment.getProperty(key);
      if (v != null && !v.isBlank()) {
        return v;
      }
      v = System.getenv(key);
      if (v != null && !v.isBlank()) {
        System.err.println("[DatabaseUrlEnvironmentPostProcessor] found " + key + " via System.getenv()");
        return v;
      }
    }
    System.err.println("[DatabaseUrlEnvironmentPostProcessor] DATABASE_URL not found in environment or system env");
    return null;
  }
}
