package br.com.fiap.challenge.service;

import br.com.fiap.challenge.entity.User;
import br.com.fiap.challenge.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Numeros consolidados da plataforma, usados pelo painel administrativo.
 *
 * Existe para que o AdminController nao precise injetar seis repositorios e
 * montar as contagens ele mesmo. O controller pede um objeto pronto; a
 * responsabilidade de saber de onde cada numero vem fica aqui.
 */
@Service
@RequiredArgsConstructor
public class PlatformOverviewService {

    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final PetRepository petRepository;
    private final VaccineRepository vaccineRepository;
    private final ClinicalRecordRepository clinicalRecordRepository;

    /** Contagens exibidas no painel. Um record: dado de leitura, sem comportamento. */
    public record Overview(long usuarios, long tutores, long veterinarios,
                           long pets, long vacinas, long prontuarios) {}

    @Transactional(readOnly = true)
    public Overview summarize() {
        return new Overview(
                userRepository.count(),
                tutorRepository.count(),
                veterinarianRepository.count(),
                petRepository.count(),
                vaccineRepository.count(),
                clinicalRecordRepository.count());
    }

    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAllByOrderByRoleAscNameAsc();
    }
}
