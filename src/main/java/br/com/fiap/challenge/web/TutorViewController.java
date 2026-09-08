package br.com.fiap.challenge.web;

import br.com.fiap.challenge.dto.form.CheckInForm;
import br.com.fiap.challenge.dto.form.PetForm;
import br.com.fiap.challenge.dto.form.VetLinkForm;
import br.com.fiap.challenge.dto.request.ActivityCheckInRequest;
import br.com.fiap.challenge.entity.Pet;
import br.com.fiap.challenge.enums.*;
import br.com.fiap.challenge.exception.BusinessException;
import br.com.fiap.challenge.security.AuthenticatedUser;
import br.com.fiap.challenge.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Area do tutor.
 *
 * O sufixo ViewController distingue esta classe do TutorController da API REST.
 * O Spring nomeia cada bean pelo nome simples da classe, entao dois
 * @Controller com o mesmo nome em pacotes diferentes disputam o mesmo bean e
 * a aplicacao nem sobe (ConflictingBeanDefinitionException). O sufixo tambem
 * deixa obvio, na leitura, qual das duas devolve HTML e qual devolve JSON.
 *
 * Toda rota daqui exige o perfil TUTOR -- a regra esta no SecurityConfig, nao
 * repetida em cada metodo. O identificador do tutor vem sempre de
 * @AuthenticationPrincipal, nunca da URL ou do formulario: e o que impede que
 * trocar um id no endereco de acesso aos dados de outra pessoa.
 */
@Controller
@RequestMapping("/tutor")
@RequiredArgsConstructor
public class TutorViewController {

    private static final int UPCOMING_REMINDER_DAYS = 30;

    private final PetService petService;
    private final PetAccessGuard petAccessGuard;
    private final PetHealthService petHealthService;
    private final ActivityCheckInService checkInService;
    private final VaccineService vaccineService;
    private final ClinicalRecordService clinicalRecordService;
    private final ReminderService reminderService;
    private final VetLinkRequestService vetLinkRequestService;
    private final VeterinarianService veterinarianService;

    // ------------------------------------------------------------- painel

    @GetMapping
    public String dashboard(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        List<Pet> pets = petService.findEntitiesByTutor(user.getTutorId());

        // Um resumo de saude por pet, indexado pelo id: a tela consulta o mapa
        // dentro do laco, sem precisar de uma chamada por linha no template.
        Map<Long, ?> resumos = pets.stream().collect(Collectors.toMap(
                Pet::getId,
                pet -> petHealthService.getHealthSummary(pet.getId()),
                (a, b) -> a,
                java.util.LinkedHashMap::new));

        model.addAttribute("pets", pets);
        model.addAttribute("resumos", resumos);
        return "tutor/dashboard";
    }

    // ---------------------------------------------------------- cadastro

    @GetMapping("/pets/novo")
    public String novoPet(Model model) {
        model.addAttribute("petForm", new PetForm());
        popularOpcoesDePet(model);
        return "tutor/pet-form";
    }

    /**
     * O @Valid dispara o Bean Validation antes do corpo do metodo. O
     * BindingResult logo em seguida -- a ordem importa, e obrigatoria -- recebe
     * os erros; sem ele, a falha viraria uma excecao em vez de voltar para o
     * formulario com as mensagens.
     */
    @PostMapping("/pets")
    public String salvarPet(@AuthenticationPrincipal AuthenticatedUser user,
                            @Valid @ModelAttribute("petForm") PetForm petForm,
                            BindingResult binding,
                            Model model,
                            RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            popularOpcoesDePet(model);
            return "tutor/pet-form";
        }

