package ai.factory.agent_fitness.chat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Error> notFound(NoResourceFoundException ignored) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error("NOT_FOUND"));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<Error> invalid(Exception ignored) {
        return ResponseEntity.badRequest().body(new Error("INVALID_REQUEST"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Error> unavailable(Exception ignored) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new Error("SERVICE_UNAVAILABLE"));
    }
}
