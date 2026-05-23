package br.com.fiap.challenge.dto.response;

import br.com.fiap.challenge.entity.Tutor;

import java.time.LocalDateTime;

public record TutorResponse(
        Long id,
        String name,
        String email,
        String phone,
        String cpf,
        int totalPets,
        LocalDateTime createdAt
) {
    public static TutorResponse from(Tutor t) {
        return new TutorResponse(t.getId(), t.getName(), t.getEmail(), t.getPhone(), t.getCpf(),
                t.getPets().size(), t.getCreatedAt());
    }
}
