package com.chronosincome.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "time_entries")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    // RN15 – formato HH:MM:SS, início obrigatório
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    // Pode ser nulo enquanto o temporizador está rodando
    @Column(name = "end_time")
    private LocalTime endTime;

    // Duração total em segundos (calculada ao parar — considera pausas)
    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EntryType type = EntryType.TIMER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TimerStatus timerStatus = TimerStatus.STOPPED;

    // Momento exato da última pausa — para calcular tempo acumulado (RN21)
    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    // Tempo já acumulado antes da pausa atual (em segundos)
    @Column(name = "accumulated_seconds")
    @Builder.Default
    private Long accumulatedSeconds = 0L;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Indica se esse registro já foi incluído em algum invoice
    @Column(name = "invoiced")
    @Builder.Default
    private Boolean invoiced = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum EntryType {
        TIMER,  // via temporizador
        MANUAL  // entrada manual
    }

    public enum TimerStatus {
        RUNNING,  // temporizador ativo
        PAUSED,   // pausado
        STOPPED   // finalizado
    }
}
