package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.entity.Reminder;
import br.com.fiap.challenge.enums.ReminderStatus;
import br.com.fiap.challenge.enums.ReminderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositório JPA para Reminder.
 * A query "findUpcoming" é usada para a funcionalidade de alertas proativos:
 * retorna apenas lembretes PENDING dentro de uma janela de datas,
 * permitindo exibir "o que precisa de atenção nos próximos 30 dias".
 */
@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    Page<Reminder> findByPetIdOrderByDueDateAsc(Long petId, Pageable pageable);

    /**
     * Retorna lembretes PENDING dentro de uma janela de datas.
     * Filtra apenas os pendentes para não poluir a tela com lembretes já concluídos.
     */
    @Query("SELECT r FROM Reminder r WHERE r.pet.id = :petId AND " +
           "r.dueDate BETWEEN :today AND :until AND r.status = 'PENDING' ORDER BY r.dueDate ASC")
    List<Reminder> findUpcoming(@Param("petId") Long petId,
                                @Param("today") LocalDate today,
                                @Param("until") LocalDate until);

    /**
     * Filtro dinâmico por status e tipo — parâmetros opcionais via IS NULL.
     * Permite listar lembretes de vacina pendentes, retornos concluídos etc.
     */
    @Query("SELECT r FROM Reminder r WHERE r.pet.id = :petId AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:type IS NULL OR r.type = :type)")
    Page<Reminder> findByPetIdAndFilters(@Param("petId") Long petId,
                                         @Param("status") ReminderStatus status,
                                         @Param("type") ReminderType type,
                                         Pageable pageable);

    /** Conta lembretes pendentes — usado no health score sem carregar as entidades. */
    @Query("SELECT COUNT(r) FROM Reminder r WHERE r.pet.id = :petId AND r.status = 'PENDING'")
    long countPendingByPetId(@Param("petId") Long petId);
}
