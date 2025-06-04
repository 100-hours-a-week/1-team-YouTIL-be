package com.youtil.Api.Interview.Service;

import com.youtil.Api.Interview.Converter.InterviewConverter;
import static com.youtil.Api.Interview.Converter.InterviewConverter.toGetInterviewsResponse;
import static com.youtil.Api.Interview.Converter.InterviewConverter.toInterviewQuestion;
import com.youtil.Api.Interview.dto.InterviewResponseDTO;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.CreateInterviewAIResponse;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.GetInterviewQuestionItem;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.InterviewQuestionResponse;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.InterviewsItem;
import com.youtil.Api.Interview.dto.interviewRequestDTO.CreateInterviewAIRequest;
import com.youtil.Api.Interview.dto.interviewRequestDTO.CreateInterviewRequest;
import com.youtil.Common.Enums.Level;
import com.youtil.Common.Enums.Status;
import com.youtil.Exception.InterviewException.InterviewException.InterviewNotMatchException;
import com.youtil.Model.Interview;
import com.youtil.Model.InterviewQuestion;
import com.youtil.Model.Til;
import com.youtil.Model.User;
import com.youtil.Repository.InterviewQuestionRepository;
import com.youtil.Repository.InterviewRepository;
import com.youtil.Repository.TilRepository;
import com.youtil.Repository.UserRepository;
import com.youtil.Util.EntityValidator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterViewService {

    private final UserRepository userRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final WebClient webClient;
    private final EntityValidator entityValidator;
    private final TilRepository tilRepository;

    @Value("${ai.api.url.primary}")
    private String primaryAiApiUrl;

    @Value("${ai.api.url.secondary}")
    private String secondaryAiApiUrl;

    /**
     * 현재 시간에 따라 적절한 AI 서버 URL을 반환합니다. 한국 시간(KST) 기준으로 판단합니다. 오후 3시(15:00) ~ 오전 12시(24:00/00:00) :
     * primary 서버 사용 오전 12시(00:00) ~ 오후 3시(15:00) : secondary 서버 사용
     */
    private String getActiveAiServerUrl() {
        // 한국 시간대로 현재 시간 가져오기
        ZonedDateTime koreaTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalTime currentTime = koreaTime.toLocalTime();

        LocalTime afternoonThree = LocalTime.of(15, 0); // 오후 3시
        LocalTime midnight = LocalTime.of(0, 0); // 자정

        // 오후 3시부터 자정까지는 primary 서버 사용
        if (currentTime.isAfter(afternoonThree) || currentTime.equals(afternoonThree)) {
            log.debug("현재 한국 시간 {}로 primary AI 서버 사용: {}", currentTime, primaryAiApiUrl);
            return primaryAiApiUrl;
        }
        // 자정부터 오후 3시까지는 secondary 서버 사용
        else {
            log.debug("현재 한국 시간 {}로 secondary AI 서버 사용: {}", currentTime, secondaryAiApiUrl);
            return secondaryAiApiUrl;
        }
    }

    public Long createInterview(CreateInterviewRequest request, long userId) {
//        final String AI_BASE_URL = getActiveAiServerUrl();
        final String AI_BASE_URL = "http://35.225.5.131:8000";
        User user = entityValidator.getValidUserOrThrow(userId);
        Til til = entityValidator.getValidTilOrThrow(request.getTilId());

        CreateInterviewAIRequest createInterviewAIRequest = CreateInterviewAIRequest.builder()
                .title(til.getTitle())
                .email(user.getEmail())
                .level(request.getLevel())
                .keywords(til.getTag())
                .til(til.getContent())
                .category(til.getCategory())
                .build();
        CreateInterviewAIResponse createInterviewAIResponse = webClient.post()
                .uri(AI_BASE_URL + "/interview")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createInterviewAIRequest)
                .retrieve()
                .bodyToMono(CreateInterviewAIResponse.class)
                .block();
        Level level = Level.EASY;
        switch (request.getLevel()) {
            case 1:
                level = Level.HARD;
                break;
            case 2:
                level = Level.NORMAL;
                break;
            case 3:
                level = Level.EASY;
                break;
        }
        Interview interview = InterviewConverter.toInterview(til,
                createInterviewAIResponse.getSummary(), level);
        Interview newInterview = interviewRepository.save(interview);
        for (InterviewQuestionResponse interviewQuestionResponse : createInterviewAIResponse.getContent()) {
            InterviewQuestion interviewQuestion = toInterviewQuestion(newInterview,
                    interviewQuestionResponse.getQuestion(), interviewQuestionResponse.getAnswer());

            interviewQuestionRepository.save(interviewQuestion);
        }
        return newInterview.getId();
    }

    public InterviewResponseDTO.GetInterviewsResponse getInterviews(long userId, Pageable pageable,
            LocalDate date) {

        User user = entityValidator.getValidUserOrThrow(userId);
        List<InterviewsItem> interviewsItems = interviewRepository.findAllUserInterviewByDate(user,
                pageable, date);

        return toGetInterviewsResponse(interviewsItems);

    }

    public InterviewResponseDTO.GetInterviewResponse getInterview(long interviewId) {
        Interview interview = entityValidator.getValidInterviewOrThrow(interviewId);

        List<GetInterviewQuestionItem> questions = interviewQuestionRepository.findAllInterviewQuestionsByInterview(
                interview.getId());

        return InterviewConverter.toGetInterviewResponse(interview, questions);
    }

    @Transactional
    public void inactivateInterview(long userId, long interviewId) {
        User user = entityValidator.getValidUserOrThrow(userId);
        Interview interview = entityValidator.getValidInterviewOrThrow(interviewId);
        if (!Objects.equals(user.getId(), interview.getTil().getUser().getId())) {
            throw new InterviewNotMatchException();
        }

        interview.setStatus(Status.deactive);
        interview.setDeletedAt(LocalDateTime.now());

    }
}
