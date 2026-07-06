package ru.formatkoda.polybank;


import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import ru.formatkoda.polybank.exception.ResourceNotFoundException;
import ru.formatkoda.polybank.exception.BusinessLogicException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(
            value = {
                    MethodArgumentNotValidException.class,
                    HandlerMethodValidationException.class
            }
    )
    ProblemDetail handleBeanValidationException(BindException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setProperty("code", "VALIDATION_ERROR");

        problem.setProperty(
                "errors",
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(
                                e -> {
                                    Map<String, Object> errors = new LinkedHashMap<>();
                                    errors.put("field", e.getField());
                                    errors.put("message", e.getDefaultMessage());
                                    errors.put("code", e.getCode());

                                    return errors;
                                }
                        )
                        .toList()
        );

        return problem;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
        return buildProblem(
                ex,
                HttpStatus.NOT_FOUND,
                "Resource doesn't exist",
                Map.of(
                        "code", "RESOURCE_NOT_FOUND"
                )
        );
    }

    @ExceptionHandler(BusinessLogicException.class)
    ProblemDetail handleBusinessLogicException(BusinessLogicException ex) {
        return buildProblem(
                ex,
                HttpStatus.CONFLICT,
                "Business logic failed",
                Map.of(
                        "code", "BUSINESS_RULE_VIOLATION"
                )
        );
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleGeneralException(Exception ex) {
        return buildProblem(
                ex,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Server logic failed",
                Map.of(
                        "code", "INTERNAL_SERVER_ERROR"
                )
        );
    }

    private ProblemDetail buildProblem(Exception ex, HttpStatus status, String title, Map<String, String> properties) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        properties.keySet().forEach(key -> problem.setProperty(key, properties.get(key)));
        problem.setProperty("message", ex.getMessage());

        return problem;
    }
}
