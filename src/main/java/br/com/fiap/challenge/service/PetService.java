package br.com.fiap.challenge.service;

import br.com.fiap.challenge.dto.form.PetForm;
import br.com.fiap.challenge.dto.request.PetRequest;
import br.com.fiap.challenge.dto.response.PetResponse;
import br.com.fiap.challenge.entity.Pet;
import br.com.fiap.challenge.entity.Veterinarian;
import br.com.fiap.challenge.enums.Species;
import br.com.fiap.challenge.exception.ResourceNotFoundException;
import br.com.fiap.challenge.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final TutorService tutorService;
    private final VeterinarianService veterinarianService;
    private final PetAccessGuard petAccessGuard;

    @Transactional(readOnly = true)
    public Page<PetResponse> findAll(Long tutorId, Species species, String breed, String name, Pageable pageable) {
        return petRepository.findByFilters(tutorId, species, breed, name, pageable).map(PetResponse::from);
    }

    @Transactional(readOnly = true)
    public PetResponse findById(Long id) {
        return PetResponse.from(getPetOrThrow(id));
    }

    @Transactional
    public PetResponse create(PetRequest request) {
        Veterinarian vet = request.veterinarianId() != null
                ? veterinarianService.getVetOrThrow(request.veterinarianId()) : null;

        Pet pet = Pet.builder()
                .name(request.name())
                .species(request.species())
                .breed(request.breed())
                .birthDate(request.birthDate())
                .weight(request.weight())
                .gender(request.gender())
                .profilePublic(Boolean.TRUE.equals(request.profilePublic()))
                .tutor(tutorService.getTutorOrThrow(request.tutorId()))
                .veterinarian(vet)
                .build();

        return PetResponse.from(petRepository.save(pet));
    }

    @CacheEvict(value = "petHealthSummary", key = "#id")
    @Transactional
    public PetResponse update(Long id, PetRequest request) {
        Pet pet = getPetOrThrow(id);
        Veterinarian vet = request.veterinarianId() != null
                ? veterinarianService.getVetOrThrow(request.veterinarianId()) : null;

        pet.setName(request.name());
        pet.setSpecies(request.species());
        pet.setBreed(request.breed());
        pet.setBirthDate(request.birthDate());
        pet.setWeight(request.weight());
        pet.setGender(request.gender());
        pet.setProfilePublic(Boolean.TRUE.equals(request.profilePublic()));
        pet.setTutor(tutorService.getTutorOrThrow(request.tutorId()));
        pet.setVeterinarian(vet);

        return PetResponse.from(petRepository.save(pet));
    }

    @CacheEvict(value = "petHealthSummary", key = "#id")
    @Transactional
    public void delete(Long id) {
        getPetOrThrow(id);
        petRepository.deleteById(id);
    }

    public Pet getPetOrThrow(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", id));
    }

    // ------------------------------------------------------- camada web (MVC)
    //
    // As telas renderizam a entidade diretamente, e nao o PetResponse: o
    // Thymeleaf roda dentro da requisicao e navega pelos relacionamentos
    // (streak, vacinas) conforme a pagina precisa. Os DTOs continuam sendo
    // usados pela API, onde a resposta precisa ser um contrato fechado.

    @Transactional(readOnly = true)
    public List<Pet> findEntitiesByTutor(Long tutorId) {
        return petRepository.findByTutorIdOrderByNameAsc(tutorId);
    }

    @Transactional(readOnly = true)
    public List<Pet> findEntitiesByVeterinarian(Long veterinarianId) {
        return petRepository.findByVeterinarianIdOrderByNameAsc(veterinarianId);
    }

    /**
     * Cadastra um pet ja associado ao tutor autenticado.
     *
     * O tutor vem da sessao, nunca do formulario. Se viesse do formulario,
     * bastaria alterar o campo na requisicao para cadastrar um pet no nome de
     * outra pessoa.
     */
    @Transactional
    public Pet createForTutor(PetForm form, Long tutorId) {
        // Criado pelo builder, e nao por new Pet(): o Lombok so aplica os
        // valores de @Builder.Default no builder. Um construtor vazio deixaria
        // as listas de vacinas e prontuarios nulas, e o calculo do percentual
        // de perfil completo quebraria ao percorre-las.
        Pet pet = Pet.builder()
                .tutor(tutorService.getTutorOrThrow(tutorId))
                .build();

        form.applyTo(pet);
        return petRepository.save(pet);
    }

    @CacheEvict(value = "petHealthSummary", key = "#petId")
    @Transactional
    public Pet updateForTutor(Long petId, PetForm form, Long tutorId) {
        Pet pet = petAccessGuard.requireTutorAccess(petId, tutorId);
        form.applyTo(pet);

        return petRepository.save(pet);
    }
}
