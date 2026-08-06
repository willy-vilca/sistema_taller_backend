package com.tallermecanico.api.client;

import com.tallermecanico.api.common.BusinessException;
import com.tallermecanico.api.common.PageResponse;
import com.tallermecanico.api.common.SearchNormalizer;
import com.tallermecanico.api.vehicle.VehicleMapper;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class ClientService {
    private final ClientRepository clientRepository;
    private final EntityManager entityManager;

    public ClientService(ClientRepository clientRepository, EntityManager entityManager) {
        this.clientRepository = clientRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientSummaryResponse> search(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());
        return PageResponse.from(
                clientRepository.search(SearchNormalizer.text(search), SearchNormalizer.plate(search), pageable),
                this::toSummary
        );
    }

    @Transactional(readOnly = true)
    public ClientDetailResponse findById(UUID id) {
        Client client = getEntity(id);
        return new ClientDetailResponse(
                client.getId(),
                client.getFullName(),
                client.getDni(),
                client.getPhone(),
                client.getEmail(),
                client.getVehicles().stream().map(VehicleMapper::toResponse).toList()
        );
    }

    public ClientDetailResponse create(ClientRequest request) {
        if (clientRepository.existsByDni(request.dni().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Ya existe un cliente con ese DNI.");
        }
        Client client = clientRepository.save(new Client(
                normalizeName(request.fullName()),
                request.dni().trim(),
                nullableTrim(request.phone()),
                normalizeEmail(request.email())
        ));
        return new ClientDetailResponse(client.getId(), client.getFullName(), client.getDni(), client.getPhone(), client.getEmail(), java.util.List.of());
    }

    public ClientDetailResponse update(UUID id, ClientRequest request) {
        Client client = getEntity(id);
        String dni = request.dni().trim();
        if (!client.getDni().equals(dni) && clientRepository.existsByDni(dni)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Ya existe un cliente con ese DNI.");
        }
        client.setFullName(normalizeName(request.fullName()));
        client.setDni(dni);
        client.setPhone(nullableTrim(request.phone()));
        client.setEmail(normalizeEmail(request.email()));
        return new ClientDetailResponse(
                client.getId(), client.getFullName(), client.getDni(), client.getPhone(), client.getEmail(),
                client.getVehicles().stream().map(VehicleMapper::toResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public Client getEntity(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "No se encontró el cliente solicitado."));
    }

    private ClientSummaryResponse toSummary(Client client) {
        Long serviceCount = entityManager.createQuery(
                        "select count(serviceRecord) from ServiceRecord serviceRecord where serviceRecord.vehicle.client.id = :clientId", Long.class)
                .setParameter("clientId", client.getId())
                .getSingleResult();
        return new ClientSummaryResponse(
                client.getId(), client.getFullName(), client.getDni(), client.getPhone(), client.getEmail(),
                client.getVehicles().size(), serviceCount
        );
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private String nullableTrim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
