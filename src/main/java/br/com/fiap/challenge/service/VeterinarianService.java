package br.com.fiap.challenge.service;

import br.com.fiap.challenge.dto.request.VeterinarianRequest;
import br.com.fiap.challenge.dto.response.VeterinarianResponse;
import br.com.fiap.challenge.entity.Veterinarian;
import br.com.fiap.challenge.exception.BusinessException;
import br.com.fiap.challenge.exception.ResourceNotFoundException;
import br.com.fiap.challenge.repository.VeterinarianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VeterinarianService {

    private final VeterinarianRepository veterinarianRepository;

    @Cacheable("veterinarians")
    @Transactional(readOnly = true)
    public Page<VeterinarianResponse> findAll(String name, String specialty, Pageable pageable) {
        return veterinarianRepository.findByFilters(name, specialty, pageable).map(VeterinarianResponse::from);
    }

    @Transactional(readOnly = true)
    public VeterinarianResponse findById(Long id) {
        return VeterinarianResponse.from(getVetOrThrow(id));
    }

    @CacheEvict(value = "veterinarians", allEntries = true)
    @Transactional
    public VeterinarianResponse create(VeterinarianRequest request) {
        if (veterinarianRepository.existsByCrmv(request.crmv()))
            throw new BusinessException("CRMV já cadastrado: " + request.crmv());

        Veterinarian vet = Veterinarian.builder()
                .name(request.name())
                .crmv(request.crmv())
                .email(request.email())
                .phone(request.phone())
                .specialty(request.specialty())
                .build();

        return VeterinarianResponse.from(veterinarianRepository.save(vet));
    }

    @CacheEvict(value = "veterinarians", allEntries = true)
    @Transactional
    public VeterinarianResponse update(Long id, VeterinarianRequest request) {
        Veterinarian vet = getVetOrThrow(id);

        if (!vet.getCrmv().equals(request.crmv()) && veterinarianRepository.existsByCrmv(request.crmv()))
            throw new BusinessException("CRMV já cadastrado: " + request.crmv());

        vet.setName(request.name());
        vet.setCrmv(request.crmv());
        vet.setEmail(request.email());
        vet.setPhone(request.phone());
        vet.setSpecialty(request.specialty());

        return VeterinarianResponse.from(veterinarianRepository.save(vet));
    }

    @CacheEvict(value = "veterinarians", allEntries = true)
    @Transactional
    public void delete(Long id) {
        getVetOrThrow(id);
        veterinarianRepository.deleteById(id);
    }

    public Veterinarian getVetOrThrow(Long id) {
        return veterinarianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinário", id));
    }
}
