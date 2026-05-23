package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.entity.Pet;
import br.com.fiap.challenge.enums.Species;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositório JPA para a entidade Pet.
 * Usa JPQL com parâmetros opcionais para implementar busca dinâmica com filtros,
 * evitando a criação de múltiplos métodos findBy para cada combinação possível.
 */
@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    Page<Pet> findByTutorId(Long tutorId, Pageable pageable);

    Page<Pet> findByVeterinarianId(Long veterinarianId, Pageable pageable);

    /**
     * Busca dinâmica com múltiplos filtros opcionais.
     * O truque ":param IS NULL OR ..." permite que cada filtro seja ignorado
     * quando não fornecido, sem precisar construir queries programaticamente.
     */
    @Query("SELECT p FROM Pet p WHERE " +
           "(:tutorId IS NULL OR p.tutor.id = :tutorId) AND " +
           "(:species IS NULL OR p.species = :species) AND " +
           "(:breed IS NULL OR LOWER(p.breed) LIKE LOWER(CONCAT('%', :breed, '%'))) AND " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))")
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
