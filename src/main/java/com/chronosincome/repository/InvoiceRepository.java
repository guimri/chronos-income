package com.chronosincome.repository;

import com.chronosincome.entity.Client;
import com.chronosincome.entity.Invoice;
import com.chronosincome.entity.Invoice.InvoiceStatus;
import com.chronosincome.entity.Project;
import com.chronosincome.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findAllByUserOrderByCreatedAtDesc(User user);

    List<Invoice> findAllByUserAndStatusOrderByCreatedAtDesc(
            User user, InvoiceStatus status);

    List<Invoice> findAllByClientAndUserOrderByCreatedAtDesc(
            Client client, User user);

    List<Invoice> findAllByProjectAndUserOrderByCreatedAtDesc(
            Project project, User user);

    Optional<Invoice> findByIdAndUser(Long id, User user);

    // Filtro por período — invoices cujo período se sobreponha ao intervalo informado
    @Query("""
        SELECT i FROM Invoice i
        WHERE i.user = :user
        AND i.periodStart <= :periodEnd
        AND i.periodEnd >= :periodStart
        ORDER BY i.createdAt DESC
    """)
    List<Invoice> findAllByUserAndPeriod(
            @Param("user") User user,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);

    // Filtro por período + status
    @Query("""
        SELECT i FROM Invoice i
        WHERE i.user = :user
        AND i.status = :status
        AND i.periodStart <= :periodEnd
        AND i.periodEnd >= :periodStart
        ORDER BY i.createdAt DESC
    """)
    List<Invoice> findAllByUserAndStatusAndPeriod(
            @Param("user") User user,
            @Param("status") InvoiceStatus status,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);

    // Gera o próximo número sequencial do invoice — ex: INV-2025-001
    @Query("""
        SELECT COUNT(i) FROM Invoice i
        WHERE i.user = :user
        AND YEAR(i.createdAt) = :year
    """)
    long countByUserAndYear(@Param("user") User user, @Param("year") int year);

    // Verifica se já existe invoice para o mesmo projeto e período
    @Query("""
        SELECT COUNT(i) > 0 FROM Invoice i
        WHERE i.user = :user
        AND i.project = :project
        AND i.status <> 'CANCELLED'
        AND i.periodStart <= :periodEnd
        AND i.periodEnd >= :periodStart
    """)
    boolean existsOverlappingInvoice(
            @Param("user") User user,
            @Param("project") Project project,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);
}