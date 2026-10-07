package com.pablo.ecommerce.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        long minute = Instant.now().getEpochSecond() / 60;
        String path = request.getRequestURI();
        boolean auth = "/auth/login".equals(path) || "/usuarios".equals(path);
        boolean read = "GET".equals(request.getMethod());
        int limit = auth ? 10 : read ? 600 : 120;
        String key = request.getRemoteAddr() + (auth ? ":auth" : read ? ":read" : ":write");
        if (windows.size() >= 20000 && !windows.containsKey(key)) {
            response.sendError(429);
            return;
        }
        Window current = windows.compute(key, (ignored, old) ->
                old == null || old.minute != minute ? new Window(minute, 1) : new Window(minute, old.count + 1));
        if (current.count > limit) {
            response.setHeader("Retry-After", Long.toString(60 - Instant.now().getEpochSecond() % 60));
            response.sendError(429);
            return;
        }
        chain.doFilter(request, response);
    }

    @Scheduled(fixedDelay = 300000)
    public void limpar() {
        long minimo = Instant.now().getEpochSecond() / 60 - 1;
        windows.entrySet().removeIf(entry -> entry.getValue().minute < minimo);
    }

    private record Window(long minute, int count) {}
}
