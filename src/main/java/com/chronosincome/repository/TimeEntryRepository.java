package com.chronosincome.repository;

import com.chronosincome.entity.Project;
import com.chronosincome.entity.TimeEntry;
import com.chronosincome.entity.TimeEntry.TimerStatus;
import com.chronosincome.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    List<TimeEntry> findAllByUserOrderByEntryDateDescStartTimeDesc(User user);

    List<TimeEntry> findAllByUserAndEntryDateBetweenOrderByEntryDateDescStartTimeDesc(
            User user, LocalDate start, LocalDate end);

    List<TimeEntry> findAllByProjectAndUser(Project project, User user);

    Optional<TimeEntry> findByIdAndUser(Long id, User user);

    // Busca temporizador ativo ou pausado do usuário (RN16 — só um por vez)
    Optional<TimeEntry> findByUserAndTimerStatusIn(
            User user, List<TimerStatus> statuses);

    // RN15 — verifica sobreposição de horários na mesma data
    @Query("""
        SELECT COUNT(t) > 0 FROM TimeEntry t
        WHERE t.user = :user
        AND t.entryDate = :date
        AND t.endTime IS NOT NULL
        AND t.id <> :excludeId
        AND (
            (t.startTime < :endTime AND t.endTime > :startTime)
        )
    """)
    boolean existsOverlap(
            @Param("user") User user,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId);

    // Registros não faturados de um projeto (para gerar invoice)
    List<TimeEntry> findAllByProjectAndUserAndInvoicedFalseAndTimerStatus(
            Project project, User user, TimerStatus status);
}
