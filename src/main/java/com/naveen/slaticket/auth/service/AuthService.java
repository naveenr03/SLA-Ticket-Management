package com.naveen.slaticket.auth.service;

import com.naveen.slaticket.auth.dto.AuthResponse;
import com.naveen.slaticket.auth.dto.LoginRequest;
import com.naveen.slaticket.auth.dto.RegisterRequest;
import com.naveen.slaticket.auth.security.JwtService;
import com.naveen.slaticket.common.exception.BadRequestException;
import com.naveen.slaticket.user.entity.Role;
import com.naveen.slaticket.user.entity.User;
import com.naveen.slaticket.user.repository.UserRepository;
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
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email().toLowerCase().trim())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(request.email().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.ROLE_USER);

        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, "Bearer");
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        String token = jwtService.generateToken(request.email());
        return new AuthResponse(token, "Bearer");
    }
}