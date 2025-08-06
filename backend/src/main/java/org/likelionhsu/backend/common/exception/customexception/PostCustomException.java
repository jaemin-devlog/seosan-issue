package org.likelionhsu.backend.common.exception.customexception;

import org.likelionhsu.backend.common.exception.CustomException;
import org.likelionhsu.backend.common.exception.ErrorCode;

public class PostCustomException extends CustomException {

    public PostCustomException(ErrorCode errorCode) {
        super(errorCode, errorCode.getMessage());
    }
}