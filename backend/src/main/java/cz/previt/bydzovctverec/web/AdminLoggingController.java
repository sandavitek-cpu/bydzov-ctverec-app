package cz.previt.bydzovctverec.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/logging")
public class AdminLoggingController {

  private static final DateTimeFormatter LOG_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
  private static final String LOG_DIR = "logs";
  private static final String LOG_FILE = LOG_DIR + "/application.log";

  private final LoggingSystem loggingSystem;

  public AdminLoggingController(LoggingSystem loggingSystem) {
    this.loggingSystem = loggingSystem;
  }

  @GetMapping("/level")
  public ResponseEntity<?> getLevel() {
    LogLevel current = loggingSystem.getLoggerConfiguration("ROOT").getEffectiveLevel();
    String label = switch (current.toString()) {
      case "DEBUG" -> "DEBUG";
      default -> "INFO";
    };
    return ResponseEntity.ok(Map.of("level", label));
  }

  @PostMapping("/level")
  public ResponseEntity<?> setLevel(@RequestBody Map<String, String> body) {
    String level = body.get("level");
    if (level == null || (!level.equals("INFO") && !level.equals("DEBUG"))) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Neplatná úroveň (INFO / DEBUG)"));
    }
    loggingSystem.setLogLevel("ROOT", LogLevel.valueOf(level));
    return ResponseEntity.ok(Map.of("level", level));
  }

  @GetMapping("/download")
  public ResponseEntity<?> download() {
    File file = new File(LOG_FILE);
    if (!file.exists()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Log soubor nenalezen"));
    }

    LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
    List<String> lines = new ArrayList<>();

    try (BufferedReader r = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
      String line;
      while ((line = r.readLine()) != null) {
        if (line.length() >= 23) {
          String tsPart = line.substring(0, 23).replace('T', ' ');
          try {
            LocalDateTime ts = LocalDateTime.parse(tsPart, LOG_TS);
            if (ts.isAfter(cutoff)) {
              lines.add(line);
            }
          } catch (DateTimeParseException e) {
            if (!lines.isEmpty()) lines.add(line);
          }
        } else {
          if (!lines.isEmpty()) lines.add(line);
        }
      }
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Chyba čtení logu: " + e.getMessage()));
    }

    String content = String.join("\n", lines);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=application.log")
        .contentType(MediaType.TEXT_PLAIN)
        .body(content);
  }
}
