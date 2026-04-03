package com.dgu.LookIT.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    WRONG_ENTRY_POINT(40000, HttpStatus.BAD_REQUEST, "잘못된 접근입니다"),
    MISSING_REQUEST_PARAMETER(40001, HttpStatus.BAD_REQUEST, "필수 요청 파라미터가 누락되었습니다."),
    INVALID_PARAMETER_FORMAT(40002, HttpStatus.BAD_REQUEST, "요청에 유효하지 않은 인자 형식입니다."),
    BAD_REQUEST_JSON(40003, HttpStatus.BAD_REQUEST, "잘못된 JSON 형식입니다."),
    INVALID_FACE_MOOD(40004, HttpStatus.BAD_REQUEST, "사용자의 얼굴분위기 값이 잘못되었습니다."),
    INVALID_FACE_SHAPE(40005, HttpStatus.BAD_REQUEST, "AI 서버가 반환한 얼굴형 값이 잘못되었습니다."),
    INVALID_BODY_TYPE(40006, HttpStatus.BAD_REQUEST, "바디형 값이 잘못되었습니다."),
    INVALID_BODY_ANALYSIS(40007, HttpStatus.BAD_REQUEST, "AI 서버가 반환한 바디형 분석 값이 잘못되었습니다."),

    //401
    INVALID_HEADER_VALUE(40100, HttpStatus.UNAUTHORIZED, "올바르지 않은 헤더값입니다."),
    TOKEN_EXPIRED_ERROR(40101, HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    TOKEN_INVALID_ERROR(40102, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_MALFORMED_ERROR(40103, HttpStatus.UNAUTHORIZED, "잘못된 형식의 토큰입니다."),
    TOKEN_TYPE_ERROR(40104, HttpStatus.UNAUTHORIZED, "토큰 타입이 일치하지 않거나 비어있습니다."),
    TOKEN_UNSUPPORTED_ERROR(40105, HttpStatus.UNAUTHORIZED, "지원하지 않는 토큰입니다."),
    TOKEN_GENERATION_ERROR(40106, HttpStatus.UNAUTHORIZED, "토큰 생성에 실패하였습니다."),
    TOKEN_UNKNOWN_ERROR(40107, HttpStatus.UNAUTHORIZED, "알 수 없는 토큰입니다."),
    LOGIN_FAILURE(40108, HttpStatus.UNAUTHORIZED, "로그인에 실패했습니다"),
    UNAUTHORIZED(40109, HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다."),

    //403
    FORBIDDEN_ROLE(40300, HttpStatus.FORBIDDEN, "권한이 존재하지 않습니다."),

    //404
    NOT_FOUND_USER(40400, HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    NOT_FOUND_IMAGE(40401, HttpStatus.NOT_FOUND, "존재하지 않는 이미지입니다."),
    NOT_FOUND_BODY_ANALYSIS(40402, HttpStatus.NOT_FOUND, "사용자의 머신러닝 바디 분석이 존재하지않습니다."),
    NOT_FOUND_BODY_TYPE(40403, HttpStatus.NOT_FOUND, "사용자의 바디타입이 존재하지않습니다."),
    NOT_FOUND_FACE_ANALYSIS(40404, HttpStatus.NOT_FOUND, "사용자의 얼굴타입이 존재하지않습니다."),
    NOT_FOUND_STYLE_RECOMMENDATION(40405, HttpStatus.NOT_FOUND, "스타일팁이 존재하지않습니다."),
    NOT_FOUND_VIRTUAL_FITTING(40406, HttpStatus.NOT_FOUND, "가상피팅 데이터가 존재하지않습니다."),

    //500
    INTERNAL_SERVER_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다"),
    AI_SERVER_ERROR(50200, HttpStatus.BAD_GATEWAY, "AI 서버 통신 중 오류가 발생했습니다.");
    ;

    private final Integer code;
    private final HttpStatus httpStatus;
    private final String message;
}
