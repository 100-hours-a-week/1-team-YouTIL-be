package com.youtil.Api.Interview.Service;

import com.youtil.Api.Interview.dto.InterviewRequestDTO.CreateInterviewRequest;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.CreateInterviewAIResponse;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.GetInterviewQuestionItem;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.GetInterviewResponse;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.GetInterviewsResponse;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.InterviewsItem;
import com.youtil.Common.Enums.Status;
import static com.youtil.Constants.MockInterviewConstants.DEFAULT_INTERVIEW_COUNT;
import static com.youtil.Constants.MockInterviewConstants.DEFAULT_PAGE_REQUEST;
import static com.youtil.Constants.MockInterviewConstants.DEFAULT_QUESTION_COUNT;
import static com.youtil.Constants.MockInterviewConstants.TODAY;
import static com.youtil.Constants.MockUserConstants.MOCK_GITHUB_TOKEN;
import static com.youtil.Constants.MockUserConstants.MOCK_USER_EMAIL;
import static com.youtil.Constants.MockUserConstants.MOCK_USER_NICKNAME;
import static com.youtil.Constants.MockUserConstants.MOCK_USER_PROFILE;
import com.youtil.Exception.InterviewException.InterviewException.InterviewNotFoundException;
import com.youtil.Exception.InterviewException.InterviewException.InterviewNotMatchException;
import com.youtil.Exception.TilException.TilException.TilNotFoundException;
import com.youtil.Exception.UserException.UserException.UserNotFoundException;
import static com.youtil.Mock.MockInterviewAIResponseBuilder.createInterviewAIResponse;
import static com.youtil.Mock.MockInterviewBuilder.createMockInterview;
import static com.youtil.Mock.MockUserBuilder.createMockUser;
import static com.youtil.Mock.TilMockBuilder.createMockTil;
import com.youtil.Model.Interview;
import com.youtil.Model.InterviewQuestion;
import com.youtil.Model.Til;
import com.youtil.Model.User;
import com.youtil.Repository.InterviewQuestionRepository;
import com.youtil.Repository.InterviewRepository;
import com.youtil.Repository.TilRepository;
import com.youtil.Repository.UserRepository;
import com.youtil.Util.EntityValidator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class InterviewServiceTest {


    @Mock
    UserRepository userRepository;
    @Mock
    TilRepository tilRepository;
    @Mock
    InterviewRepository interviewRepository;
    @Mock
    InterviewQuestionRepository interviewQuestionRepository;
    @InjectMocks
    InterViewService interViewService;
    @Mock
    private WebClient webClient;
    @Mock
    private EntityValidator entityValidator;
    private User mockUser;
    private Til mockTil;
    private Interview mockInterview;
    private InterviewQuestion mockInterviewQuestion;
    private CreateInterviewAIResponse createInterviewAIResponse;


    private WebClient.RequestBodyUriSpec uriSpec;
    private WebClient.RequestHeadersSpec headersSpec;
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    void setup() {
        mockUser = createMockUser();
        mockTil = createMockTil(mockUser);
        createInterviewAIResponse = createInterviewAIResponse();
        mockInterview = createMockInterview(mockTil, createInterviewAIResponse.getSummary());

    }

    @Test
    @DisplayName("면접 질문 생성 - 모든 값이 있을 경우 - 성공")
    void createInterview_withValidCondition_success() {
        // Arrange

        CreateInterviewRequest request = CreateInterviewRequest.builder()
                .tilId(mockTil.getId())
                .level(3) // EASY
                .build();

        // EntityValidator 모킹
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);
        when(entityValidator.getValidTilOrThrow(mockTil.getId())).thenReturn(mockTil);

        // webClient 모킹
        setupWebClient();
        when(responseSpec.bodyToMono(CreateInterviewAIResponse.class)).thenReturn(
                Mono.just(createInterviewAIResponse));
        //면접 질문 저장 모킹
        when(interviewRepository.save(any(Interview.class))).thenReturn(mockInterview);

        Long resultId = interViewService.createInterview(request, mockUser.getId());

        assertEquals(mockInterview.getId(), resultId);
        verify(interviewRepository).save(any(Interview.class));
        verify(interviewQuestionRepository, times(2)).save(any(InterviewQuestion.class));
    }

    @Test
    @DisplayName("면접 질문 생성 - 면접 질문 서버에서 오류가 날 경우 - 실패")
    void createInterview_withErrorInAIServer_fail() {
        CreateInterviewRequest request = CreateInterviewRequest.builder()
                .tilId(mockTil.getId())
                .level(3)
                .build();
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);
        when(entityValidator.getValidTilOrThrow(mockTil.getId())).thenReturn(mockTil);
        setupWebClient();
        when(responseSpec.bodyToMono(CreateInterviewAIResponse.class))
                .thenThrow(new WebClientResponseException(
                        500, "Internal Server Error", null, null, null));

        assertThatThrownBy(
                () -> interViewService.createInterview(request, mockUser.getId())
        ).isInstanceOf(WebClientResponseException.class);
    }

    @Test
    @DisplayName("면접 질문 생성 - TIL이 유효하지 않은경우 - 실패")
    void createInterview_withInValidTIL_fail() {
        CreateInterviewRequest request = CreateInterviewRequest.builder()
                .tilId(mockTil.getId())
                .level(3) // EASY
                .build();

        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);
        when(entityValidator.getValidTilOrThrow(mockTil.getId())).thenThrow(
                TilNotFoundException.class);

        assertThatThrownBy(
                () -> interViewService.createInterview(request, mockUser.getId())
        ).isInstanceOf(TilNotFoundException.class);
    }

    @Test
    @DisplayName("면접 질문 생성 -유저가 유효하지 않은 경우 - 실패")
    void createInterview_withInValidUser_fail() {
        CreateInterviewRequest request = CreateInterviewRequest.builder()
                .tilId(mockTil.getId())
                .level(3) // EASY
                .build();
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenThrow(
                UserNotFoundException.class);

        assertThatThrownBy(
                () -> interViewService.createInterview(request, mockUser.getId())
        ).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("면접 리스트 조회 - 값이 있고 유저 아이디가 유효할 경우 - 성공")
    void getInterviews_withValidUserAndInterviews_success() {

        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);
        List<InterviewsItem> interviewsItems = generateMockInterviewItems();
        when(interviewRepository.findAllUserInterviewByDate(mockUser, DEFAULT_PAGE_REQUEST,
                TODAY)).thenReturn(
                interviewsItems);

        GetInterviewsResponse getInterviewsResponse = interViewService.getInterviews(
                mockUser.getId(), DEFAULT_PAGE_REQUEST, TODAY);

        for (int i = 0; i < DEFAULT_INTERVIEW_COUNT; i++) {
            InterviewsItem expectedItem = interviewsItems.get(i);
            InterviewsItem actualItem = getInterviewsResponse.getInterviews().get(i);

            assertEquals(expectedItem.getId(), actualItem.getId());
            assertEquals(expectedItem.getTitle(), actualItem.getTitle());
            assertEquals(expectedItem.getLevel(), actualItem.getLevel());
            assertEquals(expectedItem.getCreatedAt(), actualItem.getCreatedAt());
        }
    }

    @Test
    @DisplayName("면접 리스트 조회 - 값이 존재하지 않고 유저가 유효한경우 - 성공")
    void getInterviews_withValidUserAndInvalidInterviews_success() {

        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);

        when(interviewRepository.findAllUserInterviewByDate(mockUser, DEFAULT_PAGE_REQUEST,
                TODAY)).thenReturn(
                Collections.emptyList());
        GetInterviewsResponse getInterviewsResponse = interViewService.getInterviews(
                mockUser.getId(), DEFAULT_PAGE_REQUEST, TODAY);

        assertNotNull(getInterviewsResponse);
        assertTrue(getInterviewsResponse.getInterviews().isEmpty());
    }

    @Test
    @DisplayName("면접 리스트 조회 - 유저가 유효하지 않은 경우 - 실패")
    void getInterviews_withInvalidUser_fail() {

        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenThrow(
                UserNotFoundException.class);
        assertThatThrownBy(
                () -> interViewService.getInterviews(mockUser.getId(), DEFAULT_PAGE_REQUEST,
                        TODAY)).isInstanceOf(
                UserNotFoundException.class);
    }

    @Test
    @DisplayName("면접 상세 조회 - 면접 Id 가 유효할 때")
    void getInterview_withValidInterviewId_success() {

        List<GetInterviewQuestionItem> mockQuestions = generateMockQuestions(
        );

        when(entityValidator.getValidInterviewOrThrow(mockInterview.getId())).thenReturn(
                mockInterview);
        when(interviewQuestionRepository.findAllInterviewQuestionsByInterview(
                mockInterview.getId())).thenReturn(mockQuestions);

        GetInterviewResponse getInterviewsResponse = interViewService.getInterview(
                mockInterview.getId());
        assertEquals(mockInterview.getId(), getInterviewsResponse.getId());
        assertEquals(mockInterview.getTitle(), getInterviewsResponse.getTitle());
        assertEquals(mockInterview.getLevel().toString(), getInterviewsResponse.getLevel());
        assertEquals(mockInterview.getCreatedAt(), getInterviewsResponse.getCreatedAt());
        for (int i = 0; i < DEFAULT_QUESTION_COUNT; i++) {
            GetInterviewQuestionItem expectedItem = mockQuestions.get(i);
            GetInterviewQuestionItem actualItem = getInterviewsResponse.getQuestions().get(i);

            assertEquals(expectedItem.getQuestionId(), actualItem.getQuestionId());
            assertEquals(expectedItem.getQuestion(), actualItem.getQuestion());
            assertEquals(expectedItem.getAnswer(), actualItem.getAnswer());

        }

    }

    @Test
    @DisplayName("면접 질문 조회 - 면접 ID가 유효하지 않은 경우 - 실패")
    void getInterview_withInvalidInterviewId_fail() {
        when(entityValidator.getValidInterviewOrThrow(mockInterview.getId())).thenThrow(
                InterviewNotFoundException.class);
        assertThatThrownBy(() -> interViewService.getInterview(mockInterview.getId())).isInstanceOf(
                InterviewNotFoundException.class);
    }

    @Test
    @DisplayName("면접 질문 삭제 - 면접 ID 값들과 유저 ID 값들이 유효한경우 - 성공")
    void getInterview_withValidInterviewIdsAndUserId_success() {

        List<Long> interviewIds = new ArrayList<>();
        List<Interview> mockInterviews = new ArrayList<>();

        for (long i = 0; i < DEFAULT_INTERVIEW_COUNT; i++) {
            interviewIds.add(i);
            Interview interview = Interview.builder()
                    .id(i)
                    .til(mockInterview.getTil())
                    .title(mockInterview.getTitle())
                    .level(mockInterview.getLevel())
                    .status(mockInterview.getStatus()).build();
            mockInterviews.add(interview);
            when(entityValidator.getValidInterviewOrThrow(i)).thenReturn(
                    interview);
        }
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);

        interViewService.inactivateInterview(mockUser.getId(), interviewIds);

        for (Interview interview : mockInterviews) {
            assertEquals(Status.deactive, interview.getStatus());
            assertNotNull(interview.getDeletedAt());
            assertTrue(interview.getDeletedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        }
        verify(entityValidator, times(1)).getValidUserOrThrow(mockUser.getId());
        verify(entityValidator, times(DEFAULT_INTERVIEW_COUNT)).getValidInterviewOrThrow(anyLong());
    }

    @Test
    @DisplayName("면접 질문 삭제 - 유저가 소유하지않는 면접이 포함될 경우")
    void deleteInterview_withNotMatchUserAndInterview_fail() {
        User anotherUser = User.builder()
                .id(2L)
                .email(MOCK_USER_EMAIL)
                .status(Status.active)
                .githubToken(MOCK_GITHUB_TOKEN)
                .nickname(MOCK_USER_NICKNAME)
                .profileImageUrl(MOCK_USER_PROFILE)
                .build();
        ;

        Til anotherTil = createMockTil(anotherUser);

        Interview interview = createMockInterview(anotherTil, "temp");

        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);
        when(entityValidator.getValidInterviewOrThrow(interview.getId())).thenReturn(interview);

        assertThatThrownBy(() -> interViewService.inactivateInterview(mockUser.getId(),
                List.of(interview.getId()))).isInstanceOf(
                InterviewNotMatchException.class);
    }

    @Test
    @DisplayName("면접 삭제 - 유저가 유효하지 않을 경우 - 실패")
    void deleteInterview_withInvalidUser_fail() {
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenThrow(
                UserNotFoundException.class);
        assertThatThrownBy(() -> interViewService.inactivateInterview(mockUser.getId(),
                List.of(mockInterview.getId()))).isInstanceOf(UserNotFoundException.class);

    }

    @Test
    @DisplayName("면접 삭제 - 해당 리스트 중 면접 아이디가 유효하지 않을 경우- 실패")
    void deleteInterview_withInvalidInterviewIds_fail() {

        List<Long> interviewIds = new ArrayList<>();

        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);
        when(entityValidator.getValidInterviewOrThrow(mockInterview.getId())).thenThrow(
                InterviewNotFoundException.class);
        for (long i = 0; i < DEFAULT_INTERVIEW_COUNT; i++) {
            interviewIds.add(i);
            when(entityValidator.getValidInterviewOrThrow(i)).thenReturn(mockInterview);
        }
        interviewIds.add(mockInterview.getId());

        assertThatThrownBy(() -> interViewService.inactivateInterview(mockUser.getId(),
                interviewIds)).isInstanceOf(InterviewNotFoundException.class);
    }

    //웹클라이언트 모킹
    private void setupWebClient() {
        uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        headersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(any(String.class))).thenReturn(uriSpec);
        when(uriSpec.contentType(any())).thenReturn(uriSpec);
        when(uriSpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
    }

    private List<InterviewsItem> generateMockInterviewItems() {
        List<InterviewsItem> items = new ArrayList<>();
        for (int i = 0; i < DEFAULT_INTERVIEW_COUNT; i++) {
            items.add(InterviewsItem.builder()
                    .id(i)
                    .title(mockInterview.getTitle())
                    .level(mockInterview.getLevel().toString())
                    .createdAt(mockInterview.getCreatedAt())
                    .build());
        }
        return items;
    }

    private List<GetInterviewQuestionItem> generateMockQuestions() {
        List<GetInterviewQuestionItem> questions = new ArrayList<>();
        for (int i = 0; i < DEFAULT_QUESTION_COUNT; i++) {
            questions.add(GetInterviewQuestionItem.builder()
                    .questionId(i)
                    .question("Q" + i)
                    .answer("A" + i)
                    .build());
        }
        return questions;
    }
}
