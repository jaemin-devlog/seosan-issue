package org.likelionhsu.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

 @Getter
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력 값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 오류가 발생했습니다."),
    EXTERNAL_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "외부 API 호출에 실패했습니다."),

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),

    // Naver API
    NAVER_API_BAD_REQUEST(HttpStatus.BAD_REQUEST, "네이버 API 요청이 잘못되었습니다. 파라미터를 확인해주세요."),
    NAVER_API_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "네이버 API 인증에 실패했습니다. 클라이언트 ID/Secret을 확인해주세요."),
    NAVER_API_NOT_FOUND(HttpStatus.NOT_FOUND, "네이버 API를 찾을 수 없습니다. URL을 확인해주세요."),
    NAVER_API_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "네이버 API 서버에 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    NAVER_API_UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "네이버 API 호출 중 알 수 없는 오류가 발생했습니다.");


    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}