package br.com.fiap.challenge.dto.request;

import jakarta.validation.constraints.*;

public record VeterinarianRequest(
        @NotBlank(message = "Nome é obrigatório") @Size(min = 2, max = 100) String name,
        @NotBlank(message = "CRMV é obrigatório") @Size(max = 20) String crmv,
        @Email(message = "E-mail inválido") String email,
        @Pattern(regexp = "\\d{10,11}", message = "Telefone deve ter 10 ou 11 dígitos") String phone,
        @Size(max = 100) String specialty
) {}
