package br.com.fiap.challenge.service;

import br.com.fiap.challenge.entity.Pet;
import br.com.fiap.challenge.exception.ResourceNotFoundException;
import br.com.fiap.challenge.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responde a uma unica pergunta: este usuario pode acessar este pet?
 *
 * Proteger a rota por perfil garante apenas que quem entrou em /tutor/** e um
 * tutor -- nao que aquele pet seja dele. Sem uma verificacao por registro,
 * trocar o id na URL daria acesso ao pet de outra pessoa. E a diferenca entre
 * autorizacao por papel e autorizacao por propriedade do dado.
 *
 * A regra vive em um componente proprio porque e usada tanto pelos controllers
 * quanto pelos services: duplica-la seria repetir uma decisao de seguranca em
 * varios lugares, com o risco de corrigir um e esquecer o outro.
 */
@Component
@RequiredArgsConstructor
public class PetAccessGuard {

    private final PetRepository petRepository;

    /** Carrega o pet somente se ele pertencer a este tutor. */
    @Transactional(readOnly = true)
    public Pet requireTutorAccess(Long petId, Long tutorId) {
        Pet pet = findOrThrow(petId);

        if (tutorId == null || !pet.getTutor().getId().equals(tutorId)) {
            throw new AccessDeniedException("Este pet nao pertence ao tutor autenticado.");
        }
        return pet;
    }

    /**
     * Carrega o pet somente se ele estiver vinculado a este veterinario.
     * O vinculo e o que autoriza o acesso ao prontuario -- e ele so existe
     * depois que o proprio veterinario aprovou a solicitacao do tutor.
     */
    @Transactional(readOnly = true)
    public Pet requireVeterinarianAccess(Long petId, Long veterinarianId) {
        Pet pet = findOrThrow(petId);

        boolean linked = pet.getVeterinarian() != null
                && veterinarianId != null
                && pet.getVeterinarian().getId().equals(veterinarianId);

        if (!linked) {
            throw new AccessDeniedException("Este pet nao esta vinculado ao veterinario autenticado.");
        }
        return pet;
    }

    private Pet findOrThrow(Long petId) {
        // Com relacionamentos carregados: o pet devolvido aqui e renderizado
        // pela view, ja fora da transacao.
        return petRepository.findWithRelationsById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", petId));
    }
}
