package br.com.fiap.challenge.dto.response;

import br.com.fiap.challenge.enums.PetLevel;
import br.com.fiap.challenge.enums.Species;

/**
 * Retrato consolidado da saude e do engajamento de um pet.
 *
 * Agrega numeros que vivem em tabelas diferentes -- vacinas, prontuarios,
 * lembretes e streak -- em uma unica resposta, para que a tela nao precise de
 * varias chamadas para montar o painel. Todos os campos sao calculados pelo
 * PetHealthService a cada consulta, e nenhum e persistido: um score guardado
 * no banco ficaria desatualizado assim que uma vacina vencesse.
 *
 * Um record por ser um transporte de dados imutavel, sem comportamento proprio.
 */
public record PetHealthSummaryResponse(

        Long petId,
        String petName,
        Species species,

        /** Score de 0 a 100: vinculo com veterinario, vacinas, consulta recente e streak. */
        int healthScore,
        String healthMessage,

        /** Percentual de campos preenchidos no perfil do pet. */
        int profileCompletion,
        String profileMessage,

        int currentStreak,
        int longestStreak,
        PetLevel level,
        String levelLabel,

        int totalVaccines,
        int overdueVaccines,

        int totalRecords,
        boolean hasRecentCheckup,

        int pendingReminders
) {
    /** Usado pela view para destacar o que precisa de atencao. */
    public boolean needsAttention() {
        return overdueVaccines > 0 || !hasRecentCheckup;
    }
}
