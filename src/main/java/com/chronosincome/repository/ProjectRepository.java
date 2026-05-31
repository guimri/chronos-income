package com.chronosincome.repository;

import com.chronosincome.entity.Client;
import com.chronosincome.entity.Project;
import com.chronosincome.entity.Project.ProjectStatus;
import com.chronosincome.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByUser(User user);

    List<Project> findAllByUserAndStatus(User user, ProjectStatus status);

    List<Project> findAllByClientAndUser(Client client, User user);

    Optional<Project> findByIdAndUser(Long id, User user);

    boolean existsByNameAndUser(String name, User user);

    boolean existsByNameAndUserAndIdNot(String name, User user, Long id);

    boolean existsByClientAndUserAndStatus(Client client, User user, ProjectStatus status);

    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeEntry t WHERE t.project = :project")
    BigDecimal sumHoursByProject(@Param("project") Project project);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.client = :client")
    Integer countByClient(@Param("client") Client client);
}