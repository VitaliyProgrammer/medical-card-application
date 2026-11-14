package com.vitaliy.medcard.util;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public final class FileSignatures {

    public static final int HEADER_LENGTH = 8;

    private static final Map<String, byte[]> SIGNATURES = Map.of(
            "application/pdf", new byte[] {'%', 'P', 'D', 'F', '-'},
            "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A},
            "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

    private FileSignatures() {
    }

    public static Set<String> supportedContentTypes() {
        return SIGNATURES.keySet();
    }

    public static boolean matches(String contentType, byte[] header) {
        byte[] signature = SIGNATURES.get(contentType);
        if (signature == null || header.length < signature.length) {
            return false;
        }
        return Arrays.equals(header, 0, signature.length, signature, 0, signature.length);
    }
}
