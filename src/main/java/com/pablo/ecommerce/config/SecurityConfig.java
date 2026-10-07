package com.pablo.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/usuarios", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/index.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/produtos", "/produtos/*", "/categorias").permitAll()
                        .requestMatchers(HttpMethod.POST, "/categorias").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/produtos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/produtos/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/produtos/*").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> List.of(new SimpleGrantedAuthority(
                "ADMIN".equals(jwt.getClaimAsString("papel")) ? "ROLE_ADMIN" : "ROLE_CLIENTE")));
        return converter;
    }
}
