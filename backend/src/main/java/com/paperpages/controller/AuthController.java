package com.paperpages.controller;

import com.paperpages.dto.LoginRequest;
import com.paperpages.dto.LoginResponse;
import com.paperpages.dto.Result;
import com.paperpages.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 博主登录，返回 JWT。 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    /** 校验当前 token 是否有效，返回登录名。 */
    @GetMapping("/me")
    public Result<Map<String, String>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Result.ok(Map.of("username", auth == null ? "" : auth.getName()));
    }
}
