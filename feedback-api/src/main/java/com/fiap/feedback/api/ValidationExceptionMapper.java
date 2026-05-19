package com.fiap.feedback.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

/**
 * Maps both bean-validation failures and the domain's
 * {@link IllegalArgumentException} (thrown by {@code Evaluation.newSubmission}
 * for nota out of range or blank descricao) to a structured 400 response.
 */
@Provider
public class ValidationExceptionMapper {

    @Provider
    public static class ConstraintMapper implements ExceptionMapper<ConstraintViolationException> {
        @Override
        public Response toResponse(ConstraintViolationException e) {
            List<ErrorBody.FieldError> errors = e.getConstraintViolations().stream()
                    .map(ConstraintMapper::toFieldError)
                    .toList();
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorBody("Invalid request payload", errors))
                    .build();
        }

        private static ErrorBody.FieldError toFieldError(ConstraintViolation<?> v) {
            String path = v.getPropertyPath().toString();
            int lastDot = path.lastIndexOf('.');
            String field = lastDot >= 0 ? path.substring(lastDot + 1) : path;
            return new ErrorBody.FieldError(field, v.getMessage());
        }
    }

    @Provider
    public static class IllegalArgumentMapper implements ExceptionMapper<IllegalArgumentException> {
        @Override
        public Response toResponse(IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorBody(e.getMessage(), List.of()))
                    .build();
        }
    }

    public record ErrorBody(String message, List<FieldError> errors) {
        public record FieldError(String field, String message) {
        }
    }
}
