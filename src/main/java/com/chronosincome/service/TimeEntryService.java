package com.chronosincome.service;

import com.chronosincome.dto.request.*;
import com.chronosincome.dto.response.TimeEntryResponse;
import com.chronosincome.entity.*;
import com.chronosincome.entity.TimeEntry.EntryType;
import com.chronosincome.entity.TimeEntry.TimerStatus;
import com.chronosincome.exception.BusinessException;
import com.chronosincome.exception.ResourceNotFoundException;
import com.chronosincome.repository.*;
import com.chronosincome.util.AuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final AuthHelper authHelper;

    // -------------------------------------------------------
    // ENTRADA MANUAL
    // -------------------------------------------------------

    @Transactional
    public TimeEntryResponse createManual(TimeEntryManualRequest request) {
        User user = authHelper.getLoggedUser();
        Project project = getProjectOrThrow(request.getProjectId(), user);

        // RN15 — fim deve ser depois do início
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(
                    "O horário de fim deve ser posterior ao horário de início");
        }

        // RN15 — verifica sobreposição com outros registros na mesma data
        validateNoOverlap(user, request.getEntryDate(),
                request.getStartTime(), request.getEndTime(), null);

        long durationSeconds = calculateDurationSeconds(
                request.getStartTime(), request.getEndTime());

        TimeEntry entry = TimeEntry.builder()
                .entryDate(request.getEntryDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .durationSeconds(durationSeconds)
                .type(EntryType.MANUAL)
                .timerStatus(TimerStatus.STOPPED)
                .description(request.getDescription())
                .invoiced(false)
                .project(project)
                .client(project.getClient())
                .user(user)
                .build();

        return toResponse(timeEntryRepository.save(entry));
    }

    // -------------------------------------------------------
    // TEMPORIZADOR
    // -------------------------------------------------------

    @Transactional
    public TimeEntryResponse startTimer(TimeEntryStartRequest request) {
        User user = authHelper.getLoggedUser();
        Project project = getProjectOrThrow(request.getProjectId(), user);

        // RN16 — só pode ter um temporizador ativo ou pausado por vez
        boolean hasActiveTimer = timeEntryRepository
                .findByUserAndTimerStatusIn(user,
                        List.of(TimerStatus.RUNNING, TimerStatus.PAUSED))
                .isPresent();

        if (hasActiveTimer) {
            throw new BusinessException(
                    "Já existe um temporizador em andamento. " +
                            "Finalize ou pause o atual antes de iniciar outro");
        }

        LocalDate entryDate = request.getEntryDate() != null
                ? request.getEntryDate()
                : LocalDate.now();

        LocalTime startTime = request.getStartTime() != null
                ? request.getStartTime()
                : LocalTime.now();

        // Quando startPaused=true (ex: adicionado pelo calendário), o entry
        // nasce pausado com 0 s acumulados — o usuário inicia manualmente depois
        TimerStatus initialStatus = request.isStartPaused()
                ? TimerStatus.PAUSED
                : TimerStatus.RUNNING;

        TimeEntry entry = TimeEntry.builder()
                .entryDate(entryDate)
                .startTime(startTime)
                .type(EntryType.TIMER)
                .timerStatus(initialStatus)
                .description(request.getDescription())
                .accumulatedSeconds(0L)
                .invoiced(false)
                .project(project)
                .client(project.getClient())
                .user(user)
                .build();

        return toResponse(timeEntryRepository.save(entry));
    }

    @Transactional
    public TimeEntryResponse pauseTimer(Long id) {
        User user = authHelper.getLoggedUser();
        TimeEntry entry = getEntryOrThrow(id, user);

        if (entry.getTimerStatus() != TimerStatus.RUNNING) {
            throw new BusinessException("O temporizador não está em execução");
        }

        // Acumula o tempo rodado até agora
        long secondsSinceStart = calculateElapsedSeconds(entry);
        entry.setAccumulatedSeconds(entry.getAccumulatedSeconds() + secondsSinceStart);
        entry.setPausedAt(LocalDateTime.now());
        entry.setTimerStatus(TimerStatus.PAUSED);

        return toResponse(timeEntryRepository.save(entry));
    }

    @Transactional
    public TimeEntryResponse resumeTimer(Long id) {
        User user = authHelper.getLoggedUser();
        TimeEntry entry = getEntryOrThrow(id, user);

        if (entry.getTimerStatus() != TimerStatus.PAUSED) {
            throw new BusinessException("O temporizador não está pausado");
        }

        // Reseta pausedAt — o próximo cálculo parte de agora
        entry.setPausedAt(null);
        entry.setTimerStatus(TimerStatus.RUNNING);

        // Atualiza startTime para o momento atual (referência para calcular
        // o tempo desde o último resume)
        entry.setStartTime(LocalTime.now());

        return toResponse(timeEntryRepository.save(entry));
    }

    @Transactional
    public TimeEntryResponse stopTimer(Long id) {
        User user = authHelper.getLoggedUser();
        TimeEntry entry = getEntryOrThrow(id, user);

        if (entry.getTimerStatus() == TimerStatus.STOPPED) {
            throw new BusinessException("O temporizador já está finalizado");
        }

        LocalTime endTime = LocalTime.now();
        long totalSeconds = entry.getAccumulatedSeconds();

        // Se estava RUNNING, soma o tempo desde o último start/resume
        if (entry.getTimerStatus() == TimerStatus.RUNNING) {
            totalSeconds += calculateElapsedSeconds(entry);
        }

        // Duração mínima de 1 segundo
        if (totalSeconds < 1) {
            throw new BusinessException(
                    "O registro deve ter duração mínima de 1 segundo");
        }

        entry.setEndTime(endTime);
        entry.setDurationSeconds(totalSeconds);
        entry.setTimerStatus(TimerStatus.STOPPED);
        entry.setPausedAt(null);

        return toResponse(timeEntryRepository.save(entry));
    }

    // -------------------------------------------------------
    // CONSULTAS
    // -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TimeEntryResponse> findAll() {
        User user = authHelper.getLoggedUser();
        return timeEntryRepository
                .findAllByUserOrderByEntryDateDescStartTimeDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TimeEntryResponse> findByPeriod(LocalDate start, LocalDate end) {
        User user = authHelper.getLoggedUser();

        if (end.isBefore(start)) {
            throw new BusinessException(
                    "A data de fim deve ser posterior à data de início");
        }

        return timeEntryRepository
                .findAllByUserAndEntryDateBetweenOrderByEntryDateDescStartTimeDesc(
                        user, start, end)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TimeEntryResponse> findByProject(Long projectId) {
        User user = authHelper.getLoggedUser();
        Project project = getProjectOrThrow(projectId, user);
        return timeEntryRepository.findAllByProjectAndUser(project, user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TimeEntryResponse> findByClient(Long clientId) {
        User user = authHelper.getLoggedUser();
        Client client = clientRepository.findByIdAndUser(clientId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        return timeEntryRepository.findAllByClientAndUser(client, user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TimeEntryResponse findById(Long id) {
        User user = authHelper.getLoggedUser();
        return toResponse(getEntryOrThrow(id, user));
    }

    // Retorna o temporizador ativo ou pausado (para o React restaurar o estado)
    @Transactional(readOnly = true)
    public TimeEntryResponse findActiveTimer() {
        User user = authHelper.getLoggedUser();
        return timeEntryRepository
                .findByUserAndTimerStatusIn(user,
                        List.of(TimerStatus.RUNNING, TimerStatus.PAUSED))
                .map(this::toResponse)
                .orElse(null);
    }

    // -------------------------------------------------------
    // EDIÇÃO E EXCLUSÃO
    // -------------------------------------------------------

    @Transactional
    public TimeEntryResponse update(Long id, TimeEntryUpdateRequest request) {
        User user = authHelper.getLoggedUser();
        TimeEntry entry = getEntryOrThrow(id, user);

        // Não permite editar registro ainda em andamento
        if (entry.getTimerStatus() != TimerStatus.STOPPED) {
            throw new BusinessException(
                    "Não é possível editar um registro com temporizador em andamento");
        }

        // Não permite editar registro já faturado
        if (Boolean.TRUE.equals(entry.getInvoiced())) {
            throw new BusinessException(
                    "Não é possível editar um registro já faturado");
        }

        // RN15 — fim deve ser depois do início
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(
                    "O horário de fim deve ser posterior ao horário de início");
        }

        // RN15 — verifica sobreposição excluindo o próprio registro
        validateNoOverlap(user, request.getEntryDate(),
                request.getStartTime(), request.getEndTime(), id);

        Project project = getProjectOrThrow(request.getProjectId(), user);

        long durationSeconds = calculateDurationSeconds(
                request.getStartTime(), request.getEndTime());

        entry.setEntryDate(request.getEntryDate());
        entry.setStartTime(request.getStartTime());
        entry.setEndTime(request.getEndTime());
        entry.setDurationSeconds(durationSeconds);
        entry.setDescription(request.getDescription());
        entry.setProject(project);
        entry.setClient(project.getClient());

        return toResponse(timeEntryRepository.save(entry));
    }

    @Transactional
    public void delete(Long id) {
        User user = authHelper.getLoggedUser();
        TimeEntry entry = getEntryOrThrow(id, user);

        if (entry.getTimerStatus() != TimerStatus.STOPPED) {
            throw new BusinessException(
                    "Finalize o temporizador antes de excluir o registro");
        }

        if (Boolean.TRUE.equals(entry.getInvoiced())) {
            throw new BusinessException(
                    "Não é possível excluir um registro já faturado");
        }

        timeEntryRepository.delete(entry);
    }

    // -------------------------------------------------------
    // MÉTODOS AUXILIARES PRIVADOS
    // -------------------------------------------------------

    private void validateNoOverlap(User user, LocalDate date,
                                   LocalTime start, LocalTime end, Long excludeId) {

        long excludeIdValue = excludeId != null ? excludeId : -1L;

        boolean hasOverlap = timeEntryRepository.existsOverlap(
                user, date, start, end, excludeIdValue);

        if (hasOverlap) {
            throw new BusinessException(
                    "Este horário conflita com outro registro existente na mesma data");
        }
    }

    // Calcula segundos entre o startTime do entry e agora
    private long calculateElapsedSeconds(TimeEntry entry) {
        LocalTime now = LocalTime.now();
        return java.time.Duration.between(entry.getStartTime(), now).getSeconds();
    }

    private long calculateDurationSeconds(LocalTime start, LocalTime end) {
        return java.time.Duration.between(start, end).getSeconds();
    }

    private String formatDuration(Long totalSeconds) {
        if (totalSeconds == null || totalSeconds < 0) return "00:00:00";
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private Project getProjectOrThrow(Long projectId, User user) {
        return projectRepository.findByIdAndUser(projectId, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Projeto não encontrado"));
    }

    private TimeEntry getEntryOrThrow(Long id, User user) {
        return timeEntryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registro de horas não encontrado"));
    }

    private TimeEntryResponse toResponse(TimeEntry entry) {
        return TimeEntryResponse.builder()
                .id(entry.getId())
                .entryDate(entry.getEntryDate())
                .startTime(entry.getStartTime())
                .endTime(entry.getEndTime())
                .duration(formatDuration(entry.getDurationSeconds()))
                .type(entry.getType())
                .timerStatus(entry.getTimerStatus())
                .description(entry.getDescription())
                .invoiced(entry.getInvoiced())
                .projectId(entry.getProject().getId())
                .projectName(entry.getProject().getName())
                .projectColor(entry.getProject().getColor())
                .clientId(entry.getClient().getId())
                .clientName(entry.getClient().getName())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }
}