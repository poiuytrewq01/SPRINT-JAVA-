package br.com.fiap.challenge.web;

import br.com.fiap.challenge.dto.form.ClinicalRecordForm;
import br.com.fiap.challenge.dto.request.ClinicalRecordRequest;
import br.com.fiap.challenge.entity.Pet;
import br.com.fiap.challenge.exception.BusinessException;
import br.com.fiap.challenge.security.AuthenticatedUser;
import br.com.fiap.challenge.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Area do veterinario.
 *
 * O sufixo ViewController distingue esta classe do VeterinarianController da API REST.
 * O Spring nomeia cada bean pelo nome simples da classe, entao dois
 * @Controller com o mesmo nome em pacotes diferentes disputam o mesmo bean e
 * a aplicacao nem sobe (ConflictingBeanDefinitionException). O sufixo tambem
 * deixa obvio, na leitura, qual das duas devolve HTML e qual devolve JSON.
 *
 * O acesso a um pet nao vem do perfil, e sim do vinculo aprovado: o
 * PetAccessGuard so libera o prontuario de quem esta efetivamente sob os
 * cuidados deste profissional. Um veterinario autenticado nao enxerga o
 * paciente de outro.
 */
@Controller
@RequestMapping("/vet")
@RequiredArgsConstructor
public class VetViewController {

    private static final int HISTORY_PAGE_SIZE = 10;

    private final PetService petService;
    private final PetAccessGuard petAccessGuard;
    private final PetHealthService petHealthService;
    private final VetLinkRequestService vetLinkRequestService;
    private final ClinicalRecordService clinicalRecordService;
    private final VaccineService vaccineService;

    // ------------------------------------------------------------- painel

    @GetMapping
    public String dashboard(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        Long vetId = user.getVeterinarianId();

        model.addAttribute("pacientes", petService.findEntitiesByVeterinarian(vetId));
        model.addAttribute("solicitacoesPendentes", vetLinkRequestService.countPendingForVet(vetId));
        return "vet/dashboard";
    }

    // -------------------------- FLUXO 2 (lado do vet): decidir solicitacoes

    @GetMapping("/solicitacoes")
    public String solicitacoes(@AuthenticationPrincipal AuthenticatedUser user,
                               @RequestParam(defaultValue = "0") int pagina,
                               Model model) {
        Long vetId = user.getVeterinarianId();
        Pageable pageable = PageRequest.of(pagina, HISTORY_PAGE_SIZE, Sort.by("createdAt").descending());

        model.addAttribute("pendentes", vetLinkRequestService.findPendingForVet(vetId));
        model.addAttribute("historico", vetLinkRequestService.findHistoryForVet(vetId, pageable));
        return "vet/solicitacoes";
    }

    /**
     * Aprova o vinculo. O identificador do veterinario vem da sessao e e
     * repassado ao service, que recusa a operacao caso a solicitacao pertenca
     * a fila de outro profissional.
     */
    @PostMapping("/solicitacoes/{requestId}/aprovar")
    public String aprovar(@AuthenticationPrincipal AuthenticatedUser user,
                          @PathVariable Long requestId,
                          @RequestParam(required = false) String observacao,
                          RedirectAttributes redirect) {
        return decidir(redirect, () -> {
            var pedido = vetLinkRequestService.approve(requestId, observacao, user.getVeterinarianId());
            return pedido.getPet().getName() + " agora esta sob seus cuidados.";
        });
    }

    @PostMapping("/solicitacoes/{requestId}/recusar")
    public String recusar(@AuthenticationPrincipal AuthenticatedUser user,
                          @PathVariable Long requestId,
                          @RequestParam(required = false) String observacao,
                          RedirectAttributes redirect) {
        return decidir(redirect, () -> {
            var pedido = vetLinkRequestService.reject(requestId, observacao, user.getVeterinarianId());
            return "Solicitacao de " + pedido.getPet().getName() + " recusada.";
        });
    }

    // ------------------------- FLUXO 3: atendimento e retorno automatico

    @GetMapping("/pacientes/{petId}")
    public String paciente(@AuthenticationPrincipal AuthenticatedUser user,
                           @PathVariable Long petId,
                           Model model) {
        Pet pet = petAccessGuard.requireVeterinarianAccess(petId, user.getVeterinarianId());

        model.addAttribute("pet", pet);
        model.addAttribute("resumo", petHealthService.getHealthSummary(petId));
        model.addAttribute("prontuarios",
                clinicalRecordService.findByPet(petId, null, Pageable.unpaged()).getContent());
        model.addAttribute("vacinas", vaccineService.findByPet(petId, Pageable.unpaged()).getContent());

        if (!model.containsAttribute("atendimentoForm")) {
            model.addAttribute("atendimentoForm", new ClinicalRecordForm());
        }
        return "vet/paciente";
    }

    /**
     * Registra o atendimento. O lembrete de retorno em seis meses e criado
     * pelo ClinicalRecordService dentro da mesma transacao -- o controller nao
     * conhece essa regra, apenas o resultado dela.
     */
    @PostMapping("/pacientes/{petId}/atendimentos")
    public String registrarAtendimento(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable Long petId,
                                       @Valid @ModelAttribute("atendimentoForm") ClinicalRecordForm form,
                                       BindingResult binding,
                                       RedirectAttributes redirect) {
        petAccessGuard.requireVeterinarianAccess(petId, user.getVeterinarianId());

        if (binding.hasErrors()) {
            redirect.addFlashAttribute("org.springframework.validation.BindingResult.atendimentoForm", binding);
            redirect.addFlashAttribute("atendimentoForm", form);
            return "redirect:/vet/pacientes/" + petId;
        }

        try {
            clinicalRecordService.create(new ClinicalRecordRequest(
                    form.getDate(), form.getDescription(), form.getDiagnosis(), form.getTreatment(),
                    form.getWeight(), form.getObservations(), petId, user.getVeterinarianId()));

            redirect.addFlashAttribute("sucesso",
                    "Atendimento registrado. O retorno preventivo foi agendado automaticamente.");

        } catch (BusinessException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }

        return "redirect:/vet/pacientes/" + petId;
    }

    // -------------------------------------------------------------- apoio

    /**
     * Aprovar e recusar diferem apenas na chamada ao service e na mensagem de
     * sucesso. O restante -- executar, tratar a regra de negocio violada e
     * voltar para a lista -- e identico, e fica em um lugar so.
     */
    private String decidir(RedirectAttributes redirect, Decisao decisao) {
        try {
            redirect.addFlashAttribute("sucesso", decisao.executar());
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/vet/solicitacoes";
    }

    /** Acao que decide uma solicitacao e devolve a mensagem exibida ao usuario. */
    @FunctionalInterface
    private interface Decisao {
        String executar();
    }
}
