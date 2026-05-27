package cz.previt.bydzovctverec.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Integer.MIN_VALUE)
public class MdcLoggingFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    try {
      MDC.put("correlationId", UUID.randomUUID().toString().substring(0, 8));
      MDC.put("path", req.getRequestURI());
      MDC.put("method", req.getMethod());
      String ip = req.getHeader("X-Forwarded-For");
      if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
      MDC.put("ip", ip);
      chain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }
}
