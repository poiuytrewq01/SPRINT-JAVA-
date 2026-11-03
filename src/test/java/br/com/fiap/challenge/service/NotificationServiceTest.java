package br.com.fiap.challenge.service;

import br.com.fiap.challenge.entity.Notification;
import br.com.fiap.challenge.entity.User;
import br.com.fiap.challenge.enums.NotificationType;
import br.com.fiap.challenge.messaging.NotificationEvent;
import br.com.fiap.challenge.repository.NotificationRepository;
import br.com.fiap.challenge.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Regras do consumidor de notificacoes, testadas sem banco e sem broker:
 * os repositorios sao simulados com Mockito.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService service;

    private final NotificationEvent event = NotificationEvent.of(
            NotificationType.LINK_APPROVED, 7L, "Dra. Ana aprovou o vínculo de Mia.", "/tutor/vinculos");

    @Test
    void gravaNotificacaoNova() {
        User maria = User.builder().id(7L).build();
        when(notificationRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(maria));

        assertThat(service.store(event)).isTrue();

        ArgumentCaptor<Notification> gravada = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(gravada.capture());
        assertThat(gravada.getValue().getEventId()).isEqualTo(event.eventId());
        assertThat(gravada.getValue().getUser()).isSameAs(maria);
        assertThat(gravada.getValue().isUnread()).isTrue();
    }

    @Test
    void ignoraReentregaDaMesmaMensagem() {
        when(notificationRepository.existsByEventId(event.eventId())).thenReturn(true);

        assertThat(service.store(event)).isFalse();

        verify(notificationRepository, never()).save(any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void destinatarioInexistenteVaiParaDlqSemReenfileirar() {
        when(notificationRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.store(event))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void naoAbreNotificacaoDeOutroUsuario() {
        Notification daMaria = Notification.builder()
                .id(1L).user(User.builder().id(7L).build()).link("/tutor/vinculos").build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(daMaria));

        assertThatThrownBy(() -> service.open(1L, 99L)).isInstanceOf(AccessDeniedException.class);
        assertThat(daMaria.isUnread()).isTrue();
    }

    @Test
    void abrirMarcaComoLidaEDevolveODestino() {
        Notification daMaria = Notification.builder()
                .id(1L).user(User.builder().id(7L).build()).link("/tutor/vinculos").build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(daMaria));

        assertThat(service.open(1L, 7L)).isEqualTo("/tutor/vinculos");
        assertThat(daMaria.isUnread()).isFalse();
    }
}
