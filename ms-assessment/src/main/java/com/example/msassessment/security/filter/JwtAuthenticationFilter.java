package com.example.msassessment.security.filter;

import com.example.msassessment.security.CustomUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        log.debug("🔍 GatewayHeaderFilter - Processing: {} {}", request.getMethod(), request.getRequestURI());

        try {
            String userId = request.getHeader("X-User-Id");
            String username = request.getHeader("X-Username");
            String rolesHeader = request.getHeader("X-User-Roles");

            log.debug("📥 Gateway headers - userId: {}, username: {}, roles: {}",
                    userId != null ? userId : "MISSING",
                    username != null ? username : "MISSING",
                    rolesHeader != null ? rolesHeader : "MISSING");

            if (userId != null && username != null && rolesHeader != null) {
                List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                        .map(String::trim)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                CustomUserPrincipal principal = new CustomUserPrincipal(
                        Long.parseLong(userId),
                        username,
                        rolesHeader
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("✅ Authenticated from gateway: {} (ID: {}) with roles: {}",
                        username, userId, rolesHeader);
            } else {
                log.warn("⚠️ Missing gateway headers - request will be unauthenticated");
            }
        } catch (NumberFormatException e) {
            log.error("❌ Invalid userId format: {}", e.getMessage());
        } catch (Exception e) {
            log.error("❌ Failed to process gateway headers: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}