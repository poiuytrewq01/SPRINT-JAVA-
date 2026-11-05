package br.com.fiap.challenge.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipos de notificacao que trafegam pelo RabbitMQ.
 *
 * Cada constante corresponde a um acontecimento de negocio que interessa a
 * outra pessoa alem de quem o provocou: o tutor pede, o veterinario e avisado;
 * o veterinario decide, o tutor e avisado.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationType {

    LINK_REQUESTED("Nova solicitação de vínculo"),
    LINK_APPROVED("Vínculo aprovado"),
    LINK_REJECTED("Vínculo recusado"),
    RETURN_SCHEDULED("Retorno agendado");

    private final String label;
}
