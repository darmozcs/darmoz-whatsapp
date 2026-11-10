package com.darmoz.whatsapp.config;

import com.darmoz.whatsapp.security.DarmozTokenVerifier;
import com.darmoz.whatsapp.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * El webhook es publico (Meta no manda JWT): cada POST se valida con la firma
 * X-Hub-Signature-256 de la cuenta. Todo lo demas es la API admin del
 * dashboard y requiere el rol SUPER de darmoz-auth: sin token 401, sin el rol 403.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DarmozTokenVerifier tokenVerifier,
                                           DarmozAuthProperties properties) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/webhook/**").permitAll()
                        // Interno: lo valida SendMessageService con el X-Client-Id.
                        .requestMatchers(HttpMethod.POST, "/messages").permitAll()
                        // Para que los errores (404, 400...) no se conviertan en 401.
                        .requestMatchers("/error").permitAll()
                        .anyRequest().hasRole(properties.adminRole()))
                .addFilterBefore(new JwtAuthenticationFilter(tokenVerifier),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
