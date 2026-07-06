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
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Resource doesn't exist");
        problem.setProperty("code", "RESOURCE_NOT_FOUND");
        problem.setProperty("message", ex.getMessage());

        return problem;
    }

    @ExceptionHandler(BusinessLogicException.class)
    ProblemDetail handleBusinessLogicException(BusinessLogicException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Business logic failed");
        problem.setProperty("code", "BUSINESS_RULE_VIOLATION");
        problem.setProperty("message", ex.getMessage());

        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleGeneralException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Server logic failed");
        problem.setProperty("code", "INTERNAL_SERVER_ERROR");
        problem.setProperty("message", ex.getMessage());

        return problem;
    }
}
