package com.monkey.ams.common.auth.web;

import com.monkey.ams.common.auth.context.UserContext;
import com.monkey.ams.common.auth.model.LoginSession;
import com.monkey.ams.common.auth.properties.AuthProperties;
import com.monkey.ams.common.auth.service.AuthSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

    private final AuthProperties properties;

    private final AuthSessionService sessionService;

    private final AntPathMatcher pathMatcher =
            new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        try {

            String requestPath = request.getRequestURI();

            // 白名单
            if (isExcludePath(requestPath)) {
                filterChain.doFilter(request, response);
                return;
            }

            String token =
                    resolveToken(request);

            if (token == null) {
                unauthorized(response);
                return;
            }

            LoginSession session = sessionService.getSession(token);

            if (session == null) {
                unauthorized(response);
                return;
            }

            UserContext.set(session);

            filterChain.doFilter(
                    request,
                    response
            );

        } finally {

            UserContext.clear();
        }
    }

    private String resolveToken(
            HttpServletRequest request) {

        String authorization =
                request.getHeader(
                        properties.getHeaderName()
                );

        if (authorization == null) {
            return null;
        }

        if (!authorization.startsWith(
                properties.getTokenPrefixValue())) {
            return null;
        }

        return authorization.substring(
                properties.getTokenPrefixValue().length()
        );
    }

    private boolean isExcludePath(String path) {

        return properties.getExcludePaths()
                .stream()
                .anyMatch(pattern ->
                        pathMatcher.match(
                                pattern,
                                path
                        )
                );
    }

    private void unauthorized(
            HttpServletResponse response)
            throws IOException {

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setContentType(
                "application/json;charset=UTF-8"
        );

        response.getWriter().write(
                "{\"success\":false,\"code\":\"401\",\"message\":\"未登录或登录已过期\"}"
        );
    }
}
