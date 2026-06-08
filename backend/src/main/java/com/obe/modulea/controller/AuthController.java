package com.obe.modulea.controller;

import com.obe.common.Result;
import com.obe.modulea.dto.LoginRequest;
import com.obe.modulea.dto.LoginResponse;
import com.obe.modulea.dto.PasswordChangeRequest;
import com.obe.modulea.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(request);
        return Result.ok();
    }

    @GetMapping("/info")
    public Result<LoginResponse> getCurrentUser() {
        Long userId = authService.getCurrentUserId();
        return Result.ok(null); // TODO: return current user info
    }
}
