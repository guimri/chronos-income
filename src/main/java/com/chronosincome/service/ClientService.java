package com.chronosincome.service;

import com.chronosincome.dto.request.ClientRequest;
import com.chronosincome.dto.response.ClientResponse;
import com.chronosincome.entity.Client;
import com.chronosincome.entity.User;
import com.chronosincome.exception.BusinessException;
import com.chronosincome.exception.ResourceNotFoundException;
import com.chronosincome.repository.ClientRepository;
import com.chronosincome.repository.ProjectRepository;
import com.chronosincome.util.AuthHelper;
import com.chronosincome.util.FiscalIdValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ProjectRepository projectRepository;
    private final AuthHelper authHelper;

    public ClientResponse create(ClientRequest request) {
        User user = authHelper.getLoggedUser();

        // RN08 — valida CNPJ ou EIN
        FiscalIdValidator.validate(request.getFiscalId());

        // Fiscal ID único por usuário
        if (clientRepository.existsByFiscalIdAndUser(request.getFiscalId(), user)) {
            throw new BusinessException("Já existe um cliente com este CNPJ/EIN");
        }

        Client client = Client.builder()
                .name(request.getName())
                .fiscalId(request.getFiscalId())
                .description(request.getDescription())
                .user(user)
                .build();

        return toResponse(clientRepository.save(client));
    }

    public List<ClientResponse> findAll() {
        User user = authHelper.getLoggedUser();
        return clientRepository.findAllByUser(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ClientResponse findById(Long id) {
        User user = authHelper.getLoggedUser();
        return toResponse(getClientOrThrow(id, user));
    }

    public ClientResponse update(Long id, ClientRequest request) {
        User user = authHelper.getLoggedUser();
        Client client = getClientOrThrow(id, user);

        // RN08 — valida novo fiscal ID
        FiscalIdValidator.validate(request.getFiscalId());

        // Verifica duplicidade apenas se o fiscal ID foi alterado
        boolean fiscalIdChanged = !client.getFiscalId().equals(request.getFiscalId());
        if (fiscalIdChanged && clientRepository.existsByFiscalIdAndUser(request.getFiscalId(), user)) {
            throw new BusinessException("Já existe um cliente com este CNPJ/EIN");
        }

        client.setName(request.getName());
        client.setFiscalId(request.getFiscalId());
        client.setDescription(request.getDescription());

        return toResponse(clientRepository.save(client));
    }

    public void delete(Long id) {
        User user = authHelper.getLoggedUser();
        Client client = getClientOrThrow(id, user);

        // RN07 — impede exclusão se houver projetos vinculados
        if (clientRepository.existsByIdAndProjectsIsNotEmpty(id)) {
            throw new BusinessException(
                    "Não é possível excluir um cliente com projetos vinculados");
        }

        clientRepository.delete(client);
    }

    // Busca o cliente garantindo que pertence ao usuário logado
    private Client getClientOrThrow(Long id, User user) {
        return clientRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente não encontrado"));
    }

    private ClientResponse toResponse(Client client) {
        return ClientResponse.builder()
                .id(client.getId())
                .name(client.getName())
                .fiscalId(client.getFiscalId())
                .description(client.getDescription())
                .projectCount(projectRepository.countByClient(client))
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
    }
}
