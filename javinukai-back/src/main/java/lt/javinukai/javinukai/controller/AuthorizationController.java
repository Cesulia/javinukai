package lt.javinukai.javinukai.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lt.javinukai.javinukai.config.security.AuthenticationService;
import lt.javinukai.javinukai.dto.request.auth.ForgotPasswordRequest;
import lt.javinukai.javinukai.dto.request.auth.PasswordResetRequest;
import lt.javinukai.javinukai.dto.response.AuthenticationResponse;
import lt.javinukai.javinukai.dto.request.auth.LoginRequest;
import lt.javinukai.javinukai.dto.request.user.UserRegistrationRequest;
import lt.javinukai.javinukai.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
@Validated
public class AuthorizationController {
    private final AuthenticationService authenticationService;

    @Value("${app.constants.security.jwt-cookie-valid-hours}")
    private int jwtValidTimeHours;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid UserRegistrationRequest registration) {
        log.info("Received registration request: {}", registration.toString());
        authenticationService.register(registration);
        return ResponseEntity.created(URI.create("/users")).build();
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody LoginRequest login, HttpServletResponse response) {
        log.info("Received log in request with credentials: {}", login.toString());
        AuthenticationResponse auth = authenticationService.login(login);
        String cookieString = getResponseCookie("jwt", auth.getToken(), jwtValidTimeHours).toString();
        response.addHeader(HttpHeaders.SET_COOKIE, cookieString);
        return ResponseEntity.ok().body(auth.getUser());
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response, @AuthenticationPrincipal UserDetails user) {
        if (user != null) {
        User loggingOutUser = (User) user;
        log.info("Received logout request from user: {}", loggingOutUser.toString());
        }
        String cookieString = getResponseCookie("jwt", "", 0).toString();
        response.addHeader(HttpHeaders.SET_COOKIE, cookieString);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test")
    public ResponseEntity<String> testCookie(@CookieValue("jwt") String jwt, Authentication authentication,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        return ResponseEntity.ok().body(user.getName() + " " + user.getSurname());
    }

    private ResponseCookie getResponseCookie(String name, String value, int hoursValid) {
        return ResponseCookie.from(name, value)
                .maxAge(Duration.ofHours(hoursValid))
                .httpOnly(false)
                .secure(false)
                .sameSite("lax")
                .path("/")
                .build();
    }


    @PostMapping("/confirm-email")
    public ResponseEntity<String> confirmEmail(@RequestParam String token) {
        log.info("Received email confirmation token: {}", token);
        authenticationService.confirmEmail(token);
        return ResponseEntity.ok().body("Email confirmed. You may log in");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid PasswordResetRequest passwordResetRequest) {
       log.info("Received password reset request: {}", passwordResetRequest);
       authenticationService.resetPassword(passwordResetRequest.getResetToken(), passwordResetRequest.getNewPassword());
       return ResponseEntity.ok().body("Password has been reset");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody @Valid ForgotPasswordRequest forgotPasswordRequest) {
        log.info("Received forgot password request: {}", forgotPasswordRequest);
        authenticationService.forgotPassword(forgotPasswordRequest.getEmail());
        return ResponseEntity.ok().body("Password has been reset");
    }

}