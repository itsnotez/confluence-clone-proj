package com.company.wiki.user.dto;

import com.company.wiki.user.entity.Group;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class GroupDto {

    public record CreateRequest(
            @NotBlank String name,
            String description
    ) {}

    public record Response(
            Long id,
            String name,
            String description,
            LocalDateTime createdAt,
            int memberCount
    ) {
        public static Response from(Group group) {
            return new Response(
                    group.getId(),
                    group.getName(),
                    group.getDescription(),
                    group.getCreatedAt(),
                    group.getMembers().size()
            );
        }
    }
}
