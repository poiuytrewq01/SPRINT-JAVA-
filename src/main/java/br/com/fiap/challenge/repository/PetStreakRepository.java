package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.entity.PetStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PetStreakRepository extends JpaRepository<PetStreak, Long> {
    Optional<PetStreak> findByPetId(Long petId);
}
