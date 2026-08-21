package com.paperpages.service;

import com.paperpages.dto.LoginRequest;
import com.paperpages.dto.LoginResponse;
import com.paperpages.entity.AdminUser;
import com.paperpages.exception.BusinessException;
import com.paperpages.repository.AdminUserRepository;
import com.paperpages.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(AdminUserRepository adminUserRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest req) {
        AdminUser user = adminUserRepository.findByUsername(req.username())
                .orElseThrow(() -> BusinessException.unauthorized("用户名或密码错误"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        return new LoginResponse(jwtUtil.generate(user.getUsername()), user.getUsername());
    }
}
