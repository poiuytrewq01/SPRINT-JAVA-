package br.com.fiap.challenge.dto.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Formulario do tutor para solicitar o vinculo de um pet a um veterinario. */
@Getter
@Setter
public class VetLinkForm {

    @NotNull(message = "Escolha o pet")
    private Long petId;

    @NotNull(message = "Escolha o veterinario")
    private Long veterinarianId;

    @Size(max = 300, message = "A mensagem deve ter no maximo 300 caracteres")
    private String message;
}
