package com.chat_server.error.handler;

import com.chat_server.common.propertis.CustomProperties;
import com.chat_server.error.dto.ErrorResponse;
import com.chat_server.error.enumulation.ErrorCode;
import com.chat_server.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ErrorCode + application-custom.yml 메시지를 기반으로 예외를 응답한다.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final CustomProperties customProperties;

    /**
     * 서비스/도메인 계층에서 발생한 {@link BusinessException}을 공통 에러 응답으로 변환한다.
     *
     * <p>기능:
     * <ul>
     *   <li>예외 내부의 {@link ErrorCode}를 그대로 사용해 HTTP 상태 코드를 결정한다.</li>
     *   <li>예외 메시지가 비어 있지 않으면 해당 메시지를 우선 사용한다.</li>
     *   <li>예외 메시지가 비어 있으면 yml 메시지를 fallback으로 사용한다.</li>
     * </ul>
     *
     * @param exception 비즈니스 규칙 위반 시 서비스 계층에서 던진 예외
     * @return errorCode/message가 채워진 {@link ErrorResponse}와 해당 상태 코드의 {@link ResponseEntity}
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
        log.info("handleBusinessException 호출");
        log.debug("handleBusinessException params - exceptionMessage: {}, errorCode: {}",
                exception.getMessage(), exception.getErrorCode());
        ErrorCode errorCode = exception.getErrorCode();
        ErrorResponse response = createErrorResponse(errorCode, exception.getMessage());
        log.debug("handleBusinessException return - response: {}", response);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * 입력 파라미터가 유효하지 않을 때 발생한 {@link IllegalArgumentException}을 처리한다.
     *
     * <p>기능:
     * <ul>
     *   <li>에러 코드를 {@link ErrorCode#INVALID_INPUT}으로 고정한다.</li>
     *   <li>예외 메시지가 없으면 yml 기본 메시지를 사용한다.</li>
     * </ul>
     *
     * @param exception 잘못된 입력값/인자 검증 실패로 발생한 런타임 예외
     * @return INVALID_INPUT 코드 기반의 {@link ErrorResponse}
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.info("handleIllegalArgumentException 호출");
        log.debug("handleIllegalArgumentException params - exceptionMessage: {}", exception.getMessage());
        ErrorResponse response = createErrorResponse(ErrorCode.INVALID_INPUT, exception.getMessage());
        log.debug("handleIllegalArgumentException return - response: {}", response);
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus()).body(response);
    }

    /**
     * 명시적으로 분류되지 않은 {@link RuntimeException}을 규칙 기반으로 분류해 처리한다.
     *
     * <p>기능:
     * <ul>
     *   <li>예외 클래스명 패턴(Validation/NotFound/Conflict/Exists)을 기반으로 {@link ErrorCode}를 결정한다.</li>
     *   <li>분류 불가한 경우 {@link ErrorCode#NOT_DEFINE}를 사용한다.</li>
     * </ul>
     *
     * @param exception BusinessException, IllegalArgumentException 외 런타임 예외
     * @return 분류된 에러 코드에 대응하는 {@link ErrorResponse}
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException exception) {
        log.info("handleRuntimeException 호출");
        log.debug("handleRuntimeException params - exceptionClass: {}, exceptionMessage: {}",
                exception.getClass().getName(), exception.getMessage(), exception);
        ErrorCode errorCode = resolveRuntimeErrorCode(exception);
        ErrorResponse response = createErrorResponse(errorCode, exception.getMessage());
        log.debug("handleRuntimeException return - response: {}", response);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * 위 핸들러에서 처리하지 못한 모든 체크 예외/예외를 최종 처리한다.
     *
     * <p>기능:
     * <ul>
     *   <li>시스템 예외를 {@link ErrorCode#NOT_DEFINE}으로 응답해 API 응답 형식을 유지한다.</li>
     *   <li>내부 예외 상세는 로그(debug)로 기록하고, 응답은 공통 포맷으로 반환한다.</li>
     * </ul>
     *
     * @param exception 처리되지 않은 최종 예외
     * @return NOT_DEFINE 코드 기반의 {@link ErrorResponse}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        log.info("handleException 호출");
        log.debug("handleException params - exceptionClass: {}, exceptionMessage: {}",
                exception.getClass().getName(), exception.getMessage(), exception);
        ErrorResponse response = createErrorResponse(ErrorCode.NOT_DEFINE, exception.getMessage());
        log.debug("handleException return - response: {}", response);
        return ResponseEntity.status(ErrorCode.NOT_DEFINE.getStatus()).body(response);
    }

    /**
     * 공통 에러 응답 DTO를 생성한다.
     *
     * <p>기능:
     * <ul>
     *   <li>전달받은 errorCode를 기준으로 yml에서 기본 메시지(fallback)를 조회한다.</li>
     *   <li>예외 메시지가 존재하면 해당 메시지를 사용하고, 비어 있으면 fallback 메시지를 사용한다.</li>
     * </ul>
     *
     * @param errorCode 응답 상태/의미를 나타내는 에러 코드
     * @param exceptionMessage 실제 예외에서 전달된 메시지(없을 수 있음)
     * @return errorCode와 최종 message가 채워진 {@link ErrorResponse}
     */
    private ErrorResponse createErrorResponse(ErrorCode errorCode, String exceptionMessage) {
        log.info("createErrorResponse 호출");
        log.debug("createErrorResponse params - errorCode: {}, exceptionMessage: {}", errorCode, exceptionMessage);
        String fallbackMessage = customProperties.getError().getMessage(errorCode);
        String message = (exceptionMessage == null || exceptionMessage.isBlank()) ? fallbackMessage : exceptionMessage;
        ErrorResponse response = new ErrorResponse(errorCode, message);
        log.debug("createErrorResponse return - response: {}", response);
        return response;
    }

    /**
     * 런타임 예외 클래스명을 기반으로 fallback {@link ErrorCode}를 계산한다.
     *
     * <p>기능:
     * <ul>
     *   <li>Validation 포함: {@link ErrorCode#VALIDATION_ERROR}</li>
     *   <li>NotFound 포함: {@link ErrorCode#NOT_FOUND}</li>
     *   <li>Conflict/Exists 포함: {@link ErrorCode#CONFLICT}</li>
     *   <li>그 외: {@link ErrorCode#NOT_DEFINE}</li>
     * </ul>
     *
     * <p>예외 상황:
     * <ul>
     *   <li>예외 객체 자체가 null인 경우 NPE가 발생할 수 있으므로, 호출부에서 null이 아닌 예외만 전달해야 한다.</li>
     * </ul>
     *
     * @param exception 분류 대상 런타임 예외
     * @return 분류 규칙에 따라 결정된 에러 코드
     */
    private ErrorCode resolveRuntimeErrorCode(RuntimeException exception) {
        log.info("resolveRuntimeErrorCode 호출");
        log.debug("resolveRuntimeErrorCode params - exceptionClass: {}", exception.getClass().getName());
        String simpleName = exception.getClass().getSimpleName();
        ErrorCode resolved;
        if (simpleName.contains("Validation")) {
            resolved = ErrorCode.VALIDATION_ERROR;
        } else if (simpleName.contains("NotFound")) {
            resolved = ErrorCode.NOT_FOUND;
        } else if (simpleName.contains("Conflict") || simpleName.contains("Exists")) {
            resolved = ErrorCode.CONFLICT;
        } else {
            resolved = ErrorCode.NOT_DEFINE;
        }
        log.debug("resolveRuntimeErrorCode return - resolved: {}", resolved);
        return resolved;
    }
}
