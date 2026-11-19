package io.flagship.server.security;

import io.flagship.server.domain.Environment;
import io.flagship.server.repository.EnvironmentRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final EnvironmentRepository environmentRepository;

    public ApiKeyAuthenticationFilter(EnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/sdk/")) {
            String apiKey = request.getHeader("X-FlagShip-Key");
            if (apiKey == null || apiKey.isBlank()) {
                apiKey = request.getParameter("apiKey");
            }

            if (apiKey != null && !apiKey.isBlank()) {
                Optional<Environment> envOpt = environmentRepository.findByApiKey(apiKey);
                if (envOpt.isPresent()) {
                    Environment env = envOpt.get();
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            "sdk:" + env.getProject().getProjectKey() + ":" + env.getEnvKey(),
                            apiKey,
                            List.of(new SimpleGrantedAuthority("ROLE_SDK_CLIENT"))
                    );
                    auth.setDetails(env);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
