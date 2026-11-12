package com.darmoz.whatsapp.common;

import com.darmoz.whatsapp.flow.FlowValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    ProblemDetail handleBadRequest(BadRequestException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UnknownClientException.class)
    ProblemDetail handleUnknownClient(UnknownClientException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ProblemDetail handleMissingHeader(MissingRequestHeaderException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Falta el header " + ex.getHeaderName());
    }

    @ExceptionHandler(ExternalApiException.class)
    ProblemDetail handleExternalApi(ExternalApiException ex) {
        log.warn("Fallo una API externa: {}", ex.getMessage(), ex.getCause());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        if (ex.getUpstreamStatus() != null) {
            problem.setProperty("upstreamStatus", ex.getUpstreamStatus());
        }
        return problem;
    }

    /** 422 con la lista de errores por nodo, para marcarlos en el editor. */
    @ExceptionHandler(FlowValidationException.class)
    ProblemDetail handleFlowValidation(FlowValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setProperty("errors", ex.getErrors());
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Valor invalido para el parametro " + ex.getName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos invalidos");
        ex.getBindingResult().getFieldErrors().forEach(error ->
                problem.setProperty(error.getField(), error.getDefaultMessage()));
        return problem;
    }
}
