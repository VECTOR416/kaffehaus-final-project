package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Repositories.IEntidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.ObtenerProveedoresRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.ProveedorResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Repositories.IProveedorRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.Aggregates.Proveedor;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;



@Service
@Transactional(readOnly = true)
public class ObtenerProveedoresUseCase {
    private final IProveedorRepository proveedorRepository;
    private final IPersonaRepository personaRepository;
    private final IEntidadRepository entidadRepository;

    public ObtenerProveedoresUseCase(
            IProveedorRepository proveedorRepository,
            IPersonaRepository personaRepository,
            IEntidadRepository entidadRepository) {
        this.proveedorRepository = proveedorRepository;
        this.personaRepository = personaRepository;
        this.entidadRepository = entidadRepository;
    }

    public PaginatedResponse<List<ProveedorResponse>> execute(ObtenerProveedoresRequest obtenerProveedoresRequest) {
        PaginatedResponse<List<Proveedor>> resultado = proveedorRepository.obtenerTodos(obtenerProveedoresRequest);

        List<ProveedorResponse> proveedores = resultado.getData().stream()
                .map(this::toProveedorResponse)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                proveedores,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }

    private ProveedorResponse toProveedorResponse(Proveedor proveedor) {
        Persona persona = proveedor.getPersonaId() != null
                ? personaRepository.obtenerPorId(proveedor.getPersonaId())
                        .orElseThrow(() -> DomainException.throwException(404, "La persona asociada al proveedor no existe"))
                : null;
        Entidad entidad = proveedor.getEntidadId() != null
                ? entidadRepository.obtenerPorId(proveedor.getEntidadId())
                        .orElseThrow(() -> DomainException.throwException(404, "La entidad asociada al proveedor no existe"))
                : null;
        return ProveedorResponse.createFromAggregate(proveedor, persona, entidad);
    }
}
