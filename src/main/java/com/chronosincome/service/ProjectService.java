package com.chronosincome.service;

import com.chronosincome.dto.request.ProjectRequest;
import com.chronosincome.dto.request.ProjectStatusRequest;
import com.chronosincome.dto.response.ProjectResponse;
import com.chronosincome.entity.Client;
import com.chronosincome.entity.Project;
import com.chronosincome.entity.Project.ProjectStatus;
import com.chronosincome.entity.User;
import com.chronosincome.exception.BusinessException;
import com.chronosincome.exception.ResourceNotFoundException;
import com.chronosincome.repository.ClientRepository;
import com.chronosincome.repository.ProjectRepository;
import com.chronosincome.util.AuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final AuthHelper authHelper;

    public ProjectResponse create(ProjectRequest request) {
        User user = authHelper.getLoggedUser();

        // RN10 — nome único por usuário
        if (projectRepository.existsByNameAndUser(request.getName(), user)) {
            throw new BusinessException("Já existe um projeto com este nome");
        }

        Client client = getClientOrThrow(request.getClientId(), user);

        Project project = Project.builder()
                .name(request.getName())
                .color(request.getColor())
                .hourlyRate(request.getHourlyRate())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .status(ProjectStatus.ACTIVE)
                .client(client)
                .user(user)
                .build();

        return toResponse(projectRepository.save(project));
    }

    public List<ProjectResponse> findAll() {
        User user = authHelper.getLoggedUser();
        return projectRepository.findAllByUser(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProjectResponse> findAllByStatus(ProjectStatus status) {
        User user = authHelper.getLoggedUser();
        return projectRepository.findAllByUserAndStatus(user, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProjectResponse> findAllByClient(Long clientId) {
        User user = authHelper.getLoggedUser();
        Client client = getClientOrThrow(clientId, user);
        return projectRepository.findAllByClientAndUser(client, user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse findById(Long id) {
        User user = authHelper.getLoggedUser();
        return toResponse(getProjectOrThrow(id, user));
    }

    public ProjectResponse update(Long id, ProjectRequest request) {
        User user = authHelper.getLoggedUser();
        Project project = getProjectOrThrow(id, user);

        // RN10 — verifica duplicidade de nome ignorando o próprio registro
        if (projectRepository.existsByNameAndUserAndIdNot(request.getName(), user, id)) {
            throw new BusinessException("Já existe um projeto com este nome");
        }

        // RN12 — projeto COMPLETED não pode ser editado
        if (project.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException("Projetos concluídos não podem ser editados");
        }

        Client client = getClientOrThrow(request.getClientId(), user);

        project.setName(request.getName());
        project.setColor(request.getColor());
        project.setHourlyRate(request.getHourlyRate());
        project.setDescription(request.getDescription());
        project.setStartDate(request.getStartDate());
        project.setClient(client);

        return toResponse(projectRepository.save(project));
    }

    public ProjectResponse updateStatus(Long id, ProjectStatusRequest request) {
        User user = authHelper.getLoggedUser();
        Project project = getProjectOrThrow(id, user);

        // RN12 — projeto COMPLETED não pode voltar a outro status
        if (project.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException("Projetos concluídos não podem ter o status alterado");
        }

        project.setStatus(request.getStatus());
        return toResponse(projectRepository.save(project));
    }

    public void delete(Long id) {
        User user = authHelper.getLoggedUser();
        Project project = getProjectOrThrow(id, user);

        // Apenas projetos INACTIVE podem ser excluídos
        if (project.getStatus() != ProjectStatus.INACTIVE) {
            throw new BusinessException(
                    "Apenas projetos inativos podem ser excluídos. " +
                            "Altere o status do projeto antes de excluí-lo");
        }

        projectRepository.delete(project);
    }

    // Métodos privados auxiliares

    private Project getProjectOrThrow(Long id, User user) {
        return projectRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Projeto não encontrado"));
    }

    private Client getClientOrThrow(Long clientId, User user) {
        return clientRepository.findByIdAndUser(clientId, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente não encontrado"));
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .color(project.getColor())
                .hourlyRate(project.getHourlyRate())
                .description(project.getDescription())
                .startDate(project.getStartDate())
                .status(project.getStatus())
                .clientId(project.getClient().getId())
                .clientName(project.getClient().getName())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
