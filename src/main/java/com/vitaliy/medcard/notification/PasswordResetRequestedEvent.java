package com.vitaliy.medcard.notification;

public record PasswordResetRequestedEvent(String email, String rawToken, long expiresInMinutes) {
}
