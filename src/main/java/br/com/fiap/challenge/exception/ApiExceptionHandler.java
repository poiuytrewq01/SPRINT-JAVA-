package br.com.fiap.challenge.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
/**
 * Tratamento de erros da API REST: toda resposta sai em JSON.
 *
 * O escopo esta limitado ao pacote dos @RestController de proposito. Um
 * advice sem escopo tambem capturaria as excecoes lancadas pelas telas web e
 * devolveria JSON ao navegador -- e, pior, o handler genérico de Exception
 * interceptaria AccessDeniedException antes do Spring Security, impedindo o
 * redirecionamento para a pagina de acesso negado.
 *
 * As telas web sao atendidas pelo WebExceptionHandler.
 */
@RestControllerAdvice(basePackages = "br.com.fiap.challenge.controller")
public class ApiExceptionHandler {

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

    /**
     * Autenticado, porem sem permissao para a operacao. Tratado explicitamente
     * para nao cair no handler genérico e virar um 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
                LocalDateTime.now(), 403, "Forbidden", "Voce nao tem permissao para esta operacao.",
                request.getRequestURI(), null
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
