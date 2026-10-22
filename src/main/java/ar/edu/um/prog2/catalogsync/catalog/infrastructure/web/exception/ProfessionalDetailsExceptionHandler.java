package ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.exception;

import ar.edu.um.prog2.catalogsync.catalog.application.exception.ProfessionalNotFoundException;
import ar.edu.um.prog2.catalogsync.catalog.infrastructure.web.ProfessionalDetailsController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ProfessionalDetailsController.class)
public class ProfessionalDetailsExceptionHandler {

    @ExceptionHandler(ProfessionalNotFoundException.class)
    ProblemDetail handleProfessionalNotFound() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Professional not found."
        );
    }
}
