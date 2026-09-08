package br.com.fiap.challenge.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Estados de uma solicitacao de vinculo entre pet e veterinario.
 *
 * As transicoes validas ficam declaradas no proprio enum, e nao espalhadas
 * em condicionais dentro do service. Assim existe um unico lugar para
 * consultar (ou alterar) o ciclo de vida, e o service apenas pergunta se a
 * transicao e permitida.
 *
 *   PENDING --aprovar--> APPROVED   (decisao do veterinario)
 *   PENDING --recusar--> REJECTED   (decisao do veterinario)
 *   PENDING --cancelar-> CANCELLED  (desistencia do tutor)
 *
 * APPROVED, REJECTED e CANCELLED sao finais: a solicitacao vira historico.
 */
@Getter
@RequiredArgsConstructor
public enum LinkRequestStatus {

    PENDING("Pendente"),
    APPROVED("Aprovada"),
    REJECTED("Recusada"),
    CANCELLED("Cancelada");

    private final String label;

    /**
     * Estados alcancaveis a partir deste. Declarado como metodo, e nao como
     * campo, porque um enum nao pode referenciar suas proprias constantes
     * durante a construcao.
     */
    public Set<LinkRequestStatus> allowedTransitions() {
        return this == PENDING
                ? Set.of(APPROVED, REJECTED, CANCELLED)
                : Set.of();
    }

    public boolean canTransitionTo(LinkRequestStatus target) {
        return allowedTransitions().contains(target);
    }

    public boolean isFinal() {
        return allowedTransitions().isEmpty();
    }
}
