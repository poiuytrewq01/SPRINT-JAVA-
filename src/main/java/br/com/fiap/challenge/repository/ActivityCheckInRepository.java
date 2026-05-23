package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.entity.ActivityCheckIn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ActivityCheckInRepository extends JpaRepository<ActivityCheckIn, Long> {

    boolean existsByPetIdAndDate(Long petId, LocalDate date);

    Page<ActivityCheckIn> findByPetIdOrderByDateDesc(Long petId, Pageable pageable);

    @Query("SELECT c FROM ActivityCheckIn c WHERE c.pet.id = :petId AND " +
           "c.date BETWEEN :start AND :end ORDER BY c.date DESC")
    Page<ActivityCheckIn> findByPetIdAndDateRange(@Param("petId") Long petId,
                                                   @Param("start") LocalDate start,
                                                   @Param("end") LocalDate end,
                                                   Pageable pageable);

    @Query("SELECT COUNT(c) FROM ActivityCheckIn c WHERE c.pet.id = :petId")
    long countByPetId(@Param("petId") Long petId);
}
