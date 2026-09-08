package br.com.fiap.challenge.web;

import br.com.fiap.challenge.service.PlatformOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Area administrativa: visao consolidada da plataforma e das contas de acesso.
 *
 * O sufixo ViewController distingue esta classe do controller da API da API REST.
 * O Spring nomeia cada bean pelo nome simples da classe, entao dois
 * @Controller com o mesmo nome em pacotes diferentes disputam o mesmo bean e
 * a aplicacao nem sobe (ConflictingBeanDefinitionException). O sufixo tambem
 * deixa obvio, na leitura, qual das duas devolve HTML e qual devolve JSON.
 *
 * O perfil ADMIN e deliberadamente somente leitura sobre o dominio clinico.
 * Registrar um prontuario exige responsabilidade tecnica de um veterinario, e
 * um administrador de sistema nao tem essa atribuicao -- por isso ele enxerga
 * os numeros, mas nao escreve no historico de saude de nenhum pet.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminViewController {

    private final PlatformOverviewService overviewService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("resumo", overviewService.summarize());
        model.addAttribute("usuarios", overviewService.findAllUsers());
        return "admin/dashboard";
    }
}
