package br.com.fiap.challenge.messaging;

import br.com.fiap.challenge.entity.Pet;
import br.com.fiap.challenge.entity.Reminder;
import br.com.fiap.challenge.entity.User;
import br.com.fiap.challenge.entity.VetLinkRequest;
import br.com.fiap.challenge.enums.LinkRequestStatus;
import br.com.fiap.challenge.enums.NotificationType;
import br.com.fiap.challenge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Traduz acontecimentos de negocio em notificacoes.
 *
 * Os services chamam metodos com nome de negocio (linkRequested,
 * returnScheduled) e nao sabem nada de RabbitMQ. Esta classe decide quem
 * recebe e qual e o texto, e entrega o evento ao mecanismo de eventos do
 * Spring. Quem leva o evento ate o broker e o NotificationRelay, somente
 * depois do commit da transacao.
 *
 * Essa separacao tem um motivo pratico: se o service publicasse direto no
 * RabbitMQ, uma transacao que falhasse depois do envio deixaria no ar uma
 * notificacao sobre algo que nunca foi gravado -- "seu vinculo foi aprovado"
 * para uma aprovacao que sofreu rollback.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ApplicationEventPublisher events;
    private final UserRepository userRepository;

    /** Tutor pediu vinculo: avisa o veterinario que tem algo na fila dele. */
    public void linkRequested(VetLinkRequest request) {
        userRepository.findByVeterinarianId(request.getVeterinarian().getId())
                .ifPresentOrElse(
                        vetUser -> publish(NotificationEvent.of(
                                NotificationType.LINK_REQUESTED,
                                vetUser.getId(),
                                request.getRequestedBy().getName() + " pediu vínculo de "
                                        + request.getPet().getName() + " com você.",
                                "/vet/solicitacoes")),
                        () -> log.warn("Veterinario {} sem conta de acesso; notificacao nao enviada",
                                request.getVeterinarian().getId()));
    }

    /**
     * Veterinario decidiu: avisa quem abriu a solicitacao. O destinatario ja
     * esta na propria solicitacao (requestedBy), sem consulta extra.
     */
    public void linkDecided(VetLinkRequest request) {
        boolean approved = request.getStatus() == LinkRequestStatus.APPROVED;
        String vetName = request.getVeterinarian().getName();
        String petName = request.getPet().getName();

        String message = approved
                ? vetName + " aprovou o vínculo de " + petName + ". O prontuário já está disponível para ele."
                : vetName + " recusou o vínculo de " + petName + "."
                        + (request.getResponseNote() != null && !request.getResponseNote().isBlank()
                                ? " Motivo: " + request.getResponseNote() : "");

        publish(NotificationEvent.of(
                approved ? NotificationType.LINK_APPROVED : NotificationType.LINK_REJECTED,
                request.getRequestedBy().getId(),
                message,
                "/tutor/vinculos"));
    }

    /** Atendimento gerou retorno automatico: avisa o tutor da data. */
    public void returnScheduled(Pet pet, Reminder reminder) {
        userRepository.findByTutorId(pet.getTutor().getId())
                .map(User::getId)
                .ifPresent(tutorUserId -> publish(NotificationEvent.of(
                        NotificationType.RETURN_SCHEDULED,
                        tutorUserId,
                        "Retorno de " + pet.getName() + " agendado para "
                                + reminder.getDueDate().format(DATA_BR) + ".",
                        "/tutor/pets/" + pet.getId())));
    }

    private void publish(NotificationEvent event) {
        events.publishEvent(event);
    }
}
