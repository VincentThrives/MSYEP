package com.vincent.msyep.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    private static final String[] PUBLIC = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/otp/**",
            "/api/v1/public/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(c -> c.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Location master lookups are public (needed by the student self-registration form)
                .requestMatchers(HttpMethod.GET, "/api/v1/locations/**").permitAll()
                // User management: super admin / admin only
                .requestMatchers("/api/v1/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                // Zone writes: super admin / admin; reads allowed to authenticated (scoped in service)
                .requestMatchers(HttpMethod.GET, "/api/v1/zones/**").authenticated()
                .requestMatchers("/api/v1/zones/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "ZONE")
                .requestMatchers(HttpMethod.GET, "/api/v1/centers/**").authenticated()
                .requestMatchers("/api/v1/centers/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "ZONE", "CENTER")
                .requestMatchers(HttpMethod.GET, "/api/v1/students/**").authenticated()
                .requestMatchers("/api/v1/students/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "ZONE", "CENTER")
                .requestMatchers("/api/v1/finance/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "FINANCE", "ZONE", "CENTER")
                .requestMatchers("/api/v1/staff/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF", "ZONE", "CENTER")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                // Unauthenticated (missing / expired / invalid token) → 401 JSON so the
                // client redirects to login. Authenticated-but-forbidden → 403 JSON.
                .authenticationEntryPoint((req, res, e) -> writeError(res, 401,
                        "Your session has expired - please sign in again."))
                .accessDeniedHandler((req, res, e) -> writeError(res, 403, "Access denied")))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void writeError(jakarta.servlet.http.HttpServletResponse res, int status, String message)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"success\":false,\"message\":\"" + message + "\",\"data\":null}");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition", "X-Sow-Note"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
