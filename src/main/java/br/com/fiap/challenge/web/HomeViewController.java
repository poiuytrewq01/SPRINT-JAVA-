package br.com.fiap.challenge.web;

import br.com.fiap.challenge.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Rotas publicas: entrada, login e aviso de acesso negado.
 *
 * O sufixo ViewController distingue esta classe do controller da API da API REST.
 * O Spring nomeia cada bean pelo nome simples da classe, entao dois
 * @Controller com o mesmo nome em pacotes diferentes disputam o mesmo bean e
 * a aplicacao nem sobe (ConflictingBeanDefinitionException). O sufixo tambem
 * deixa obvio, na leitura, qual das duas devolve HTML e qual devolve JSON.
 *
 * Nao existe metodo para o POST de /login. Esse endereco e interceptado pelo
 * UsernamePasswordAuthenticationFilter do Spring Security antes de chegar ao
 * DispatcherServlet -- criar um metodo para ele aqui seria codigo morto.
 */
@Controller
public class HomeViewController {

    /**
     * Raiz do site. Cada perfil tem sua propria area, entao quem ja esta
     * autenticado e enviado direto para a dele; os demais vao para o login.
     */
    @GetMapping("/")
    public String index(@AuthenticationPrincipal AuthenticatedUser user) {
        return user == null
                ? "redirect:/login"
                : "redirect:" + user.getRole().getHomePath();
    }

    /**
     * Tela de login.
     *
     * Os parametros "erro" e "sair" sao acrescentados pelo proprio Spring
     * Security ao redirecionar (failureUrl e logoutSuccessUrl). A presenca
     * deles define qual aviso a pagina mostra.
     */
    @GetMapping("/login")
    public String login(@AuthenticationPrincipal AuthenticatedUser user, Model model,
                        @RequestParam(required = false) String erro,
                        @RequestParam(required = false) String sair) {
        if (user != null) {
            return "redirect:" + user.getRole().getHomePath();
        }

        model.addAttribute("erroLogin", erro != null);
        model.addAttribute("logoutRealizado", sair != null);
        return "login";
    }

    /** Usuario autenticado que tentou acessar uma area de outro perfil. */
    @GetMapping("/acesso-negado")
    public String acessoNegado() {
        return "acesso-negado";
    }
}
