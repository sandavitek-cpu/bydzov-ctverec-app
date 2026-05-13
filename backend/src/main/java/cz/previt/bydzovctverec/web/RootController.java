package cz.previt.bydzovctverec.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Render / load balancer často volá {@code GET /} — musí vracet 200, ne 401. */
@RestController
public class RootController {

  @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
  public String root() {
    return "bydzov-ctverec-api ok\n";
  }
}
