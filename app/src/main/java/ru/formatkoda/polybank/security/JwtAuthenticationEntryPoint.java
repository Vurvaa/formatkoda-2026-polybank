package ru.formatkoda.polybank.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         @NonNull AuthenticationException authException)
            throws IOException {
        Exception exception = (Exception) request.getAttribute("exception");

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(APPLICATION_JSON_VALUE);

        String cause = exception != null ?
                exception.getMessage() : authException.getCause().toString();

        OutputStream out = response.getOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(out, buildProblem(cause, request.getRequestURI()));
        out.flush();
    }

    private ProblemDetail buildProblem(String message, String instant) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);

        problem.setType((URI.create("polybank:authentication-error")));
        problem.setTitle("The user failed authentication");
        problem.setDetail("Authentication process failed");
        problem.setProperty("code", "AUTHENTICATION_ERROR");
        problem.setInstance(URI.create(instant));
        problem.setProperty("message", message);

        return problem;
    }

}
