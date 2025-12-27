package  com.sujon.spring_data_analysis_api.exception;

/**
 * Thrown when a client sends an invalid request.
 * Mapped to HTTP 400 by GlobalExceptionHandler.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
