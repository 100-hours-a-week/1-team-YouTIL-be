package com.youtil.Api.User.Controller;

import com.youtil.Api.User.Dto.UserRequestDTO;
import com.youtil.Api.User.Dto.UserResponseDTO;
import com.youtil.Api.User.Service.UserService;
import com.youtil.Common.ApiResponse;
import com.youtil.Util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "user", description = "유저 관련 API")
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "github 로그인", description = "깃허브 소셜 로그인입니다.")
    @PostMapping("/github")
    public ApiResponse<UserResponseDTO.LoginResponseDTO> loginUserController(
            @RequestBody UserRequestDTO.LoginRequestDTO loginRequestDTO) {

        return new ApiResponse<>("로그인에 성공했습니다!", "200",
                userService.loginUserService(loginRequestDTO.getAuthorizationCode()));
    }

    @Operation(summary = "유저 정보 조회", description = "마이페이지의 유저 정보를 조회하는 API 입니다")
    @GetMapping("/{userId}")
    public ApiResponse<UserResponseDTO.GetUserInfoResponseDTO> getUserController(
            @Parameter(name = "userId", description = "유저 아이디 입력입니다.", required = true, example = "382")
            @PathVariable Long userId) {
        return new ApiResponse<>("유저 조회에 성공했습니다!", "200", userService.getUserInfoService(userId));

    }

    @Operation(summary = "유저 탈퇴", description = "유저 탈퇴를 조회하는 API 입니다.")
    @DeleteMapping("")
    public ApiResponse<String> deleteUserController() {
        userService.inactiveUserService(JwtUtil.getAuthenticatedUserId());
        return new ApiResponse<>("유저 탈퇴에 성공했습니다!", "200");
    }

    @Operation(summary = "유저 til 기록 조회", description = "유저 til 기록을 조회하는 API 입니다.")
    @GetMapping("/tils")
    public ApiResponse<UserResponseDTO.GetUserTilCountResponseDTO> getUserTilCountController(
            @Parameter(name = "year", description = "연도입니다", required = true, example = "2025")
            @RequestParam Integer year) {

        return new ApiResponse<>("유저 til 조회에 성공했습니다!", "200",
                userService.getUserTilCountService(JwtUtil.getAuthenticatedUserId(), year));
    }

}
