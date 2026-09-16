package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Datasources.IDetalleVentaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos.ObtenerDetallesVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.ValueObjects.DetalleVentaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Infrastructure.Persistence.Jpa.DetalleVentaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Infrastructure.Persistence.Jpa.DetalleVentaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Infrastructure.Persistence.Jpa.DetalleVentaJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class DetalleVentaDatasourceImpl implements IDetalleVentaDatasource {

    private final DetalleVentaJpaRepository detalleVentaJpaRepository;

    public DetalleVentaDatasourceImpl(DetalleVentaJpaRepository detalleVentaJpaRepository) {
        this.detalleVentaJpaRepository = detalleVentaJpaRepository;
    }

    @Override
    public DetalleVenta crear(DetalleVenta detalleVenta) {
        DetalleVentaJpaModel model = DetalleVentaJpaMapper.toPersistence(detalleVenta);
        return DetalleVentaJpaMapper.toDomain(detalleVentaJpaRepository.save(model));
    }

    @Override
    public DetalleVenta actualizar(DetalleVentaId detalleVentaId, DetalleVenta detalleVenta) {
        DetalleVentaJpaModel model = detalleVentaJpaRepository.findById(aUuid(detalleVentaId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El detalle de venta con id '" + detalleVentaId.getValue() + "' no existe"));

        DetalleVentaJpaMapper.copiarCambios(detalleVenta, model);

        return DetalleVentaJpaMapper.toDomain(detalleVentaJpaRepository.save(model));
    }

    @Override
    public Optional<DetalleVenta> obtenerPorId(DetalleVentaId detalleVentaId) {
        return detalleVentaJpaRepository.findById(aUuid(detalleVentaId.getValue()))
                .map(DetalleVentaJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<DetalleVenta>> obtenerTodos(ObtenerDetallesVentaRequest obtenerDetallesVentaRequest) {
        int skip = obtenerDetallesVentaRequest.getSkip();
        int limit = obtenerDetallesVentaRequest.getLimit();
        //String search = obtenerDetallesVentaRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<DetalleVentaJpaModel> pagina = detalleVentaJpaRepository.findAll(pageable);

        List<DetalleVenta> datos = pagina.getContent().stream()
                .map(DetalleVentaJpaMapper::toDomain)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                datos,
                skip,
                limit,
                pagina.getTotalElements(),
                skip + datos.size() < pagina.getTotalElements()
        );
    }

    @Override
    public void eliminar(DetalleVentaId detalleVentaId) {
        UUID id = aUuid(detalleVentaId.getValue());

        if (!detalleVentaJpaRepository.existsById(id)) {
            throw DomainException.throwException(404, "El detalle de venta con id '" + detalleVentaId.getValue() + "' no existe");
        }

        detalleVentaJpaRepository.deleteById(id);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
