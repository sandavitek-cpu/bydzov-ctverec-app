package cz.previt.bydzovctverec.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Integer.MIN_VALUE + 50)
public class RateLimitingFilter implements Filter {

  private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
  private static final int MAX_REQUESTS = 100;
  private static final long WINDOW_MS = 60_000L;

  private final ConcurrentMap<String, Window> counters = new ConcurrentHashMap<>();

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    String path = req.getRequestURI();

    if (path.startsWith("/api/auth/") || path.startsWith("/api/public/")) {
      long now = System.currentTimeMillis();
      String ip = req.getHeader("X-Forwarded-For");
      if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
      String key = ip + ":" + path;
      if (counters.size() > 10_000) {
        counters.entrySet().removeIf(e -> now - e.getValue().start > WINDOW_MS);
      }
      Window w = counters.compute(key, (k, v) -> {
        if (v == null || now - v.start > WINDOW_MS) {
          return new Window(now, 1);
        }
        v.count = v.count + 1;
        return v;
      });

      if (w.count > MAX_REQUESTS) {
        String errorCode = ErrorCodeGenerator.generate();
        log.warn("ErrorCode={} Rate limit exceeded for {} from {}", errorCode, path, ip);
        HttpServletResponse res = (HttpServletResponse) response;
        res.setStatus(429);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"Příliš mnoho požadavků, zkuste později\",\"errorCode\":\"" + errorCode + "\"}");
        return;
      }
    }

    chain.doFilter(request, response);
  }

  private static class Window {
    long start;
    int count;

    Window(long start, int count) {
      this.start = start;
      this.count = count;
    }
  }
}
