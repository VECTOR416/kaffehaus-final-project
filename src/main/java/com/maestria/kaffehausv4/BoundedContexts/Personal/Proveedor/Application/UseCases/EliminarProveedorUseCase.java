package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Repositories.IProveedorRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarProveedorUseCase {
    private final IProveedorRepository proveedorRepository;

    public EliminarProveedorUseCase(IProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    public void execute(String proveedorId) {
        ProveedorId id = new ProveedorId(proveedorId);

        proveedorRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El proveedor con id '" + proveedorId + "' no existe"));

        proveedorRepository.eliminar(id);
    }
}
