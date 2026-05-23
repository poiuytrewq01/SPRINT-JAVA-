package br.com.fiap.challenge.dto.response;

import br.com.fiap.challenge.entity.Veterinarian;

import java.time.LocalDateTime;

public record VeterinarianResponse(
        Long id,
        String name,
        String crmv,
        String email,
        String phone,
        String specialty,
        LocalDateTime createdAt
) {
    public static VeterinarianResponse from(Veterinarian v) {
        return new VeterinarianResponse(v.getId(), v.getName(), v.getCrmv(), v.getEmail(),
                v.getPhone(), v.getSpecialty(), v.getCreatedAt());
    }
}
