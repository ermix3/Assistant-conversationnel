package ai.factory.agent_fitness.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class ApiSecurityFilter extends OncePerRequestFilter {
    private final Set<String> origins;
    private final Map<String, Window> clients = new HashMap<>();

    private record Window(long minute, int count) {
    }

    public ApiSecurityFilter(@Value("${club.allowed-origins}") String origins) {
        this.origins = Set.of(origins.split(","));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    private synchronized boolean allow(String ip) {
        long minute = Instant.now().getEpochSecond() / 60;
        clients.entrySet().removeIf(e -> e.getValue().minute() != minute);
        var current = clients.getOrDefault(ip, new Window(minute, 0));
        if (current.count() >= 20 || (!clients.containsKey(ip) && clients.size() >= 10000)) return false;
        clients.put(ip, new Window(minute, current.count() + 1));
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        String origin = request.getHeader("Origin");
        if (origin != null) {
            if (!origins.contains(origin)) {
                response.sendError(403);
                return;
            }
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        }
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(204);
            return;
        }
        if ("POST".equals(request.getMethod())) {
            if (!allow(request.getRemoteAddr())) {
                response.setHeader("Retry-After", "60");
                response.sendError(429);
                return;
            }
            if (request.getContentLengthLong() > 16384) {
                response.sendError(413);
                return;
            }
            // Also cap chunked bodies before Jackson can allocate an unbounded JSON string.
            byte[] body = request.getInputStream().readNBytes(16385);
            if (body.length > 16384) {
                response.sendError(413);
                return;
            }
            var wrapped = new HttpServletRequestWrapper(request) {
                @Override
                public ServletInputStream getInputStream() {
                    var input = new java.io.ByteArrayInputStream(body);
                    return new ServletInputStream() {
                        @Override
                        public int read() {
                            return input.read();
                        }

                        @Override
                        public boolean isFinished() {
                            return input.available() == 0;
                        }

                        @Override
                        public boolean isReady() {
                            return true;
                        }

                        @Override
                        public void setReadListener(ReadListener listener) {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
            };
            chain.doFilter(wrapped, response);
            return;
        }
        chain.doFilter(request, response);
    }
}
