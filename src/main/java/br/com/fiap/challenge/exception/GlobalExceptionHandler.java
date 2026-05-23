package br.com.fiap.challenge.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tratamento centralizado de exceções da API.
 *
 * @RestControllerAdvice intercepta exceções lançadas em qualquer controller
 * e as converte em respostas HTTP padronizadas (ErrorResponse), evitando
 * que stack traces ou mensagens internas vazem para o cliente.
 *
 * Hierarquia de handlers (da mais específica para a mais genérica):
 * 1. ResourceNotFoundException → 404 Not Found
 * 2. BusinessException         → 400 Bad Request (regras de negócio violadas)
 * 3. MethodArgumentNotValidException → 400 com detalhes por campo (Bean Validation)
 * 4. Exception                 → 500 Internal Server Error (fallback genérico)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                LocalDateTime.now(), 404, "Not Found", ex.getMessage(), request.getRequestURI(), null
        ));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(
                LocalDateTime.now(), 400, "Bad Request", ex.getMessage(), request.getRequestURI(), null
        ));
    }

    /**
     * Captura erros do Bean Validation (@Valid no controller).
     * Extrai todos os erros de campo e os lista no body — o cliente sabe
     * exatamente quais campos e por que falharam, sem precisar tentar novamente às cegas.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorResponse.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(
                LocalDateTime.now(), 400, "Validation Error", "Erro de validação nos campos enviados",
                request.getRequestURI(), fieldErrors
        ));
    }

    // Handler genérico: nunca deve expor detalhes internos ao cliente em produção
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                LocalDateTime.now(), 500, "Internal Server Error", "Erro interno inesperado",
                request.getRequestURI(), null
        ));
    }
}
