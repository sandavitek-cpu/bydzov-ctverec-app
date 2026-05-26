package cz.previt.bydzovctverec.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Integer.MIN_VALUE)
public class ApiVersionFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    String path = req.getRequestURI();

    if (path.startsWith("/api/v1/")) {
      String newPath = "/api/" + path.substring("/api/v1/".length());
      chain.doFilter(new ForwardedRequestWrapper(req, newPath), response);
    } else {
      chain.doFilter(request, response);
    }
  }

  private static class ForwardedRequestWrapper extends HttpServletRequestWrapper {
    private final String newPath;

    ForwardedRequestWrapper(HttpServletRequest request, String newPath) {
      super(request);
      this.newPath = newPath;
    }

    @Override
    public String getRequestURI() {
      return newPath;
    }

    @Override
    public String getServletPath() {
      return newPath;
    }

    @Override
    public String getContextPath() {
      return "";
    }
  }
}
