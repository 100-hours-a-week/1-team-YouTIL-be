package com.youtil.Common.DuplicatePrevention;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Common.ApiResponse;
import com.youtil.Util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.OffsetDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DuplicatePreventionInterceptor implements HandlerInterceptor {

    private final DuplicatePreventionManager duplicatePreventionManager;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        // Handler가 메서드가 아니면 통과
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // @PreventDuplicate 어노테이션 체크
        PreventDuplicate annotation = handlerMethod.getMethodAnnotation(PreventDuplicate.class);

        if (annotation == null) {
            return true; // 어노테이션이 없으면 통과
        }

        try {
            // 사용자 ID 추출
            Long userId = JwtUtil.getAuthenticatedUserId();
            if (userId == null) {
                return true; // 인증되지 않은 사용자는 통과
            }

            // 데이터 해시 생성
            String dataHash = createDataHash(request, annotation);

            log.debug("중복 방지 체크 - action: {}, userId: {}, dataHash: {}",
                    annotation.action(), userId, dataHash);

            // 중복 방지 체크 (기존 DuplicatePreventionManager 활용)
            boolean isAllowed = callPreventDuplicate(annotation.action(), userId, dataHash, annotation);

            if (!isAllowed) {
                // 429 응답 반환
                sendTooManyRequestsResponse(response);
                return false; // 요청 차단
            }

            return true; // 통과

        } catch (Exception e) {
            log.error("중복 방지 인터셉터 오류 - action: {}", annotation.action(), e);
            return true; // 오류 발생시 통과시킴 (안전장치)
        }
    }

    /**
     * URL에서 데이터 해시 생성
     */
    private String createDataHash(HttpServletRequest request, PreventDuplicate annotation) {
        try {
            StringBuilder hashData = new StringBuilder();

            // 1. Query Parameter에서 추출
            for (String field : annotation.dataFields()) {
                String value = request.getParameter(field);
                if (value != null && !value.trim().isEmpty()) {
                    hashData.append(field).append(":").append(value).append("|");
                }
            }

            // 2. Path Variable에서 추출
            Map<String, String> pathVariables = getPathVariables(request);
            if (pathVariables != null) {
                for (String field : annotation.dataFields()) {
                    String value = pathVariables.get(field);
                    if (value != null && !value.trim().isEmpty()) {
                        hashData.append(field).append(":").append(value).append("|");
                    }
                }
            }

            String combined = hashData.toString();
            if (combined.isEmpty()) {
                return "default";
            }

            // 해시값 생성
            return String.valueOf(Math.abs(combined.hashCode()));

        } catch (Exception e) {
            log.warn("데이터 해시 생성 실패", e);
            return "default";
        }
    }

    /**
     * Path Variable 추출
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> getPathVariables(HttpServletRequest request) {
        return (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    }

    /**
     * 액션별로 적절한 중복 방지 메서드 호출
     */
    private boolean callPreventDuplicate(String action, Long userId, String dataHash, PreventDuplicate annotation) {
        // 기존 메서드들과 호환성 유지
        switch (action) {
            case "guestbook":
            case "guestbook_create":
                // 방명록은 기존 메서드 형태 유지 (content는 dataHash로 대체)
                return duplicatePreventionManager.preventGuestbookDuplicate(userId, userId, dataHash, annotation);

            case "til_recommend":
            case "til_like":
                // TIL 추천은 기존 메서드 형태 유지
                Long tilId = parseTilIdFromDataHash(dataHash);
                return duplicatePreventionManager.preventTilRecommendDuplicate(userId, tilId, annotation);

            case "comment":
            case "comment_create":
                // 댓글은 기존 메서드 형태 유지
                Long commentTilId = parseTilIdFromDataHash(dataHash);
                return duplicatePreventionManager.preventCommentDuplicate(userId, commentTilId, dataHash, annotation);

            default:
                // 새로운 액션들은 범용 메서드 사용
                return duplicatePreventionManager.preventDuplicate(action, userId, dataHash, annotation);
        }
    }

    /**
     * dataHash에서 tilId 추출
     */
    private Long parseTilIdFromDataHash(String dataHash) {
        try {
            // dataHash가 "tilId:123|" 형태라면 123 추출
            if (dataHash.startsWith("tilId:")) {
                String[] parts = dataHash.split(":");
                if (parts.length > 1) {
                    String idPart = parts[1].replace("|", "");
                    return Long.parseLong(idPart);
                }
            }
            return 1L; // 기본값
        } catch (Exception e) {
            log.warn("tilId 파싱 실패: {}", dataHash);
            return 1L;
        }
    }

    /**
     * 429 응답 전송
     */
    private void sendTooManyRequestsResponse(HttpServletResponse response) throws Exception {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> errorResponse = ApiResponse.builder()
                .success(false)
                .code("429")
                .message("너무 빠른 요청입니다. 잠시 후 다시 시도해주세요.")
                .responseAt(OffsetDateTime.now())
                .data(null)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
