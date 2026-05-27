package com.chronosincome.service;

import com.chronosincome.dto.request.InvoiceRequest;
import com.chronosincome.dto.request.InvoiceStatusRequest;
import com.chronosincome.dto.response.InvoiceResponse;
import com.chronosincome.dto.response.TimeEntrySummary;
import com.chronosincome.entity.*;
import com.chronosincome.entity.Invoice.InvoiceStatus;
import com.chronosincome.entity.TimeEntry.TimerStatus;
import com.chronosincome.exception.BusinessException;
import com.chronosincome.exception.ResourceNotFoundException;
import com.chronosincome.repository.*;
import com.chronosincome.util.AuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ProjectRepository projectRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final AuthHelper authHelper;

    // -------------------------------------------------------
    // GERAÇÃO DO INVOICE
    // -------------------------------------------------------

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        User user = authHelper.getLoggedUser();
        Project project = getProjectOrThrow(request.getProjectId(), user);

        // Período válido
        if (!request.getPeriodEnd().isAfter(request.getPeriodStart())) {
            throw new BusinessException(
                    "A data de fim deve ser posterior à data de início do período");
        }

        // Período não pode ser futuro
        if (request.getPeriodStart().isAfter(LocalDate.now())) {
            throw new BusinessException(
                    "Não é possível gerar fatura para um período futuro");
        }

        // RN30 — verifica sobreposição de período para o mesmo projeto
        if (invoiceRepository.existsOverlappingInvoice(
                user, project, request.getPeriodStart(), request.getPeriodEnd())) {
            throw new BusinessException(
                    "Já existe uma fatura para este projeto no período informado");
        }

        // Busca TimeEntries finalizados e não faturados do projeto no período
        List<TimeEntry> entries = timeEntryRepository
                .findAllByProjectAndUserAndInvoicedFalseAndTimerStatus(
                        project, user, TimerStatus.STOPPED)
                .stream()
                .filter(e -> !e.getEntryDate().isBefore(request.getPeriodStart())
                        && !e.getEntryDate().isAfter(request.getPeriodEnd()))
                .toList();

        if (entries.isEmpty()) {
            throw new BusinessException(
                    "Nenhum registro de horas encontrado para este projeto no período informado");
        }

        // RN30 — totaliza horas e calcula valor
        long totalSeconds = entries.stream()
                .mapToLong(TimeEntry::getDurationSeconds)
                .sum();

        BigDecimal totalHours = BigDecimal.valueOf(totalSeconds)
                .divide(BigDecimal.valueOf(3600), 10, RoundingMode.HALF_UP);

        BigDecimal totalAmount = totalHours
                .multiply(project.getHourlyRate())
                .setScale(2, RoundingMode.HALF_UP);

        // Gera número sequencial do invoice — ex: INV-2025-001
        String invoiceNumber = generateInvoiceNumber(user);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .totalSeconds(totalSeconds)
                .hourlyRate(project.getHourlyRate())
                .totalAmount(totalAmount)
                .status(InvoiceStatus.PENDING)
                .client(project.getClient())
                .project(project)
                .user(user)
                .timeEntries(entries)
                .build();

        Invoice saved = invoiceRepository.save(invoice);

        // Marca os TimeEntries como faturados
        entries.forEach(e -> e.setInvoiced(true));
        timeEntryRepository.saveAll(entries);

        return toResponse(saved);
    }

    // -------------------------------------------------------
    // CONSULTAS
    // -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAll() {
        User user = authHelper.getLoggedUser();
        return invoiceRepository.findAllByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAllByStatus(InvoiceStatus status) {
        User user = authHelper.getLoggedUser();
        return invoiceRepository.findAllByUserAndStatusOrderByCreatedAtDesc(user, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAllByClient(Long clientId) {
        User user = authHelper.getLoggedUser();
        Client client = getClientOrThrow(clientId, user);
        return invoiceRepository.findAllByClientAndUserOrderByCreatedAtDesc(client, user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAllByProject(Long projectId) {
        User user = authHelper.getLoggedUser();
        Project project = getProjectOrThrow(projectId, user);
        return invoiceRepository.findAllByProjectAndUserOrderByCreatedAtDesc(project, user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(Long id) {
        User user = authHelper.getLoggedUser();
        return toResponse(getInvoiceOrThrow(id, user));
    }

    // -------------------------------------------------------
    // ATUALIZAÇÃO DE STATUS
    // -------------------------------------------------------

    @Transactional
    public InvoiceResponse updateStatus(Long id, InvoiceStatusRequest request) {
        User user = authHelper.getLoggedUser();
        Invoice invoice = getInvoiceOrThrow(id, user);

        // Invoice cancelado não pode mudar de status
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessException(
                    "Faturas canceladas não podem ter o status alterado");
        }

        // Invoice pago não pode voltar para pendente
        if (invoice.getStatus() == InvoiceStatus.PAID
                && request.getStatus() == InvoiceStatus.PENDING) {
            throw new BusinessException(
                    "Faturas pagas não podem voltar ao status pendente");
        }

        invoice.setStatus(request.getStatus());

        // Se cancelado, libera os TimeEntries para nova faturação
        if (request.getStatus() == InvoiceStatus.CANCELLED) {
            invoice.getTimeEntries().forEach(e -> e.setInvoiced(false));
            timeEntryRepository.saveAll(invoice.getTimeEntries());
        }

        return toResponse(invoiceRepository.save(invoice));
    }

    // -------------------------------------------------------
    // EXCLUSÃO
    // -------------------------------------------------------

    @Transactional
    public void delete(Long id) {
        User user = authHelper.getLoggedUser();
        Invoice invoice = getInvoiceOrThrow(id, user);

        // Só permite excluir invoices cancelados
        if (invoice.getStatus() != InvoiceStatus.CANCELLED) {
            throw new BusinessException(
                    "Apenas faturas canceladas podem ser excluídas. " +
                            "Cancele a fatura antes de excluí-la");
        }

        invoiceRepository.delete(invoice);
    }

    // -------------------------------------------------------
    // MÉTODOS AUXILIARES PRIVADOS
    // -------------------------------------------------------

    private String generateInvoiceNumber(User user) {
        int year = LocalDate.now().getYear();
        long count = invoiceRepository.countByUserAndYear(user, year);
        return String.format("INV-%d-%03d", year, count + 1);
    }

    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private Invoice getInvoiceOrThrow(Long id, User user) {
        return invoiceRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fatura não encontrada"));
    }

    private Project getProjectOrThrow(Long projectId, User user) {
        return projectRepository.findByIdAndUser(projectId, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Projeto não encontrado"));
    }

    private Client getClientOrThrow(Long clientId, User user) {
        // Reaproveitamos o findByIdAndUser via relacionamento do projeto
        return invoiceRepository.findAllByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(Invoice::getClient)
                .filter(c -> c.getId().equals(clientId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente não encontrado"));
    }

    private TimeEntrySummary toEntrySummary(TimeEntry entry) {
        long totalSeconds = entry.getDurationSeconds() != null
                ? entry.getDurationSeconds() : 0L;
        return TimeEntrySummary.builder()
                .id(entry.getId())
                .entryDate(entry.getEntryDate())
                .startTime(entry.getStartTime())
                .endTime(entry.getEndTime())
                .duration(formatDuration(totalSeconds))
                .description(entry.getDescription())
                .build();
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        List<TimeEntrySummary> summaries = invoice.getTimeEntries() != null
                ? invoice.getTimeEntries().stream()
                .map(this::toEntrySummary)
                .toList()
                : List.of();

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .periodStart(invoice.getPeriodStart())
                .periodEnd(invoice.getPeriodEnd())
                .totalDuration(formatDuration(invoice.getTotalSeconds()))
                .hourlyRate(invoice.getHourlyRate())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .clientId(invoice.getClient().getId())
                .clientName(invoice.getClient().getName())
                .clientFiscalId(invoice.getClient().getFiscalId())
                .projectId(invoice.getProject().getId())
                .projectName(invoice.getProject().getName())
                .timeEntries(summaries)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }
}