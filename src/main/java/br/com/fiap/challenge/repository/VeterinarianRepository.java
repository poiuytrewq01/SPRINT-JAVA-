package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.entity.Veterinarian;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VeterinarianRepository extends JpaRepository<Veterinarian, Long> {

    Optional<Veterinarian> findByCrmv(String crmv);

    boolean existsByCrmv(String crmv);

    @Query("SELECT v FROM Veterinarian v WHERE " +
           "(:name IS NULL OR LOWER(v.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:specialty IS NULL OR LOWER(v.specialty) LIKE LOWER(CONCAT('%', :specialty, '%')))")
    Page<Veterinarian> findByFilters(@Param("name") String name,
                                     @Param("specialty") String specialty,
                                     Pageable pageable);
}
