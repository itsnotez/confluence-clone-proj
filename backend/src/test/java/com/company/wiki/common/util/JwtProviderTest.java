package com.company.wiki.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(
                "thisIsAVeryLongSecretKeyForJwtTokenGenerationAtLeast256BitsLong!",
                3600000L,
                604800000L
        );
    }

    @Test
    void generateAndValidateAccessToken() {
        String token = jwtProvider.generateAccessToken(1L, "MEMBER");
        assertThat(jwtProvider.isValid(token)).isTrue();
        assertThat(jwtProvider.getUserId(token)).isEqualTo(1L);
    }

    @Test
    void invalidToken_returnsFalse() {
        assertThat(jwtProvider.isValid("invalid.token.value")).isFalse();
    }

    @Test
    void generateRefreshToken_isValidAndHasCorrectSubject() {
        String token = jwtProvider.generateRefreshToken(42L);
        assertThat(jwtProvider.isValid(token)).isTrue();
        assertThat(jwtProvider.getUserId(token)).isEqualTo(42L);
    }
}
