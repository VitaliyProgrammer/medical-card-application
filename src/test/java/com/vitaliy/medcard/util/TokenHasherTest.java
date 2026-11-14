package com.vitaliy.medcard.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenHasherTest {

    @Test
    @DisplayName("sha256Hex: matches the known SHA-256 digest of a fixed input")
    void sha256Hex_knownVector() {
        assertThat(TokenHasher.sha256Hex("abc")).isEqualTo(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    @DisplayName("sha256Hex: is deterministic and always 64 hex characters long")
    void sha256Hex_deterministicAndFixedLength() {
        String hash = TokenHasher.sha256Hex("some-refresh-token");

        assertThat(hash).isEqualTo(TokenHasher.sha256Hex("some-refresh-token"));
        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }
}
