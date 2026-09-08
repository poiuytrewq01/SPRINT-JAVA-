package br.com.fiap.challenge.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Tratamento de erros das telas web: toda resposta e uma pagina HTML.
 *
 * AccessDeniedException recebe tratamento especial: e relancada, para que o
 * ExceptionTranslationFilter do Spring Security possa redirecionar para a
 * pagina configurada em accessDeniedPage.
 */
@Slf4j
@ControllerAdvice(basePackages = "br.com.fiap.challenge.web")
public class WebExceptionHandler {

    private static final String ERROR_VIEW = "erro";

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("erroTitulo", "Registro nao encontrado");
        model.addAttribute("erroMensagem", ex.getMessage());
        return ERROR_VIEW;
    }

    /**
     * Regra de negocio violada em um acesso direto por URL. O caminho normal e
     * o controller capturar a excecao e devolver a mensagem no formulario; esta
     * pagina cobre o que escapou disso.
     */
    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, Model model) {
        model.addAttribute("erroTitulo", "Operacao nao permitida");
        model.addAttribute("erroMensagem", ex.getMessage());
        return ERROR_VIEW;
    }

    /**
     * Devolve a excecao para a cadeia de filtros.
     *
     * @ExceptionHandler(Exception.class), logo abaixo, capturaria tambem a
     * AccessDeniedException -- ela e uma RuntimeException como qualquer outra --
     * e o usuario veria "algo deu errado" em vez da pagina de acesso negado.
     * Relancar devolve o controle ao ExceptionTranslationFilter do Spring
     * Security, que sabe para onde redirecionar.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public void devolverAoSpringSecurity(AccessDeniedException ex) {
        throw ex;
    }

    /**
     * Falha inesperada. A causa vai para o log do servidor; a tela mostra
     * apenas uma mensagem generica, para nao expor detalhes internos.
     */
    @ExceptionHandler(Exception.class)
    public String handleUnexpected(Exception ex, Model model) {
        log.error("Erro inesperado na camada web", ex);
        model.addAttribute("erroTitulo", "Algo deu errado");
        model.addAttribute("erroMensagem", "Ocorreu um erro inesperado. Tente novamente em instantes.");
        return ERROR_VIEW;
    }
}
