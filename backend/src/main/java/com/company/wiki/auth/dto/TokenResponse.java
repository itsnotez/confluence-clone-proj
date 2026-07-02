package com.company.wiki.auth.dto;

import com.company.wiki.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private UserSummary user;

    @Getter
    @Builder
    public static class UserSummary {
        private Long id;
        private String name;
        private String email;
        private String role;
    }

    public static TokenResponse of(String accessToken, String refreshToken, User user) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserSummary.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .build();
    }
}
