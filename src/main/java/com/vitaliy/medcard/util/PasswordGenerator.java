package com.vitaliy.medcard.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PasswordGenerator {

    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARACTERS = "!@#$%^&*()-_=+";
    private static final String ALL_CHARACTERS =
            LOWERCASE + UPPERCASE + DIGITS + SPECIAL_CHARACTERS;
    private static final int PASSWORD_LENGTH = 14;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {
    }

    public static String generate() {
        List<Character> characters = new ArrayList<>(PASSWORD_LENGTH);
        characters.add(randomCharFrom(LOWERCASE));
        characters.add(randomCharFrom(UPPERCASE));
        characters.add(randomCharFrom(DIGITS));
        characters.add(randomCharFrom(SPECIAL_CHARACTERS));

        for (int i = characters.size(); i < PASSWORD_LENGTH; i++) {
            characters.add(randomCharFrom(ALL_CHARACTERS));
        }
        Collections.shuffle(characters, RANDOM);

        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        characters.forEach(password::append);
        return password.toString();
    }

    private static char randomCharFrom(String source) {
        return source.charAt(RANDOM.nextInt(source.length()));
    }
}
