package com.vitaliy.medcard.security;

import com.vitaliy.medcard.exception.UserNotFoundException;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found with email: " + email + "!"));
    }
}
