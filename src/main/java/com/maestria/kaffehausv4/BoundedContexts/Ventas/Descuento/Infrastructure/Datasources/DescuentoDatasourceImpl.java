package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Infrastructure.Datasources;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Datasources.IDescuentoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.ObtenerDescuentosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates.Descuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.ValueObjects.DescuentoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Infrastructure.Persistence.Jpa.DescuentoJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Infrastructure.Persistence.Jpa.DescuentoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Infrastructure.Persistence.Jpa.DescuentoJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class DescuentoDatasourceImpl implements IDescuentoDatasource {

    private final DescuentoJpaRepository descuentoJpaRepository;

    public DescuentoDatasourceImpl(DescuentoJpaRepository descuentoJpaRepository) {
        this.descuentoJpaRepository = descuentoJpaRepository;
    }

    @Override
    public Descuento crear(Descuento descuento) {
        DescuentoJpaModel model = DescuentoJpaMapper.toPersistence(descuento);
        return DescuentoJpaMapper.toDomain(descuentoJpaRepository.save(model));
    }

    @Override
    public Descuento actualizar(DescuentoId descuentoId, Descuento descuento) {
        DescuentoJpaModel model = descuentoJpaRepository.findByIdAndBorradoFalse(aUuid(descuentoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El descuento con id '" + descuentoId.getValue() + "' no existe"));

        DescuentoJpaMapper.copiarCambios(descuento, model);

        return DescuentoJpaMapper.toDomain(descuentoJpaRepository.save(model));
    }

    @Override
    public Optional<Descuento> obtenerPorId(DescuentoId descuentoId) {
        return descuentoJpaRepository.findByIdAndBorradoFalse(aUuid(descuentoId.getValue()))
                .map(DescuentoJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Descuento>> obtenerTodos(ObtenerDescuentosRequest obtenerDescuentosRequest) {
        int skip = obtenerDescuentosRequest.getSkip();
        int limit = obtenerDescuentosRequest.getLimit();
        //String search = obtenerDescuentosRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<DescuentoJpaModel> pagina = descuentoJpaRepository.findByBorradoFalse(pageable);

        List<Descuento> datos = pagina.getContent().stream()
                .map(DescuentoJpaMapper::toDomain)
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
    public void eliminar(DescuentoId descuentoId) {
        DescuentoJpaModel model = descuentoJpaRepository.findByIdAndBorradoFalse(aUuid(descuentoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El descuento con id '" + descuentoId.getValue() + "' no existe"));

        Descuento descuento = DescuentoJpaMapper.toDomain(model);
        descuento.eliminar();

        DescuentoJpaMapper.copiarCambios(descuento, model);
        descuentoJpaRepository.save(model);
    }

    @Override
    public List<Descuento> obtenerVigentes(ProductoId productoId, LocalDateTime fecha) {
        return descuentoJpaRepository
                .findByIdProductoAndEstadoTrueAndBorradoFalseAndFechaInicioLessThanEqualAndFechaFinalGreaterThanEqual(
                        aUuid(productoId.getValue()), fecha, fecha)
                .stream()
                .map(DescuentoJpaMapper::toDomain)
                .collect(Collectors.toList());
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }

    @Override
    public boolean existeConTipoDescuento(TipoDescuentoId tipoDescuentoId) {
        return descuentoJpaRepository.existsByIdTipoDescuentoAndBorradoFalse(aUuid(tipoDescuentoId.getValue()));
    }

    @Override
    public List<Descuento> obtenerPorProducto(ProductoId productoId) {
        return descuentoJpaRepository.findByIdProductoAndBorradoFalse(aUuid(productoId.getValue())).stream()
                .map(DescuentoJpaMapper::toDomain)
                .collect(Collectors.toList());
    }
}
