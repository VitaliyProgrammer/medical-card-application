package com.vitaliy.medcard.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FileSignaturesTest {

    private static final byte[] PDF = "%PDF-1.7".getBytes();
    private static final byte[] PNG =
            {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

    @Test
    @DisplayName("matches: accepts real PDF, PNG and JPEG headers for their own type")
    void matches_realHeaders() {
        assertThat(FileSignatures.matches("application/pdf", PDF)).isTrue();
        assertThat(FileSignatures.matches("image/png", PNG)).isTrue();
        assertThat(FileSignatures.matches("image/jpeg", JPEG)).isTrue();
    }

    @Test
    @DisplayName("matches: rejects a header that belongs to a different type")
    void matches_mismatchedType() {
        assertThat(FileSignatures.matches("application/pdf", PNG)).isFalse();
        assertThat(FileSignatures.matches("image/png", JPEG)).isFalse();
    }

    @Test
    @DisplayName("matches: rejects unsupported types and headers that are too short")
    void matches_unsupportedOrTruncated() {
        assertThat(FileSignatures.matches("application/x-msdownload", PDF)).isFalse();
        assertThat(FileSignatures.matches("image/png", new byte[] {(byte) 0x89, 'P'})).isFalse();
    }
}
