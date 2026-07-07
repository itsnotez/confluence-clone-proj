package com.company.wiki.user.dto;

import com.company.wiki.user.entity.User;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class UserDto {

    public record CreateRequest(
            @NotBlank String loginId,
            @NotBlank String name,
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8) String password,
            String role
    ) {
        public CreateRequest {
            if (role == null) role = "MEMBER";
        }
    }

    public record UpdateRequest(
            @Nullable String name,
            @Nullable @Email String email,
            @Nullable String role,
            @Nullable @Size(min = 8) String password
    ) {}

    public record Response(
            Long id,
            String loginId,
            String name,
            String email,
            String role,
            String status,
            LocalDateTime createdAt
    ) {
        public static Response from(User user) {
            return new Response(
                    user.getId(),
                    user.getLoginId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getStatus(),
                    user.getCreatedAt()
            );
        }
    }
}
