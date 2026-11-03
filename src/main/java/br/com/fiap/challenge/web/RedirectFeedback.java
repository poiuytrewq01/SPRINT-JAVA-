package br.com.fiap.challenge.web;

import br.com.fiap.challenge.exception.BusinessException;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.function.Supplier;

/**
 * Padroes repetidos das acoes POST das telas, extraidos para um lugar so.
 *
 * Toda acao que altera dados segue o mesmo roteiro: executar, mostrar a
 * mensagem de sucesso ou o erro de regra de negocio, e redirecionar (padrao
 * POST-Redirect-GET). Antes cada metodo de controller repetia o mesmo bloco
 * try/catch; agora ele declara so o que e proprio dele -- a acao e o destino.
 *
 * Classe utilitaria: final e com construtor privado, porque nao guarda estado
 * e nao faz sentido instancia-la.
 */
final class RedirectFeedback {

    private static final String BINDING_RESULT_PREFIX = "org.springframework.validation.BindingResult.";

    private RedirectFeedback() {
    }

    /**
     * Executa a acao e devolve o redirect para o destino.
     *
     * @param acao executa a operacao e devolve a mensagem de sucesso
     */
    static String executar(RedirectAttributes redirect, String destino, Supplier<String> acao) {
        try {
            redirect.addFlashAttribute("sucesso", acao.get());
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:" + destino;
    }

    /**
     * Leva os erros de validacao de um formulario atraves do redirect.
     *
     * Flash attributes sobrevivem a exatamente um redirect. O BindingResult
     * precisa ir com a chave que o Thymeleaf procura
     * ("org.springframework.validation.BindingResult." + nome do objeto) para
     * que th:errors encontre as mensagens na pagina de destino.
     */
    static String devolverErros(RedirectAttributes redirect, String destino,
                                String nomeDoForm, Object form, BindingResult binding) {
        redirect.addFlashAttribute(BINDING_RESULT_PREFIX + nomeDoForm, binding);
        redirect.addFlashAttribute(nomeDoForm, form);
        return "redirect:" + destino;
    }
}
