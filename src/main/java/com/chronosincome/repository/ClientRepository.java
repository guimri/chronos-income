package com.chronosincome.repository;

import com.chronosincome.entity.Client;
import com.chronosincome.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findAllByUser(User user);

    Optional<Client> findByIdAndUser(Long id, User user);

    boolean existsByFiscalIdAndUser(String fiscalId, User user);

    boolean existsByIdAndUser(Long id, User user);

    // Verifica se existe algum projeto ativo vinculado ao cliente
    boolean existsByIdAndProjectsIsNotEmpty(Long id);
}
