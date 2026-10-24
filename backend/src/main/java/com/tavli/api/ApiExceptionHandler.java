package com.tavli.api;

import com.tavli.engine.IllegalTurnException;
import com.tavli.game.GameFinishedException;
import com.tavli.game.GameNotFoundException;
import com.tavli.game.StaleTurnException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps domain errors to RFC 9457 problem details. The base class does the same for Spring's own errors
 * (malformed JSON, a bad UUID in the path, and so on), which become 400s.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(GameNotFoundException.class)
    ProblemDetail notFound(GameNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Game not found", e);
    }

    @ExceptionHandler(StaleTurnException.class)
    ProblemDetail staleTurn(StaleTurnException e) {
        return problem(HttpStatus.CONFLICT, "Stale turn", e);
    }

    @ExceptionHandler(GameFinishedException.class)
    ProblemDetail finished(GameFinishedException e) {
        return problem(HttpStatus.CONFLICT, "Game is over", e);
    }

    @ExceptionHandler(IllegalTurnException.class)
    ProblemDetail illegalTurn(IllegalTurnException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Illegal turn", e);
    }

    private static ProblemDetail problem(HttpStatus status, String title, Exception e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setTitle(title);
        return problem;
    }
}
