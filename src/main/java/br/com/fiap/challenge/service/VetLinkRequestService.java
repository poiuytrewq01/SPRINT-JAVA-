package br.com.fiap.challenge.service;

import br.com.fiap.challenge.entity.Pet;
import br.com.fiap.challenge.entity.User;
import br.com.fiap.challenge.entity.VetLinkRequest;
import br.com.fiap.challenge.entity.Veterinarian;
import br.com.fiap.challenge.enums.LinkRequestStatus;
import br.com.fiap.challenge.exception.BusinessException;
import br.com.fiap.challenge.exception.ResourceNotFoundException;
import br.com.fiap.challenge.repository.UserRepository;
import br.com.fiap.challenge.repository.VetLinkRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fluxo de aprovacao do vinculo entre pet e veterinario.
 *
 * O vinculo deixa de ser um campo que o tutor preenche sozinho e passa a
 * depender do aceite do profissional. O tutor solicita; o veterinario aprova
 * ou recusa; so a aprovacao altera pets.veterinarian_id.
 *
 * Duas pessoas com perfis diferentes atuam sobre o mesmo registro, entao cada
 * operacao confere nao apenas se a transicao de estado e valida, mas se quem
 * pediu tem legitimidade sobre aquele registro.
 */
@Service
@RequiredArgsConstructor
public class VetLinkRequestService {

    private final VetLinkRequestRepository linkRepository;
    private final UserRepository userRepository;
    private final PetAccessGuard petAccessGuard;
    private final VeterinarianService veterinarianService;
    private final PetHealthService petHealthService;

    // ---------------------------------------------------------------- leitura

    @Transactional(readOnly = true)
    public List<VetLinkRequest> findPendingForVet(Long veterinarianId) {
        return linkRepository.findByVeterinarianAndStatus(veterinarianId, LinkRequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public Page<VetLinkRequest> findHistoryForVet(Long veterinarianId, Pageable pageable) {
        return linkRepository.findByVeterinarian(veterinarianId, pageable);
    }

    @Transactional(readOnly = true)
    public List<VetLinkRequest> findByTutor(Long tutorId) {
        return linkRepository.findByTutor(tutorId);
    }

    @Transactional(readOnly = true)
    public long countPendingForVet(Long veterinarianId) {
        return linkRepository.countByVeterinarianIdAndStatus(veterinarianId, LinkRequestStatus.PENDING);
    }

    // ------------------------------------------------------------- solicitacao

    /**
     * Abre uma solicitacao de vinculo.
     *
     * Recusa antes de gravar quando: o pet nao e do tutor, o pet ja esta
     * vinculado aquele veterinario, ou ja existe uma solicitacao pendente para
     * o mesmo par -- caso em que uma segunda so criaria ruido na fila do
     * profissional.
     */
    @Transactional
    public VetLinkRequest request(Long petId, Long veterinarianId, String message, Long requesterUserId) {
        User requester = findUserOrThrow(requesterUserId);
        Pet pet = petAccessGuard.requireTutorAccess(petId, tutorIdOf(requester));

        Veterinarian veterinarian = veterinarianService.getVetOrThrow(veterinarianId);

        if (isAlreadyLinked(pet, veterinarianId)) {
            throw new BusinessException(
                    pet.getName() + " ja esta vinculado a " + veterinarian.getName() + ".");
        }

        if (linkRepository.existsByPetIdAndVeterinarianIdAndStatus(petId, veterinarianId, LinkRequestStatus.PENDING)) {
            throw new BusinessException(
                    "Ja existe uma solicitacao pendente de " + pet.getName()
                    + " para " + veterinarian.getName() + ".");
        }

        return linkRepository.save(VetLinkRequest.builder()
                .pet(pet)
                .veterinarian(veterinarian)
                .requestedBy(requester)
                .status(LinkRequestStatus.PENDING)
                .message(message)
                .build());
    }

    // ---------------------------------------------------------------- decisoes

    /**
     * Aprova a solicitacao e efetiva o vinculo no pet.
     *
     * As duas escritas -- status da solicitacao e veterinario do pet -- ocorrem
     * na mesma transacao. Se a segunda falhar, a primeira e desfeita, e o
     * sistema nunca fica com uma solicitacao aprovada cujo vinculo nao existe.
     */
    @Transactional
    public VetLinkRequest approve(Long requestId, String note, Long veterinarianId) {
        VetLinkRequest request = findForVeterinarian(requestId, veterinarianId);
        applyTransition(request, LinkRequestStatus.APPROVED, note);

        Pet pet = request.getPet();
        pet.setVeterinarian(request.getVeterinarian());

        // O vinculo com veterinario vale 25 dos 100 pontos do health score.
        petHealthService.evictCache(pet.getId());
        return request;
    }

    @Transactional
    public VetLinkRequest reject(Long requestId, String note, Long veterinarianId) {
        VetLinkRequest request = findForVeterinarian(requestId, veterinarianId);
        applyTransition(request, LinkRequestStatus.REJECTED, note);
        return request;
    }

    /** Desistencia do tutor, possivel apenas enquanto ninguem respondeu. */
    @Transactional
    public VetLinkRequest cancel(Long requestId, Long requesterUserId) {
        User requester = findUserOrThrow(requesterUserId);
        VetLinkRequest request = findRequestOrThrow(requestId);
        petAccessGuard.requireTutorAccess(request.getPet().getId(), tutorIdOf(requester));

        applyTransition(request, LinkRequestStatus.CANCELLED, null);
        return request;
    }

    // ----------------------------------------------------------------- apoio

    /**
     * Executa a mudanca de estado depois de consultar o proprio enum sobre a
     * validade da transicao. Concentrar isso num unico metodo evita repetir a
     * verificacao em approve, reject e cancel.
     */
    private void applyTransition(VetLinkRequest request, LinkRequestStatus target, String note) {
        LinkRequestStatus current = request.getStatus();

        if (!current.canTransitionTo(target)) {
            throw new BusinessException("Esta solicitacao ja foi "
                    + current.getLabel().toLowerCase() + " e nao pode mais ser alterada.");
        }

        request.setStatus(target);
        request.setResponseNote(note);
        request.setRespondedAt(LocalDateTime.now());
    }

    /**
     * Carrega a solicitacao garantindo que ela pertence a fila deste
     * veterinario. Sem esta checagem, bastaria trocar o id na URL para decidir
     * sobre a solicitacao de outro profissional.
     */
    private VetLinkRequest findForVeterinarian(Long requestId, Long veterinarianId) {
        VetLinkRequest request = findRequestOrThrow(requestId);

        if (!request.getVeterinarian().getId().equals(veterinarianId)) {
            throw new AccessDeniedException("Solicitacao pertence a outro veterinario.");
        }
        return request;
    }

    private VetLinkRequest findRequestOrThrow(Long requestId) {
        return linkRepository.findWithDetailsById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitacao de vinculo", requestId));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));
    }

    private Long tutorIdOf(User user) {
        return user.getTutor() != null ? user.getTutor().getId() : null;
    }

    private boolean isAlreadyLinked(Pet pet, Long veterinarianId) {
        return pet.getVeterinarian() != null
                && pet.getVeterinarian().getId().equals(veterinarianId);
    }
}
