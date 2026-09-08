package br.com.fiap.challenge.dto.form;

import br.com.fiap.challenge.entity.Pet;
import br.com.fiap.challenge.enums.Gender;
import br.com.fiap.challenge.enums.Species;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Formulario de cadastro e edicao de pet, usado pelo tutor. */
@Getter
@Setter
public class PetForm {

    @NotBlank(message = "Informe o nome do pet")
    @Size(max = 50, message = "O nome deve ter no maximo 50 caracteres")
    private String name;

    @NotNull(message = "Escolha a especie")
    private Species species;

    @Size(max = 80, message = "A raca deve ter no maximo 80 caracteres")
    private String breed;

    @NotNull(message = "Informe a data de nascimento")
    @Past(message = "A data de nascimento deve ser anterior a hoje")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    @DecimalMin(value = "0.1", message = "O peso minimo e 0,1 kg")
    @DecimalMax(value = "300.0", message = "O peso maximo e 300 kg")
    @Digits(integer = 3, fraction = 2, message = "Use no maximo duas casas decimais")
    private BigDecimal weight;

    private Gender gender;

    private boolean profilePublic;

    /**
     * Copia os campos do formulario para o pet.
     *
     * O mapeamento fica no formulario, e nao no service, porque so o
     * formulario sabe quais campos ele expoe. Um pet tem tutor, veterinario e
     * historico clinico que nenhuma tela de cadastro edita -- e este metodo,
     * por construcao, nao toca neles.
     */
    public void applyTo(Pet pet) {
        pet.setName(name);
        pet.setSpecies(species);
        pet.setBreed(breed);
        pet.setBirthDate(birthDate);
        pet.setWeight(weight);
        pet.setGender(gender);
        pet.setProfilePublic(profilePublic);
    }

    /** Preenche o formulario a partir de um pet existente, para a tela de edicao. */
    public static PetForm from(Pet pet) {
        PetForm form = new PetForm();
        form.setName(pet.getName());
        form.setSpecies(pet.getSpecies());
        form.setBreed(pet.getBreed());
        form.setBirthDate(pet.getBirthDate());
        form.setWeight(pet.getWeight());
        form.setGender(pet.getGender());
        form.setProfilePublic(pet.isProfilePublic());
        return form;
    }
}
