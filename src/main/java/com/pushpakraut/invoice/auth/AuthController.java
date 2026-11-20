package com.pushpakraut.invoice.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record Credentials(@Email @NotBlank String email, @NotBlank @Size(min = 8, max = 72) String password) {}
    public record TokenResponse(String token) {}

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(AppUserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody Credentials c) {
        String email = c.email().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        users.save(new AppUser(email, encoder.encode(c.password())));
        return new TokenResponse(jwt.issue(email));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody Credentials c) {
        String email = c.email().toLowerCase();
        AppUser user = users.findByEmail(email)
                .filter(u -> encoder.matches(c.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new TokenResponse(jwt.issue(user.getEmail()));
    }
}
