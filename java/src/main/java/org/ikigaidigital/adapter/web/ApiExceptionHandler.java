package org.ikigaidigital.adapter.web;

import lombok.extern.slf4j.Slf4j;
import org.ikigaidigital.domain.port.ConcurrentUpdateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ConcurrentUpdateException.class)
    ProblemDetail handleConcurrentUpdate(ConcurrentUpdateException exception) {
        log.warn("Concurrent time deposit update rejected", exception);

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Time deposits were updated concurrently; retry the request");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);

        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }
}
