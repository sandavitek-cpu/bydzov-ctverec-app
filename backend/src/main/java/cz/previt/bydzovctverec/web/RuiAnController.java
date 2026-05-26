package cz.previt.bydzovctverec.web;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/public/ruian")
public class RuiAnController {

  private static final Logger log = LoggerFactory.getLogger(RuiAnController.class);
  private static final String CUZK_ARCGIS_URL =
      "https://ags.cuzk.cz/arcgis/rest/services/RUIAN/MapServer/1/query";

  private final RestTemplate restTemplate;

  public RuiAnController() {
    this.restTemplate = new RestTemplate();
  }

  @GetMapping("/search")
  public ResponseEntity<List<RuiAnAddress>> search(@RequestParam("q") String query) {
    if (query == null || query.trim().isEmpty()) {
      return ResponseEntity.ok(List.of());
    }

    String escaped = query.trim()
        .replace("\\", "\\\\")
        .replace("'", "''")
        .replace("%", "\\%")
        .replace("_", "\\_");

    String where = "upper(adresa) like upper('%" + escaped + "%') AND nespravny = 'N'";
    String url = CUZK_ARCGIS_URL + "?where=" + java.net.URLEncoder.encode(where, java.nio.charset.StandardCharsets.UTF_8)
        + "&outFields=kod,adresa,psc,cislodomovni,cisloorientacni,cisloorientacnipismeno"
        + "&returnGeometry=false&f=json&resultRecordCount=15";

    try {
      var response = restTemplate.getForObject(url, ArcGisResponse.class);
      if (response == null || response.features == null) {
        return ResponseEntity.ok(List.of());
      }
      List<RuiAnAddress> results = response.features.stream()
          .map(f -> new RuiAnAddress(
              f.attributes.kod,
              f.attributes.adresa,
              f.attributes.psc,
              f.attributes.cislodomovni,
              f.attributes.cisloorientacni,
              f.attributes.cisloorientacnipismeno))
          .toList();
      return ResponseEntity.ok(results);
    } catch (Exception e) {
      log.warn("RUIAN search failed for query '{}': {}", query, e.getMessage());
      return ResponseEntity.ok(List.of());
    }
  }

  private record ArcGisResponse(List<Feature> features) {}
  private record Feature(Attributes attributes) {}
  private record Attributes(
      int kod, String adresa, Integer psc,
      Integer cislodomovni, Integer cisloorientacni,
      String cisloorientacnipismeno) {}

  public record RuiAnAddress(
      int kod, String adresa, Integer psc,
      Integer cislodomovni, Integer cisloorientacni,
      String cisloorientacnipismeno) {}
}
