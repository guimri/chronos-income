package com.chronosincome.repository;

import com.chronosincome.entity.Client;
import com.chronosincome.entity.Project;
import com.chronosincome.entity.Project.ProjectStatus;
import com.chronosincome.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByUser(User user);

    List<Project> findAllByUserAndStatus(User user, ProjectStatus status);

    List<Project> findAllByClientAndUser(Client client, User user);

    Optional<Project> findByIdAndUser(Long id, User user);

    // RN10 — nome único por usuário
    boolean existsByNameAndUser(String name, User user);

    // Para verificar duplicidade ao editar (ignora o próprio registro)
    boolean existsByNameAndUserAndIdNot(String name, User user, Long id);

    // Verifica se há projetos ativos vinculados ao cliente (usado no delete de cliente)
    boolean existsByClientAndUserAndStatus(Client client, User user, ProjectStatus status);
}
