package br.com.fiap.challenge.web;

import br.com.fiap.challenge.security.AuthenticatedUser;
import br.com.fiap.challenge.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Disponibiliza o numero de notificacoes nao lidas para todas as telas.
 *
 * O contador aparece no menu, que esta no layout -- ou seja, em toda pagina.
 * Sem este advice, cada metodo de cada controller teria de lembrar de colocar
 * o valor no Model. O @ModelAttribute de um @ControllerAdvice roda antes de
 * qualquer handler dos controllers do pacote web.
 */
@ControllerAdvice(basePackages = "br.com.fiap.challenge.web")
@RequiredArgsConstructor
public class NotificationModelAdvice {

    private final NotificationService notificationService;

    @ModelAttribute("notificacoesNaoLidas")
    public long notificacoesNaoLidas(@AuthenticationPrincipal AuthenticatedUser user) {
        return user == null ? 0 : notificationService.countUnread(user.getId());
    }
}