        Pet pet = petService.createForTutor(petForm, user.getTutorId());
        redirect.addFlashAttribute("sucesso", pet.getName() + " foi cadastrado.");
        return "redirect:/tutor/pets/" + pet.getId();
    }

    // ------------------------------------------------------ detalhe do pet

    @GetMapping("/pets/{petId}")
    public String detalhePet(@AuthenticationPrincipal AuthenticatedUser user,
                             @PathVariable Long petId,
                             Model model) {
        Pet pet = petAccessGuard.requireTutorAccess(petId, user.getTutorId());

        model.addAttribute("pet", pet);
        model.addAttribute("resumo", petHealthService.getHealthSummary(petId));
        model.addAttribute("vacinas", vaccineService.findByPet(petId, Pageable.unpaged()).getContent());
        model.addAttribute("prontuarios",
                clinicalRecordService.findByPet(petId, null, Pageable.unpaged()).getContent());
        model.addAttribute("lembretes", reminderService.findUpcoming(petId, UPCOMING_REMINDER_DAYS));
        model.addAttribute("checkInHoje", checkInService.hasCheckedInToday(petId));

        if (!model.containsAttribute("checkInForm")) {
            model.addAttribute("checkInForm", new CheckInForm());
        }
        model.addAttribute("tiposAtividade", ActivityType.values());
        return "tutor/pet-detalhe";
    }

    // ------------------------------------------- FLUXO 1: check-in diario

    /**
     * Registra o check-in do dia e atualiza o streak.
     *
     * Erros de validacao e violacoes de regra de negocio voltam como flash
     * attributes porque a resposta e um redirect. O padrao POST-Redirect-GET
     * evita que atualizar a pagina reenvie o formulario e tente um segundo
     * check-in no mesmo dia.
     */
    @PostMapping("/pets/{petId}/check-in")
    public String registrarCheckIn(@AuthenticationPrincipal AuthenticatedUser user,
                                   @PathVariable Long petId,
                                   @Valid @ModelAttribute("checkInForm") CheckInForm form,
                                   BindingResult binding,
                                   RedirectAttributes redirect) {
        petAccessGuard.requireTutorAccess(petId, user.getTutorId());

        if (binding.hasErrors()) {
            redirect.addFlashAttribute("org.springframework.validation.BindingResult.checkInForm", binding);
            redirect.addFlashAttribute("checkInForm", form);
            return "redirect:/tutor/pets/" + petId;
        }

        try {
            var checkIn = checkInService.checkIn(petId, new ActivityCheckInRequest(
                    form.getActivityType(), form.getDurationMinutes(), form.getNotes()));

            var streak = checkInService.getStreak(petId);
            redirect.addFlashAttribute("sucesso", mensagemDeStreak(streak.currentStreak(), streak.levelLabel()));
            redirect.addFlashAttribute("checkInRegistrado", checkIn.date());

        } catch (BusinessException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }

        return "redirect:/tutor/pets/" + petId;
    }

    // ------------------------------------ FLUXO 2: vinculo com veterinario

    @GetMapping("/vinculos")
    public String vinculos(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        model.addAttribute("solicitacoes", vetLinkRequestService.findByTutor(user.getTutorId()));
        model.addAttribute("pets", petService.findEntitiesByTutor(user.getTutorId()));
        model.addAttribute("veterinarios", veterinarianService.findAllEntities());

        if (!model.containsAttribute("vetLinkForm")) {
            model.addAttribute("vetLinkForm", new VetLinkForm());
        }
        return "tutor/vinculos";
    }

    @PostMapping("/vinculos")
    public String solicitarVinculo(@AuthenticationPrincipal AuthenticatedUser user,
                                   @Valid @ModelAttribute("vetLinkForm") VetLinkForm form,
                                   BindingResult binding,
                                   RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("org.springframework.validation.BindingResult.vetLinkForm", binding);
            redirect.addFlashAttribute("vetLinkForm", form);
            return "redirect:/tutor/vinculos";
        }

        try {
            vetLinkRequestService.request(form.getPetId(), form.getVeterinarianId(),
                    form.getMessage(), user.getId());
            redirect.addFlashAttribute("sucesso",
                    "Solicitacao enviada. O veterinario precisa aprovar para o vinculo valer.");

        } catch (BusinessException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }

        return "redirect:/tutor/vinculos";
    }

    @PostMapping("/vinculos/{requestId}/cancelar")
    public String cancelarVinculo(@AuthenticationPrincipal AuthenticatedUser user,
                                  @PathVariable Long requestId,
                                  RedirectAttributes redirect) {
        try {
            vetLinkRequestService.cancel(requestId, user.getId());
            redirect.addFlashAttribute("sucesso", "Solicitacao cancelada.");

        } catch (BusinessException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }

        return "redirect:/tutor/vinculos";
    }

    // --------------------------- FLUXO 3 (lado do tutor): fechar lembrete

    @PostMapping("/pets/{petId}/lembretes/{reminderId}/concluir")
    public String concluirLembrete(@AuthenticationPrincipal AuthenticatedUser user,
                                   @PathVariable Long petId,
                                   @PathVariable Long reminderId,
                                   RedirectAttributes redirect) {
        petAccessGuard.requireTutorAccess(petId, user.getTutorId());

        try {
            reminderService.markDoneForPet(reminderId, petId);
            redirect.addFlashAttribute("sucesso", "Lembrete marcado como concluido.");

        } catch (BusinessException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }

        return "redirect:/tutor/pets/" + petId;
    }

    // -------------------------------------------------------------- apoio

    private void popularOpcoesDePet(Model model) {
        model.addAttribute("especies", Species.values());
        model.addAttribute("generos", Gender.values());
    }

    private String mensagemDeStreak(int streak, String nivel) {
        return streak == 1
                ? "Check-in registrado. Comeca aqui uma nova sequencia."
                : "Check-in registrado. Sequencia de " + streak + " dias -- nivel " + nivel + ".";
    }
}
