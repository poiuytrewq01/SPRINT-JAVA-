package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.entity.ClinicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * Repositório JPA para ClinicalRecord (prontuários).
 * A query hasRecentRecord é usada pelo PetHealthService para verificar
 * se o pet teve consulta nos últimos 12 meses — um dos critérios do health score.
 */
@Repository
public interface ClinicalRecordRepository extends JpaRepository<ClinicalRecord, Long> {

    // Spring Data gera o SQL automaticamente a partir do nome do método
    Page<ClinicalRecord> findByPetIdOrderByDateDesc(Long petId, Pageable pageable);

    /**
     * Verifica se existe ao menos um prontuário após a data informada.
     * Retorna boolean direto em vez de List para evitar carregar entidades desnecessariamente.
     */
    @Query("SELECT COUNT(r) > 0 FROM ClinicalRecord r WHERE r.pet.id = :petId AND r.date >= :since")
    boolean hasRecentRecord(@Param("petId") Long petId, @Param("since") LocalDate since);

    @Query("SELECT COUNT(r) FROM ClinicalRecord r WHERE r.pet.id = :petId")
    long countByPetId(@Param("petId") Long petId);

    /**
     * Busca textual em descrição e diagnóstico — permite ao tutor localizar
     * prontuários por sintoma, doença ou procedimento realizado.
     * LOWER + LIKE garante busca case-insensitive independente do banco.
     */
    /*
     * O CAST(:param AS String) nao e enfeite.
     *
     * Quando o filtro vem nulo, o PostgreSQL recebe um parametro sem tipo
     * declarado dentro de CONCAT/LOWER, nao consegue inferi-lo e assume bytea,
     * falhando com "function lower(bytea) does not exist". O CAST informa o
     * tipo e a consulta passa.
     *
     * No H2 da sprint anterior isso nunca aparecia: ele e permissivo com
     * parametros sem tipo. E um caso classico de query que funciona em um banco
     * e quebra em outro -- motivo pelo qual o ambiente de desenvolvimento deve
     * usar o mesmo banco da producao.
     */
    @Query("SELECT r FROM ClinicalRecord r WHERE r.pet.id = :petId AND " +
            "(CAST(:keyword AS String) IS NULL " +
            "OR LOWER(r.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')) " +
            "OR LOWER(r.diagnosis)   LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')))")
    Page<ClinicalRecord> findByPetIdAndKeyword(@Param("petId") Long petId,
                                               @Param("keyword") String keyword,
                                               Pageable pageable);
}
