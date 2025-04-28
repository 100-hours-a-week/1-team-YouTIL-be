package com.youtil.Api.User.Controller;

import com.youtil.Api.User.Dto.UserRequestDTO;
import com.youtil.Api.User.Dto.UserResponseDTO;
import com.youtil.Api.User.Service.UserService;
import com.youtil.Common.ApiResponse;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    @PostMapping("/github")
    public ApiResponse<UserResponseDTO.LoginResponseDTO> LoginUserController(@RequestBody UserRequestDTO.LoginRequestDTO loginRequestDTO){

        return new ApiResponse<>("로그인에 성공했습니다!","200",userService.loginUserService(loginRequestDTO.getAuthorizationCode()));
    }
}
