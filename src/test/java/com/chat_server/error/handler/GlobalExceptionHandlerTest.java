package com.chat_server.error.handler;

import com.chat_server.common.propertis.CustomProperties;
import com.chat_server.error.dto.ErrorResponse;
import com.chat_server.error.enumulation.ErrorCode;
import com.chat_server.error.exception.BusinessException;
import com.chat_server.file.exception.FileValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        CustomProperties properties = new CustomProperties();
        CustomProperties.Error error = new CustomProperties.Error();
        error.setMessages(Map.of(
                "NOT_DEFINE", "정의되지 않은 오류입니다.",
                "INVALID_INPUT", "입력값이 올바르지 않습니다.",
                "VALIDATION_ERROR", "검증에 실패했습니다.",
                "NOT_FOUND", "대상을 찾을 수 없습니다.",
                "CONFLICT", "충돌이 발생했습니다."
        ));
        properties.setError(error);
        handler = new GlobalExceptionHandler(properties);
    }

    /**
     * BusinessException의 에러 코드와 상세 메시지가 그대로 응답에 반영되는지 검증한다.
     */
    @Test
    @DisplayName("비즈니스 예외 처리 성공 - 예외의 상태 코드와 상세 메시지를 응답한다")
    void handleBusinessExceptionShouldUseExceptionCodeAndMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(
                new BusinessException(ErrorCode.NOT_FOUND, "채팅방이 없습니다.")
        );

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.NOT_FOUND.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("채팅방이 없습니다.");
    }

    /**
     * 예외 메시지가 공백인 경우 application 설정의 기본 메시지로 대체되는지 검증한다.
     */
    @Test
    @DisplayName("비즈니스 예외 메시지 경계값 - 공백 메시지는 설정 기본 메시지로 대체한다")
    void handleBusinessExceptionShouldUseFallbackWhenMessageIsBlank() {
        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(
                new BusinessException(ErrorCode.CONFLICT, "   ")
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("충돌이 발생했습니다.");
    }

    /**
     * IllegalArgumentException을 INVALID_INPUT 응답으로 변환하는지 검증한다.
     */
    @Test
    @DisplayName("잘못된 인자 처리 - INVALID_INPUT 상태와 예외 메시지를 반환한다")
    void handleIllegalArgumentExceptionShouldReturnInvalidInput() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("roomId는 필수입니다.")
        );

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INVALID_INPUT.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(response.getBody().message()).isEqualTo("roomId는 필수입니다.");
    }

    /**
     * 런타임 예외 클래스명에 Validation이 포함된 경우 VALIDATION_ERROR로 분류하는지 검증한다.
     */
    @Test
    @DisplayName("런타임 예외 분류 - Validation 이름의 예외는 VALIDATION_ERROR로 응답한다")
    void handleRuntimeExceptionShouldResolveValidationError() {
        ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(
                new SampleValidationException("검증 실패")
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    /**
     * 런타임 예외 클래스명에 NotFound가 포함된 경우 NOT_FOUND로 분류하는지 검증한다.
     */
    @Test
    @DisplayName("런타임 예외 분류 - NotFound 이름의 예외는 NOT_FOUND로 응답한다")
    void handleRuntimeExceptionShouldResolveNotFound() {
        ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(
                new SampleNotFoundException("대상 없음")
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    /**
     * Conflict와 Exists 이름의 예외가 모두 CONFLICT로 분류되는지 검증한다.
     */
    @Test
    @DisplayName("런타임 예외 분류 - Conflict와 Exists 이름의 예외는 CONFLICT로 응답한다")
    void handleRuntimeExceptionShouldResolveConflictPatterns() {
        ResponseEntity<ErrorResponse> conflict = handler.handleRuntimeException(
                new SampleConflictException("충돌")
        );
        ResponseEntity<ErrorResponse> exists = handler.handleRuntimeException(
                new SampleExistsException("이미 존재")
        );

        assertThat(conflict.getBody()).isNotNull();
        assertThat(conflict.getBody().errorCode()).isEqualTo(ErrorCode.CONFLICT);
        assertThat(exists.getBody()).isNotNull();
        assertThat(exists.getBody().errorCode()).isEqualTo(ErrorCode.CONFLICT);
    }

    /**
     * 분류 규칙에 해당하지 않는 RuntimeException이 NOT_DEFINE으로 처리되는지 검증한다.
     */
    @Test
    @DisplayName("런타임 예외 분류 한계 - 알 수 없는 예외는 NOT_DEFINE으로 응답한다")
    void handleRuntimeExceptionShouldUseNotDefineForUnknownType() {
        ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(
                new RuntimeException("시스템 오류")
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(ErrorCode.NOT_DEFINE);
    }

    /**
     * 일반 체크 예외를 최종 NOT_DEFINE 응답으로 변환하는지 검증한다.
     */
    @Test
    @DisplayName("일반 예외 처리 - 분류되지 않은 Exception은 NOT_DEFINE으로 응답한다")
    void handleExceptionShouldReturnNotDefine() {
        ResponseEntity<ErrorResponse> response = handler.handleException(new Exception("checked error"));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.NOT_DEFINE.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("checked error");
    }

    /** 파일 최대 크기 초과 예외의 전용 코드와 메시지를 검증한다. */
    @Test
    @DisplayName("파일 업로드 한계값 - 최대 크기 초과 시 FILE_SIZE_EXCEEDED를 반환한다")
    void handleMaxUploadSizeExceededShouldReturnFileSizeExceeded() {
        ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSizeExceeded(
                new MaxUploadSizeExceededException(1024L)
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED);
        assertThat(response.getBody().message()).contains("파일 크기");
    }

    /** 잘못된 multipart 요청을 INVALID_FILE_UPLOAD 응답으로 변환하는지 검증한다. */
    @Test
    @DisplayName("Multipart 요청 실패 - 올바르지 않은 업로드 요청은 INVALID_FILE_UPLOAD로 응답한다")
    void handleMultipartExceptionShouldReturnInvalidFileUpload() {
        ResponseEntity<ErrorResponse> response = handler.handleMultipartException(
                new MultipartException("invalid multipart")
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(ErrorCode.INVALID_FILE_UPLOAD);
    }

    /** FileValidationException의 코드와 메시지가 손실되지 않는지 검증한다. */
    @Test
    @DisplayName("파일 검증 실패 - FileValidationException의 코드와 메시지를 그대로 반환한다")
    void handleFileValidationExceptionShouldPreserveErrorInformation() {
        ResponseEntity<ErrorResponse> response = handler.handleFileValidationException(
                new FileValidationException(ErrorCode.INVALID_FILE_UPLOAD, "허용되지 않은 파일입니다.")
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(ErrorCode.INVALID_FILE_UPLOAD);
        assertThat(response.getBody().message()).isEqualTo("허용되지 않은 파일입니다.");
    }

    private static class SampleValidationException extends RuntimeException {
        private SampleValidationException(String message) { super(message); }
    }

    private static class SampleNotFoundException extends RuntimeException {
        private SampleNotFoundException(String message) { super(message); }
    }

    private static class SampleConflictException extends RuntimeException {
        private SampleConflictException(String message) { super(message); }
    }

    private static class SampleExistsException extends RuntimeException {
        private SampleExistsException(String message) { super(message); }
    }
}
