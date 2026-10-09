package com.dahamdi.travelmanagement.api;

import com.dahamdi.travelmanagement.api.ApiDtos.LoginRequest;
import com.dahamdi.travelmanagement.api.ApiDtos.RegisterRequest;
import com.dahamdi.travelmanagement.api.ApiDtos.UserDto;
import com.dahamdi.travelmanagement.config.AuthInterceptor;
import com.dahamdi.travelmanagement.entity.User;
import com.dahamdi.travelmanagement.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Login for the Vite frontend. Uses the same HTTP session as the Thymeleaf pages,
 * so the AuthInterceptor protects both in the same way.
 * /api/auth/** is public (excluded in WebConfig).
 */
@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final UserService userService;

    public ApiAuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body, HttpServletRequest request) {
        Optional<User> userOpt = userService.authenticate(body.username(), body.password());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Wrong username or password."));
        }

        HttpSession session = request.getSession(true);
        request.changeSessionId();
        User sessionUser = UserService.toSessionUser(userOpt.get());
        session.setAttribute(AuthInterceptor.SESSION_USER, sessionUser);

        return ResponseEntity.ok(UserDto.of(sessionUser));
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody RegisterRequest body) {
        User user = userService.register(body.username(), body.password(), body.fullName(), body.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.of(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute(AuthInterceptor.SESSION_USER) : null;
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not logged in."));
        }
        return ResponseEntity.ok(UserDto.of(user));
    }
}
