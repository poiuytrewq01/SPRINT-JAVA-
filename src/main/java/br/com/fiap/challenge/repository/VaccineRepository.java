package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.entity.Vaccine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VaccineRepository extends JpaRepository<Vaccine, Long> {

    Page<Vaccine> findByPetId(Long petId, Pageable pageable);

    @Query("SELECT v FROM Vaccine v WHERE v.pet.id = :petId AND v.nextDoseDate IS NOT NULL AND v.nextDoseDate < :today")
    List<Vaccine> findOverdueByPetId(@Param("petId") Long petId, @Param("today") LocalDate today);

    @Query("SELECT v FROM Vaccine v WHERE v.pet.id = :petId AND v.nextDoseDate IS NOT NULL AND v.nextDoseDate BETWEEN :today AND :until ORDER BY v.nextDoseDate ASC")
    List<Vaccine> findUpcomingByPetId(@Param("petId") Long petId, @Param("today") LocalDate today, @Param("until") LocalDate until);

    @Query("SELECT COUNT(v) FROM Vaccine v WHERE v.pet.id = :petId AND v.nextDoseDate IS NOT NULL AND v.nextDoseDate < :today")
    long countOverdueByPetId(@Param("petId") Long petId, @Param("today") LocalDate today);
}
