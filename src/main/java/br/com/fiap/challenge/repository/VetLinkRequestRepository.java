package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.entity.VetLinkRequest;
import br.com.fiap.challenge.enums.LinkRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VetLinkRequestRepository extends JpaRepository<VetLinkRequest, Long> {

    /**
     * Busca por id com pet e veterinario carregados.
     *
     * O controller monta a mensagem de sucesso a partir do objeto devolvido
     * ("Rex agora esta sob seus cuidados"), ja fora da transacao. Com o
     * findById padrao, pet seria um proxy nao inicializado no caso de recusa --
     * onde nada no fluxo toca o pet -- e a leitura do nome falharia.
     */
    @EntityGraph(attributePaths = {"pet", "veterinarian"})
    Optional<VetLinkRequest> findWithDetailsById(Long id);

    /** Fila de trabalho do veterinario: o que aguarda decisao dele. */
    @Query("""
           SELECT r FROM VetLinkRequest r
           JOIN FETCH r.pet p
           JOIN FETCH p.tutor
           WHERE r.veterinarian.id = :vetId AND r.status = :status
           ORDER BY r.createdAt ASC
           """)
    List<VetLinkRequest> findByVeterinarianAndStatus(@Param("vetId") Long vetId,
                                                     @Param("status") LinkRequestStatus status);

    /** Historico completo do veterinario, paginado. */
    @Query(value = """
                   SELECT r FROM VetLinkRequest r
                   JOIN FETCH r.pet p
                   JOIN FETCH p.tutor
                   JOIN FETCH r.veterinarian
                   WHERE r.veterinarian.id = :vetId
                   """,
           countQuery = "SELECT COUNT(r) FROM VetLinkRequest r WHERE r.veterinarian.id = :vetId")
    Page<VetLinkRequest> findByVeterinarian(@Param("vetId") Long vetId, Pageable pageable);

    /** Solicitacoes abertas pelo tutor, para acompanhamento na tela dele. */
    @Query("""
           SELECT r FROM VetLinkRequest r
           JOIN FETCH r.pet
           JOIN FETCH r.veterinarian
           WHERE r.pet.tutor.id = :tutorId
           ORDER BY r.createdAt DESC
           """)
    List<VetLinkRequest> findByTutor(@Param("tutorId") Long tutorId);

    /**
     * Complemento em codigo do indice unico parcial uk_vet_link_pending:
     * permite recusar a solicitacao com uma mensagem clara em vez de deixar
     * o banco lancar uma violacao de constraint.
     */
    boolean existsByPetIdAndVeterinarianIdAndStatus(Long petId, Long veterinarianId, LinkRequestStatus status);

    long countByVeterinarianIdAndStatus(Long veterinarianId, LinkRequestStatus status);
}
