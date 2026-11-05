package br.com.fiap.challenge.web;

import br.com.fiap.challenge.security.AuthenticatedUser;
import br.com.fiap.challenge.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Caixa de notificacoes do usuario logado (tutor ou veterinario).
 *
 * As notificacoes chegam pelo RabbitMQ e sao gravadas pelo
 * NotificationConsumer; esta tela apenas le o que ja esta no banco. Por isso
 * uma notificacao pode aparecer um instante depois da acao que a gerou -- e
 * o comportamento esperado de um processamento assincrono.
 */
@Controller
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
public class NotificationViewController {

    private final NotificationService notificationService;

    @GetMapping
    public String listar(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        model.addAttribute("notificacoes", notificationService.findRecent(user.getId()));
        return "notificacoes";
    }

    /**
     * Marca como lida e leva o usuario ao ponto da aplicacao a que ela se
     * refere. POST, e nao GET, porque altera estado (e assim exige o token CSRF).
     *
     * O destino vem do banco, mas e conferido de novo aqui: so caminhos
     * internos sao aceitos. Redirecionar para um endereco qualquer guardado
     * na notificacao seria uma vulnerabilidade de open redirect.
     */
    @PostMapping("/{id}/abrir")
    public String abrir(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        String link = notificationService.open(id, user.getId());
        return "redirect:" + (isInternalPath(link) ? link : "/notificacoes");
    }

    @PostMapping("/marcar-todas")
    public String marcarTodas(@AuthenticationPrincipal AuthenticatedUser user, RedirectAttributes redirect) {
        int total = notificationService.markAllAsRead(user.getId());
        redirect.addFlashAttribute("sucesso", total == 0
                ? "Nenhuma notificação pendente."
                : total + (total == 1 ? " notificação marcada como lida." : " notificações marcadas como lidas."));
        return "redirect:/notificacoes";
    }

    private static boolean isInternalPath(String link) {
        return link != null && link.startsWith("/") && !link.startsWith("//");
    }
}
