package com.company.wiki.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    SPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "Space를 찾을 수 없습니다."),
    CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "콘텐츠를 찾을 수 없습니다."),
    MAIL_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "메일 계정을 찾을 수 없습니다."),
    MAIL_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메일 메시지를 찾을 수 없습니다."),
    ALREADY_CONVERTED(HttpStatus.CONFLICT, "이미 페이지로 변환된 메일입니다."),

    DUPLICATE_SPACE_KEY(HttpStatus.CONFLICT, "이미 사용 중인 Space Key입니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 로그인 ID입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    ARCHIVED_SPACE(HttpStatus.BAD_REQUEST, "보관된 Space는 수정할 수 없습니다."),
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다."),

    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "해당 콘텐츠에 대한 권한이 없습니다."),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
