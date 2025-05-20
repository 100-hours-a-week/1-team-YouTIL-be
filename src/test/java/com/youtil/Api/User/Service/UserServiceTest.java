package com.youtil.Api.User.Service;

import com.youtil.Common.Enums.Status;
import com.youtil.Model.User;
import com.youtil.Repository.UserRepository;
import com.youtil.Util.EntityValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EntityValidator entityValidator;
    @InjectMocks
    private UserService userService;

    private User createMockUser() {
        User user = User.builder()
                .id(1L)
                .email("test@email.com")
                .status(Status.active)
                .githubToken("accessToken")
                .nickname("nick")
                .profileImageUrl("profile-image")
                .build();
        return user;
    }

    //테스트코드는 행위 기반으로 서술하기 때문에 스네이크 패턴을 혼용해서 사용한다,
    //Ex) methodName_condition_expectedResult()

    //유저 로그인 관련
    @Test
    @DisplayName("유저 로그인 - 로그인 성공")
    void loginUser_withValidCredentials_success() {
    }

    @Test
    @DisplayName("유저 로그인 - 인가코드 잘못됨- 로그인 실패")
    void loginUser_withWrongAuthorizationCode_fail() {

    }

    @Test
    @DisplayName("유저 로그인 - 스코프 권한 없음 - 로그인 실패")
    void loginUser_withWrongScope_fail() {

    }

    //유저 정보 조회
    @Test
    @DisplayName("유저 정보 조회 -값이 있는 경우 - 조회 성공")
    void findUser_withValidUser_success() {

    }

    @Test
    @DisplayName("유저 정보 조회 - 유저가 탈퇴했거나 존재 하지 않는 경우 - 조회 실패")
    void findUser_withInvalidUser_fail() {

    }

    //유저 탈퇴
    @Test
    @DisplayName("유저 탈퇴 - 유저 존재 - 탈퇴 성공")
    void inActiveUser_withValidUser_success() {

    }

    @Test
    @DisplayName("유저 탈퇴 - 유저가 이미 탈퇴했거나 존재하지 않는 경우 - 탈퇴 실패")
    void inActiveUser_withInvalidUser_fail() {

    }

    //유저 TIL 작성 글 조회
    @Test
    @DisplayName("유저 TIL 작성 글 조회 - 유저가 존재하고 값이 존재할 경우 - 조회 성공")
    void findUserPost_withValidUserAndValidPost_success() {

    }

    @Test
    @DisplayName("유저 TIL 작성글 조회 -유저가 존재하고 값이 존재하지 않을 경우 - 조회 성공 ")
    void findUserPost_withInvalidUserAndValidPost_success() {

    }

    @Test
    @DisplayName("유저 TIL 작성글 조회 - 유저가 존재하지 않을 경우 - 조회 실패")
    void findUserPost_withInvalidUser_fail() {

    }


    //유저 TIL 기록 조회
    @Test
    @DisplayName("유저 TIL 기록 조회 - 유저가 존재할 경우 - 조회 성공")
    void findUserTilCount_withValidUser_success() {

    }

    @Test
    @DisplayName("유저 TIL 기록 조회 - 유저가 존재하지 않을 경우 - 조회 실패")
    void findUserTilCount_withInvalidUser_fail() {

    }

}
