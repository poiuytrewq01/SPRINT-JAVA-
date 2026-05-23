package br.com.fiap.challenge.service;

import br.com.fiap.challenge.dto.request.TutorRequest;
import br.com.fiap.challenge.dto.response.TutorResponse;
import br.com.fiap.challenge.entity.Tutor;
import br.com.fiap.challenge.exception.BusinessException;
import br.com.fiap.challenge.exception.ResourceNotFoundException;
import br.com.fiap.challenge.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TutorService {

    private final TutorRepository tutorRepository;

    @Transactional(readOnly = true)
    public Page<TutorResponse> findAll(String name, String email, Pageable pageable) {
        return tutorRepository.findByFilters(name, email, pageable).map(TutorResponse::from);
    }

    @Transactional(readOnly = true)
    public TutorResponse findById(Long id) {
        return TutorResponse.from(getTutorOrThrow(id));
    }

    @Transactional
    public TutorResponse create(TutorRequest request) {
        if (tutorRepository.existsByEmail(request.email()))
            throw new BusinessException("E-mail já cadastrado: " + request.email());
        if (tutorRepository.existsByCpf(request.cpf()))
            throw new BusinessException("CPF já cadastrado: " + request.cpf());

        Tutor tutor = Tutor.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .cpf(request.cpf())
                .build();

        return TutorResponse.from(tutorRepository.save(tutor));
    }

    @Transactional
    public TutorResponse update(Long id, TutorRequest request) {
        Tutor tutor = getTutorOrThrow(id);

        if (!tutor.getEmail().equals(request.email()) && tutorRepository.existsByEmail(request.email()))
            throw new BusinessException("E-mail já cadastrado: " + request.email());
        if (!tutor.getCpf().equals(request.cpf()) && tutorRepository.existsByCpf(request.cpf()))
            throw new BusinessException("CPF já cadastrado: " + request.cpf());

        tutor.setName(request.name());
        tutor.setEmail(request.email());
        tutor.setPhone(request.phone());
        tutor.setCpf(request.cpf());

        return TutorResponse.from(tutorRepository.save(tutor));
    }

    @Transactional
    public void delete(Long id) {
        getTutorOrThrow(id);
        tutorRepository.deleteById(id);
    }

    public Tutor getTutorOrThrow(Long id) {
        return tutorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tutor", id));
    }
}
