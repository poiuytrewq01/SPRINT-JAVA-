package br.com.fiap.challenge.dto.response;

import br.com.fiap.challenge.entity.Pet;
import br.com.fiap.challenge.enums.Gender;
import br.com.fiap.challenge.enums.Species;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

public record PetResponse(
        Long id,
        String name,
        Species species,
        String breed,
        LocalDate birthDate,
        int ageYears,
        BigDecimal weight,
        Gender gender,
        boolean profilePublic,
        Long tutorId,
        String tutorName,
        Long veterinarianId,
        String veterinarianName,
        LocalDateTime createdAt
) {
    public static PetResponse from(Pet p) {
        int age = p.getBirthDate() != null ? Period.between(p.getBirthDate(), LocalDate.now()).getYears() : 0;
        return new PetResponse(
                p.getId(), p.getName(), p.getSpecies(), p.getBreed(), p.getBirthDate(), age,
                p.getWeight(), p.getGender(), p.isProfilePublic(),
                p.getTutor() != null ? p.getTutor().getId() : null,
                p.getTutor() != null ? p.getTutor().getName() : null,
                p.getVeterinarian() != null ? p.getVeterinarian().getId() : null,
                p.getVeterinarian() != null ? p.getVeterinarian().getName() : null,
                p.getCreatedAt()
        );
    }
}
