package org.example.currency.common;

import lombok.extern.slf4j.Slf4j;
import org.example.currency.exchange.ExchangeAmountTooSmallException;
import org.example.currency.exchange.UnsupportedExchangeException;
import org.example.currency.nbp.ExchangeRateUnavailableException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail handleAccountNotFound(AccountNotFoundException exception) {
        return problemDetail(HttpStatus.NOT_FOUND, "Account not found", exception.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    ProblemDetail handleInsufficientFunds(InsufficientFundsException exception) {
        ProblemDetail problemDetail = problemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds", exception.getMessage());
        problemDetail.setProperty("accountId", exception.getAccountId());
        problemDetail.setProperty("currency", exception.getCurrency());
        problemDetail.setProperty("availableBalance", exception.getAvailableBalance());
        problemDetail.setProperty("requestedAmount", exception.getRequestedAmount());
        return problemDetail;
    }

    @ExceptionHandler(ExchangeAmountTooSmallException.class)
    ProblemDetail handleAmountTooSmall(ExchangeAmountTooSmallException exception) {
        ProblemDetail problemDetail = problemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Exchange amount too small", exception.getMessage());
        problemDetail.setProperty("fromCurrency", exception.getFromCurrency());
        problemDetail.setProperty("toCurrency", exception.getToCurrency());
        problemDetail.setProperty("amount", exception.getAmount());
        return problemDetail;
    }

    @ExceptionHandler(UnsupportedExchangeException.class)
    ProblemDetail handleUnsupportedExchange(UnsupportedExchangeException exception) {
        return problemDetail(HttpStatus.BAD_REQUEST, "Unsupported exchange pair", exception.getMessage());
    }

    @ExceptionHandler(ExchangeRateUnavailableException.class)
    ProblemDetail handleRateUnavailable(ExchangeRateUnavailableException exception) {
        log.warn("NBP rate unavailable", exception);
        return problemDetail(HttpStatus.SERVICE_UNAVAILABLE, "Exchange rate unavailable", exception.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentModification(ObjectOptimisticLockingFailureException exception) {
        return problemDetail(HttpStatus.CONFLICT, "Concurrent modification", "Account was modified concurrently, please retry");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handlePathTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String detail = "Parameter '%s' has invalid value: '%s'".formatted(
                exception.getName(), exception.getValue());
        return problemDetail(HttpStatus.BAD_REQUEST, "Invalid request parameter", detail);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> Map.of(
                        "field", fieldError.getField(),
                        "message", fieldError.getDefaultMessage() == null ? "invalid" : fieldError.getDefaultMessage()))
                .toList();
        ProblemDetail problemDetail = problemDetail(HttpStatus.BAD_REQUEST, "Validation failed", "Request body has invalid fields");
        problemDetail.setProperty("errors", fieldErrors);
        return new ResponseEntity<>(problemDetail, headers, status);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error handling request", exception);
        return problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "Unexpected error, please try again later");
    }

    private static ProblemDetail problemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        return problemDetail;
    }
}
