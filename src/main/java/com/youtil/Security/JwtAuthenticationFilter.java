package com.youtil.Security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 변환기
    private final List<String> excludedPaths; // ✅ 필터 제외할 경로 리스트

    public JwtAuthenticationFilter(JwtUtil jwtUtil, List<String> excludedPaths) {
        this.jwtUtil = jwtUtil;
        this.excludedPaths = excludedPaths;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // 인증 제외 경로는 그대로 유지
        if (!method.equals("DELETE") && isExcludedPath(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        String token = resolveAccessToken(httpRequest);

        if (token == null) {
            handleMissingOrInvalidAccessToken(httpRequest, httpResponse, chain);
            return;
        }

        try {
            authenticateFromToken(token);
            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            handleExpiredAccessToken(httpRequest, httpResponse, chain);
        } catch (MalformedJwtException e) {
            sendErrorResponse(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 토큰 형식입니다.");
        }catch (Exception e) {
            log.error("JWT 인증 실패", e);
            sendErrorResponse(httpResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "서버 내부 오류입니다.");
        }
    }

    private String extractRefreshTokenFromCookies(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("RefreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }

//        response.reset();
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("status", status);
        errorDetails.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
        response.getWriter().flush();
    }

    private boolean isExcludedPath(String uri) {
        return excludedPaths.stream().anyMatch(uri::startsWith);
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }

    private void handleMissingOrInvalidAccessToken(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws IOException {

        String refreshToken = extractRefreshTokenFromCookies(request.getCookies());
        if (refreshToken != null) {
            if (jwtUtil.isTokenBlacklisted(refreshToken)) {
                expireRefreshTokenCookie(response, request);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "무효화된 Refresh Token입니다.");
                return;
            }
            try {
                String userId = jwtUtil.validateToken(refreshToken).getSubject();
                String newAccessToken = jwtUtil.generateAccessToken(Long.parseLong(userId));

                sendAccessTokenOnly(response, request, newAccessToken);
            } catch (Exception e) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Refresh Token이 유효하지 않습니다.");
            }
        } else {
            sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "토큰이 존재하지 않습니다.");
        }
    }

    private void handleExpiredAccessToken(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws IOException {

        String refreshToken = extractRefreshTokenFromCookies(request.getCookies());
        if (refreshToken != null) {
            if (jwtUtil.isTokenBlacklisted(refreshToken)) {
                expireRefreshTokenCookie(response, request);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "무효화된 Refresh Token입니다.");
                return;
            }
            try {
                String userId = jwtUtil.validateToken(refreshToken).getSubject();
                String newAccessToken = jwtUtil.generateAccessToken(Long.parseLong(userId));

                sendAccessTokenOnly(response, request, newAccessToken);
            } catch (Exception e) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Refresh Token이 유효하지 않습니다.");
            }
        } else {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Access Token이 만료되었습니다. Refresh Token을 사용해 주세요.");
        }
    }

    private void authenticateFromToken(String token) {
        Claims claims = jwtUtil.validateToken(token);
        String userId = claims.getSubject();
        setAuthentication(userId);
    }

    private void authenticateAndRespond(String userId, HttpServletResponse response,
            String newAccessToken) {
        setAuthentication(userId);
        response.setHeader("Authorization", "Bearer " + newAccessToken);
        response.setHeader("Access-Control-Expose-Headers", "Authorization");
    }

    private void setAuthentication(String userId) {
        UserDetails userDetails = new User(userId, "", Collections.emptyList());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null,
                        userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void sendAccessTokenOnly(HttpServletResponse response, HttpServletRequest request,
            String accessToken)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        String origin = request.getHeader("Origin");

//        response.reset();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("Authorization", "Bearer " + accessToken); // 헤더에 새 토큰 삽입
        response.setHeader("Access-Control-Expose-Headers", "Authorization"); // CORS 대응
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Credentials", "true");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", HttpServletResponse.SC_UNAUTHORIZED);
        result.put("message", "Access Token이 만료되어 새 토큰이 발급되었습니다.");

        response.getWriter().write(objectMapper.writeValueAsString(result));
        response.getWriter().flush();
    }

    private void expireRefreshTokenCookie(HttpServletResponse response,
            HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String domain = getValidDomain(origin);

        ResponseCookie expiredCookie = ResponseCookie.from("RefreshToken", "")
                .domain(domain)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", expiredCookie.toString());
    }

    private String getValidDomain(String origin) {
        if (origin == null) {
            return ".youtil.co.kr";
        }

        if (origin.contains("localhost")) {
            return "localhost";
        } else if (origin.contains("youtil.co.kr")) {
            return ".youtil.co.kr";
        } else {
            return "35.216.71.138";
        }

    }
}
