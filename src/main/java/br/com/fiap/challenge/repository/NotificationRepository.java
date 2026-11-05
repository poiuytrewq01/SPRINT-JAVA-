package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Consulta de idempotencia do consumidor: esta mensagem ja foi processada? */
    boolean existsByEventId(UUID eventId);

    List<Notification> findTop30ByUserIdOrderByOccurredAtDesc(Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);

    /**
     * Marca todas como lidas em um unico UPDATE, sem carregar as entidades.
     * clearAutomatically limpa o contexto de persistencia para que nenhuma
     * entidade em memoria fique com o valor antigo de readAt.
     */
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.readAt = :now where n.user.id = :userId and n.readAt is null")
    int markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
