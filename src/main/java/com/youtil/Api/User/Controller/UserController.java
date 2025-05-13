package com.youtil.Api.User.Controller;

import com.youtil.Api.User.Dto.UserRequestDTO;
import com.youtil.Api.User.Dto.UserResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO.GetUserTilsResponseDTO;
import com.youtil.Api.User.Service.UserService;
import com.youtil.Common.ApiResponse;
import com.youtil.Common.Enums.MessageCode;
import com.youtil.Util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Tag(name = "user", description = "유저 관련 API")
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/github")
    public ApiResponse<UserResponseDTO.LoginResponseDTO> loginUserController(
            @RequestBody UserRequestDTO.LoginRequestDTO loginRequestDTO,
            HttpServletRequest request, HttpServletResponse response) {

        String origin = request.getHeader("Origin");
        UserResponseDTO.LoginResponseDTO tokens = userService.loginUserService(
                loginRequestDTO.getAuthorizationCode(), origin);

        ResponseCookie refreshTokenCookie = ResponseCookie.from("RefreshToken",
                        tokens.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("None")  // 또는 "Lax" 필요 시 변경
                .build();

        response.addHeader("Set-Cookie", refreshTokenCookie.toString());

        return new ApiResponse<>(MessageCode.LOGIN_SUCCESS.getMessage(), "200", tokens);
    }

    @Operation(summary = "유저 정보 조회", description = "마이페이지의 유저 정보를 조회하는 API 입니다")
    @GetMapping("")
    public ApiResponse<UserResponseDTO.GetUserInfoResponseDTO> getUserController(
            @Parameter(name = "userId", description = "유저 아이디 입력입니다.", required = false)
            @RequestParam(required = false) Long userId) {

        if (userId == null) {
            return new ApiResponse<>(MessageCode.FIND_USER_INFORMATION_SUCCESS.getMessage(), "200",
                    userService.getUserInfoService(JwtUtil.getAuthenticatedUserId()));
        }
        return new ApiResponse<>(MessageCode.FIND_USER_INFORMATION_SUCCESS.getMessage(), "200",
                userService.getUserInfoService(userId));

    }

    @Operation(summary = "유저 탈퇴", description = "유저 탈퇴를 조회하는 API 입니다.")
    @DeleteMapping("")
    public ApiResponse<String> deleteUserController() {
        userService.inactiveUserService(JwtUtil.getAuthenticatedUserId());
        return new ApiResponse<>(MessageCode.USER_DEACTIVE.getMessage(), "200");
    }

    @Operation(summary = "유저 til 기록 조회", description = "유저 til 기록을 조회하는 API 입니다.")
    @GetMapping("/tils")
    public ApiResponse<UserResponseDTO.GetUserTilCountResponseDTO> getUserTilCountController(
            @Parameter(name = "year", description = "연도입니다", required = true, example = "2025")
            @RequestParam Integer year) {

        return new ApiResponse<>(MessageCode.FIND_USER_TILS__COUNT_SUCCESS.getMessage(), "200",
                userService.getUserTilCountService(JwtUtil.getAuthenticatedUserId(), year));
    }

    @Operation(summary = "유저 til 작성 글 조회", description = "유저 til 작성 글을 조회하는 API 입니다.")
    @GetMapping("/{userId}/tils")
    public ApiResponse<GetUserTilsResponseDTO> getUserTilsController(
            @Parameter(name = "userId", description = "조회하고자 하는 유저 아이디입니다.", required = true, example = "1")
            @PathVariable Long userId,
            @Parameter(name = "page", description = "조회하고자 하는 페이지 입니다.", required = false, example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(name = "offset", description = "조회하고자 하는 아이템 개수 입니다.", required = false, example = "20")
            @RequestParam(defaultValue = "20") int offset) {

        Pageable pageable = PageRequest.of(page, offset);
        return new ApiResponse<>(MessageCode.FIND_USER_WRITE_TILS_SUCCESS.getMessage(), "200",
                userService.getUserTilsService(userId, pageable));
    }
}
