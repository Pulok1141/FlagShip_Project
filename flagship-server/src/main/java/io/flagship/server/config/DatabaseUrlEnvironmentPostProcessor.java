package io.flagship.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Automatically converts Render / Heroku DATABASE_URL (postgres://user:pass@host:port/db)
 * into standard Spring Boot JDBC properties (spring.datasource.url, username, password)
 * if SPRING_DATASOURCE_URL is not explicitly configured.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUrlEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        // If SPRING_DATASOURCE_URL is already provided explicitly, preserve it
        String explicitUrl = environment.getProperty("SPRING_DATASOURCE_URL");
        if (explicitUrl != null && !explicitUrl.isBlank()) {
            return;
        }

        try {
            Map<String, Object> props = new HashMap<>();
            if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
                String cleanUrl = databaseUrl.replaceFirst("^postgres(ql)?://", "http://");
                URI uri = new URI(cleanUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null) {
                    String[] parts = userInfo.split(":", 2);
                    props.put("spring.datasource.username", parts[0]);
                    if (parts.length > 1) {
                        props.put("spring.datasource.password", parts[1]);
                    }
                }
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath(); // includes leading '/'
                String host = uri.getHost();
                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                props.put("spring.datasource.url", jdbcUrl);
                props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
                log.info("Detected Render/Heroku DATABASE_URL, mapped to JDBC: {}", jdbcUrl);
            } else if (databaseUrl.startsWith("jdbc:")) {
                props.put("spring.datasource.url", databaseUrl);
            }

            if (!props.isEmpty()) {
                environment.getPropertySources().addFirst(new MapPropertySource("renderDatabaseProperties", props));
            }
        } catch (Exception e) {
            log.warn("Failed to parse DATABASE_URL ({}): {}", databaseUrl, e.getMessage());
        }
    }
}
