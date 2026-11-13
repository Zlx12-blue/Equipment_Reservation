package com.lab.reservation.security;

import com.lab.reservation.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parse(header.substring(7));
                Long userId = Long.valueOf(claims.getSubject());
                // 已删除或已禁用的账号立即失效,不必等待 token 过期
                userRepository.findById(userId)
                        .filter(u -> u.isEnabled())
                        .ifPresent(user -> {
                            var authentication = new UsernamePasswordAuthenticationToken(
                                    userId, null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        });
            } catch (Exception ignored) {
                // 无效 token 按未认证处理
            }
        }
        chain.doFilter(request, response);
    }
}
