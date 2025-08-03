package org.likelionhsu.backend.common.exception.customexception;

import org.likelionhsu.backend.common.exception.CustomException;
import org.likelionhsu.backend.common.exception.ErrorCode;

public class PostCustomException extends CustomException {

    public PostCustomException(ErrorCode errorCode) {
        super(errorCode, "기상청 API 응답 형식 이 올바르지 않습니다. (JSON 아님)");
    }
}