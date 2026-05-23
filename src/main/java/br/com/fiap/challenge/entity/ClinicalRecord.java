package br.com.fiap.challenge.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Prontuário clínico do pet — registro de consultas, diagnósticos e tratamentos.
 *
 * Ao criar um prontuário, o ClinicalRecordService gera automaticamente um
 * Reminder de retorno para 6 meses após a data da consulta, incentivando
 * o acompanhamento preventivo contínuo (diferencial do produto).
 *
 * O peso no prontuário registra o peso na data da consulta, permitindo
 * análise da evolução do pet ao longo do tempo.
 */
@Entity
@Table(name = "clinical_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @PastOrPresent
    @Column(nullable = false)
    private LocalDate date;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String description;

    @Size(max = 300)
    private String diagnosis;

    @Size(max = 500)
    private String treatment;

    // Peso registrado na data da consulta (diferente do peso atual do pet)
    @DecimalMin(value = "0.1", message = "Peso mínimo 0.1 kg")
    @DecimalMax(value = "300.0", message = "Peso máximo 300 kg")
    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    @Size(max = 1000)
    private String observations;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Pet pet;

    // Veterinário responsável pelo prontuário (opcional para registros feitos pelo tutor)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinarian_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Veterinarian veterinarian;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
