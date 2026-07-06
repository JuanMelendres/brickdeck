package com.brickdeck.api.security.service;

import com.brickdeck.api.security.EmailAlreadyUsedException;
import com.brickdeck.api.security.dto.AuthResponse;
import com.brickdeck.api.security.dto.LoginRequest;
import com.brickdeck.api.security.dto.RegisterRequest;
import com.brickdeck.api.security.entity.User;
import com.brickdeck.api.security.jwt.JwtService;
import com.brickdeck.api.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User existing;

    @BeforeEach
    void setup() {
        existing = new User();
        existing.setId(UUID.randomUUID());
        existing.setEmail("user@brickdeck.test");
        existing.setPasswordHash("stored-hash");
        existing.setRole("USER");
    }

    @Test
    void registerHashesPasswordPersistsUserAndReturnsToken() {
        when(userRepository.existsByEmail("user@brickdeck.test")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(), eq("user@brickdeck.test"))).thenReturn("tok");

        AuthResponse response = authService.register(
                new RegisterRequest("User@Brickdeck.test", "secret123", "Juan"));

        assertThat(response.token()).isEqualTo("tok");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("user@brickdeck.test");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("user@brickdeck.test");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("secret123");
        assertThat(saved.getValue().getDisplayName()).isEqualTo("Juan");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("user@brickdeck.test")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("user@brickdeck.test", "secret123", null)))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginVerifiesPasswordAndReturnsToken() {
        when(userRepository.findByEmail("user@brickdeck.test")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("secret123", "stored-hash")).thenReturn(true);
        when(jwtService.generateToken(existing.getId(), "user@brickdeck.test")).thenReturn("tok");

        AuthResponse response = authService.login(
                new LoginRequest("User@Brickdeck.test", "secret123"));

        assertThat(response.token()).isEqualTo("tok");
        assertThat(response.user().id()).isEqualTo(existing.getId());
    }

    @Test
    void loginWithWrongPasswordThrowsBadCredentials() {
        when(userRepository.findByEmail("user@brickdeck.test")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("user@brickdeck.test", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginWithUnknownEmailThrowsBadCredentials() {
        when(userRepository.findByEmail("nobody@brickdeck.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("nobody@brickdeck.test", "secret123")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
