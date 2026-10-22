package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.exception;

import ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.ProfessionalSearchController;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ProfessionalSearchController.class)
public class ProfessionalSearchValidationExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid professional search request."
        );
    }
}
