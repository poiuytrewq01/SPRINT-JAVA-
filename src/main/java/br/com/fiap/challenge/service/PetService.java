package br.com.fiap.challenge.service;

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

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final TutorService tutorService;
    private final VeterinarianService veterinarianService;

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
}
