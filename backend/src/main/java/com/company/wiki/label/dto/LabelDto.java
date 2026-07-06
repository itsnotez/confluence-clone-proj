package com.company.wiki.label.dto;

import lombok.*;

public class LabelDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String name;
        private String color;
        private Long spaceId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AddRequest {
        private Long labelId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        private Long spaceId;
        private String name;
        private String color;
    }
}
