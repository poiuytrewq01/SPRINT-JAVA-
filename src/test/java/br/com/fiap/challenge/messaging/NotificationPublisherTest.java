package br.com.fiap.challenge.messaging;

import br.com.fiap.challenge.entity.Pet;
import br.com.fiap.challenge.entity.Reminder;
import br.com.fiap.challenge.entity.Tutor;
import br.com.fiap.challenge.entity.User;
import br.com.fiap.challenge.entity.VetLinkRequest;
import br.com.fiap.challenge.entity.Veterinarian;
import br.com.fiap.challenge.enums.LinkRequestStatus;
import br.com.fiap.challenge.enums.NotificationType;
import br.com.fiap.challenge.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Destinatario e texto de cada notificacao, testados sem banco e sem broker:
 * o publicador de eventos e o repositorio sao simulados com Mockito e o
 * evento publicado e capturado para conferencia.
 */
@ExtendWith(MockitoExtension.class)
class NotificationPublisherTest {

    @Mock
    private ApplicationEventPublisher events;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationPublisher publisher;

    private final Tutor tutor = Tutor.builder().id(10L).name("Maria").build();
    private final Veterinarian vet = Veterinarian.builder().id(20L).name("Dra. Ana").build();
    private final User tutorUser = User.builder().id(7L).name("Maria").tutor(tutor).build();
    private final Pet mia = Pet.builder().id(5L).name("Mia").tutor(tutor).veterinarian(vet).build();

    private VetLinkRequest request(LinkRequestStatus status, String note) {
        return VetLinkRequest.builder()
                .pet(mia)
                .veterinarian(vet)
                .requestedBy(tutorUser)
                .status(status)
                .responseNote(note)
                .build();
    }

    private NotificationEvent eventoPublicado() {
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(events).publishEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    void solicitacaoDeVinculoVaiParaOUsuarioDoVeterinario() {
        User vetUser = User.builder().id(3L).veterinarian(vet).build();
        when(userRepository.findByVeterinarianId(20L)).thenReturn(Optional.of(vetUser));

        publisher.linkRequested(request(LinkRequestStatus.PENDING, null));

        NotificationEvent evento = eventoPublicado();
        assertThat(evento.type()).isEqualTo(NotificationType.LINK_REQUESTED);
        assertThat(evento.recipientUserId()).isEqualTo(3L);
        assertThat(evento.link()).isEqualTo("/vet/solicitacoes");
        assertThat(evento.message()).isEqualTo("Maria pediu vínculo de Mia com você.");
    }

    @Test
    void veterinarioSemContaNaoRecebeNotificacao() {
        when(userRepository.findByVeterinarianId(20L)).thenReturn(Optional.empty());

        publisher.linkRequested(request(LinkRequestStatus.PENDING, null));

        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void vinculoAprovadoNotificaQuemPediu() {
        publisher.linkDecided(request(LinkRequestStatus.APPROVED, null));

        NotificationEvent evento = eventoPublicado();
        assertThat(evento.type()).isEqualTo(NotificationType.LINK_APPROVED);
        assertThat(evento.recipientUserId()).isEqualTo(7L);
        assertThat(evento.link()).isEqualTo("/tutor/vinculos");
        assertThat(evento.message()).contains("Dra. Ana aprovou o vínculo de Mia");
        // O destinatario ja esta na solicitacao: nenhuma consulta extra.
        verifyNoInteractions(userRepository);
    }

    @Test
    void vinculoRecusadoComObservacaoMostraOMotivo() {
        publisher.linkDecided(request(LinkRequestStatus.REJECTED, "Agenda cheia este mês"));

        NotificationEvent evento = eventoPublicado();
        assertThat(evento.type()).isEqualTo(NotificationType.LINK_REJECTED);
        assertThat(evento.recipientUserId()).isEqualTo(7L);
        assertThat(evento.message())
                .contains("recusou o vínculo de Mia")
                .contains("Motivo: Agenda cheia este mês");
    }

    @Test
    void vinculoRecusadoSemObservacaoNaoMostraMotivo() {
        publisher.linkDecided(request(LinkRequestStatus.REJECTED, "  "));

        assertThat(eventoPublicado().message()).doesNotContain("Motivo:");
    }

    /** A coluna message comporta 600 caracteres (V5): a recusa com observacao no limite precisa caber. */
    @Test
    void recusaComObservacaoNoLimiteCabeNaColunaDaNotificacao() {
        publisher.linkDecided(request(LinkRequestStatus.REJECTED, "x".repeat(300)));

        assertThat(eventoPublicado().message()).hasSizeLessThanOrEqualTo(600);
    }

    @Test
    void retornoAgendadoVaiParaOTutorComALinkDoPetEDataBrasileira() {
        when(userRepository.findByTutorId(10L)).thenReturn(Optional.of(tutorUser));
        Reminder retorno = Reminder.builder().dueDate(LocalDate.of(2026, 12, 5)).build();

        publisher.returnScheduled(mia, retorno);

        NotificationEvent evento = eventoPublicado();
        assertThat(evento.type()).isEqualTo(NotificationType.RETURN_SCHEDULED);
        assertThat(evento.recipientUserId()).isEqualTo(7L);
        assertThat(evento.link()).isEqualTo("/tutor/pets/5");
        assertThat(evento.message()).isEqualTo("Retorno de Mia agendado para 05/12/2026.");
    }
}
