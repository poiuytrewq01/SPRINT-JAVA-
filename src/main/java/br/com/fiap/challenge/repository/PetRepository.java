package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.entity.Pet;
import br.com.fiap.challenge.enums.Species;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA para a entidade Pet.
 * Usa JPQL com parâmetros opcionais para implementar busca dinâmica com filtros,
 * evitando a criação de múltiplos métodos findBy para cada combinação possível.
 */
@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    Page<Pet> findByTutorId(Long tutorId, Pageable pageable);

    Page<Pet> findByVeterinarianId(Long veterinarianId, Pageable pageable);

    /*
     * Listagens das telas web. Retornam List, e nao Page, porque um tutor tem
     * poucos pets e um veterinario poucas dezenas de pacientes: paginar aqui
     * so acrescentaria controles de navegacao sem utilidade.
     *
     * O @EntityGraph traz os relacionamentos indicados na mesma consulta.
     *
     * Como spring.jpa.open-in-view esta desligado, a sessao do Hibernate fecha
     * junto com a transacao do service -- antes de o Thymeleaf renderizar. Sem
     * o carregamento antecipado, a tela lancaria LazyInitializationException ao
     * ler pet.veterinarian.name. Cada tela declara aqui o que realmente usa.
     */
    @EntityGraph(attributePaths = "veterinarian")
    List<Pet> findByTutorIdOrderByNameAsc(Long tutorId);

    @EntityGraph(attributePaths = "tutor")
    List<Pet> findByVeterinarianIdOrderByNameAsc(Long veterinarianId);

    /** Busca por id com tutor e veterinario carregados, para as telas de detalhe. */
    @EntityGraph(attributePaths = {"tutor", "veterinarian"})
    Optional<Pet> findWithRelationsById(Long id);

    /** Usado para conferir a posse do pet sem carregar a entidade inteira. */
    boolean existsByIdAndTutorId(Long id, Long tutorId);

    boolean existsByIdAndVeterinarianId(Long id, Long veterinarianId);

    /**
     * Busca dinâmica com múltiplos filtros opcionais.
     * O truque ":param IS NULL OR ..." permite que cada filtro seja ignorado
     * quando não fornecido, sem precisar construir queries programaticamente.
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
    @Query("SELECT p FROM Pet p WHERE " +
            "(:tutorId IS NULL OR p.tutor.id = :tutorId) AND " +
            "(:species IS NULL OR p.species = :species) AND " +
            "(CAST(:breed AS String) IS NULL OR LOWER(p.breed) LIKE LOWER(CONCAT('%', CAST(:breed AS String), '%'))) AND " +
            "(CAST(:name  AS String) IS NULL OR LOWER(p.name)  LIKE LOWER(CONCAT('%', CAST(:name  AS String), '%')))")
    Page<Pet> findByFilters(@Param("tutorId") Long tutorId,
                            @Param("species") Species species,
                            @Param("breed") String breed,
                            @Param("name") String name,
                            Pageable pageable);

    /**
     * Identifica pets sem nenhuma vacina registrada.
     * Usa LEFT JOIN + HAVING COUNT = 0 pois WHERE não pode filtrar por
     * ausência de relacionamento sem agregar primeiro.
     */
    @Query("SELECT p FROM Pet p LEFT JOIN p.vaccines v WHERE p.tutor.id = :tutorId " +
            "GROUP BY p HAVING COUNT(v) = 0")
    Page<Pet> findPetsWithoutVaccines(@Param("tutorId") Long tutorId, Pageable pageable);
}
