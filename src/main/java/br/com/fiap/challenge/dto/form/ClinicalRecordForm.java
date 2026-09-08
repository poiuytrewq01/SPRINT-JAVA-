package br.com.fiap.challenge.dto.form;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Formulario de atendimento clinico preenchido pelo veterinario.
 *
 * O @DateTimeFormat converte o texto "aaaa-mm-dd" enviado pelo input
 * type="date" em LocalDate. Sem ele, a data chegaria como String e o binding
 * falharia.
 */
@Getter
@Setter
public class ClinicalRecordForm {

    @NotNull(message = "Informe a data do atendimento")
    @PastOrPresent(message = "A data do atendimento nao pode ser futura")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date = LocalDate.now();

    @NotBlank(message = "Descreva o atendimento")
    @Size(max = 200, message = "A descricao deve ter no maximo 200 caracteres")
    private String description;

    @Size(max = 300, message = "O diagnostico deve ter no maximo 300 caracteres")
    private String diagnosis;

    @Size(max = 500, message = "O tratamento deve ter no maximo 500 caracteres")
    private String treatment;

    @DecimalMin(value = "0.1", message = "O peso minimo e 0,1 kg")
    @DecimalMax(value = "300.0", message = "O peso maximo e 300 kg")
    @Digits(integer = 3, fraction = 2, message = "Use no maximo duas casas decimais")
    private BigDecimal weight;

    @Size(max = 1000, message = "As observacoes devem ter no maximo 1000 caracteres")
    private String observations;
}
