package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.exception;

import ar.edu.um.prog2.catalogsync.catalog.application.exception.ProfessionalNotFoundException;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.controller.ProfessionalDetailsController;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.controller.InternalProfessionalDetailsController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = {ProfessionalDetailsController.class, InternalProfessionalDetailsController.class})
public class ProfessionalDetailsExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleInvalidProfessionalId() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Professional ID must be a valid 64-bit integer."
        );
    }

    @ExceptionHandler(ProfessionalNotFoundException.class)
    ProblemDetail handleProfessionalNotFound() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Professional not found."
        );
    }
}
