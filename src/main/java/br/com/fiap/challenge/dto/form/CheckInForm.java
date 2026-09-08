package br.com.fiap.challenge.dto.form;

import br.com.fiap.challenge.enums.ActivityType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Dados do formulario de check-in diario.
 *
 * As classes de formulario sao separadas das entidades de proposito: o
 * @ModelAttribute do Spring popula o objeto com o que veio da requisicao antes
 * de validar. Se o alvo fosse a entidade, um campo extra enviado na requisicao
 * poderia sobrescrever um atributo que o formulario nem exibe -- por exemplo o
 * pet dono do check-in.
 *
 * Sao classes com getter e setter, e nao records, porque o th:field precisa ler
 * o valor atual para reexibir o formulario preenchido quando a validacao falha.
 */
@Getter
@Setter
public class CheckInForm {

    @NotNull(message = "Escolha o tipo de atividade")
    private ActivityType activityType;

    @NotNull(message = "Informe a duracao")
    @Min(value = 1, message = "A duracao minima e de 1 minuto")
    @Max(value = 1440, message = "A duracao maxima e de 1440 minutos (24 horas)")
    private Integer durationMinutes;

    @Size(max = 500, message = "A observacao deve ter no maximo 500 caracteres")
    private String notes;
}
