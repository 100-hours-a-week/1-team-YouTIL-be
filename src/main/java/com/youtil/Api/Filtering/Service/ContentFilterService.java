package com.youtil.Api.Filtering.Service;

import com.youtil.Api.Filtering.Dto.FilterRequestDto;
import com.youtil.Api.Filtering.Dto.FilterResponseDto;
import com.youtil.Common.Enums.GuestbookStatus;
import com.youtil.Common.Enums.Status;
import com.youtil.Concurrency.GpuTimeChecker;
import com.youtil.Model.Comment;
import com.youtil.Model.Guestbook;
import com.youtil.Repository.CommentRepository;
import com.youtil.Repository.GuestbookRepository;
import com.youtil.Util.EntityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentFilterService {

    private final CommentRepository commentRepository;
    private final GuestbookRepository guestbookRepository;
    private final WebClient webClient;
    private final EntityValidator entityValidator;

    @Value("${ai.api.url.primary}")
    private String primaryAiApiUrl;

    @Value("${ai.api.url.secondary}")
    private String secondaryAiApiUrl;

    /**
     * 현재 시간에 따라 적절한 AI 서버 URL을 반환합니다. 한국 시간(KST) 기준으로 판단합니다. 오후 3시(15:00) ~ 오전 12시(24:00/00:00) :
     * primary 서버 사용 오전 12시(00:00) ~ 오후 3시(15:00) : secondary 서버 사용
     */
    private String getActiveAiServerUrl() {

        // 오후 3시부터 자정까지는 primary 서버 사용
        if (GpuTimeChecker.isGpuTimeNow()) {

            return primaryAiApiUrl;
        }
        // 자정부터 오후 3시까지는 secondary 서버 사용
        else {

            return secondaryAiApiUrl;
        }
    }

    @Transactional
    public void CommentFilter(Long commentId, String content, String type) {
        final String AI_BASE_URL = getActiveAiServerUrl();
        FilterRequestDto request = FilterRequestDto.builder()
                .id(commentId)
                .content(content)
                .type(type)
                .build();
        FilterResponseDto isNormal = webClient.post()
                .uri(AI_BASE_URL + "/filter")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(FilterResponseDto.class)
                .block();
        log.info("필터링 로직 실행됨: "+isNormal.getResult());
        if (isNormal.getResult().equals("True")) {
            switch (type) {
                case "GUESTBOOK":
                    Guestbook guestbook = guestbookRepository.findById(commentId).orElse(null);
                    guestbook.setContent("부적절한 표현을 감지해서 필터 봇이 삭제했습니다.");
                    guestbook.setStatus(GuestbookStatus.DEACTIVE);
                    break;
                case "COMMENT":
                    Comment comment = entityValidator.getValidCommentOrThrowException(commentId);
                    comment.setContent("부적절한 표현을 감지해서 필터 봇이 삭제했습니다.");
                    comment.setStatus(Status.deactive);

            }

        }
    }

}
