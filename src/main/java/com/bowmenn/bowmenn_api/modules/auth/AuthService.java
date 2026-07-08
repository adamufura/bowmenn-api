package com.bowmenn.bowmenn_api.modules.auth;

import com.bowmenn.bowmenn_api.common.exception.BadRequestException;
import com.bowmenn.bowmenn_api.common.exception.ResourceNotFoundException;
import com.bowmenn.bowmenn_api.common.exception.UnauthorizedException;
import com.bowmenn.bowmenn_api.common.security.JwtService;
import com.bowmenn.bowmenn_api.modules.auth.dto.AuthResponse;
import com.bowmenn.bowmenn_api.modules.auth.dto.LoginRequest;
import com.bowmenn.bowmenn_api.modules.auth.dto.RegisterRequest;
import com.bowmenn.bowmenn_api.modules.user.User;
import com.bowmenn.bowmenn_api.modules.user.UserRepository;
import com.bowmenn.bowmenn_api.modules.user.UserRole;
import com.bowmenn.bowmenn_api.modules.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        if (request.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("Cannot self-register as admin");
        }

        User user = User.builder()
            .fullName(request.getFullName())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .phone(request.getPhone())
            .role(request.getRole())
            .isActive(true)
            .build();

        user = userRepository.save(user);
        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
            .token(token)
            .user(UserResponse.from(user))
            .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
            .token(token)
            .user(UserResponse.from(user))
            .build();
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.from(user);
    }
}
