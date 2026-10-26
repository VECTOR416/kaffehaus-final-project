package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Datasources.ITipoPagoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Application.Dtos.ObtenerTiposPagoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.ValueObjects.TipoPagoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Infrastructure.Persistence.Jpa.TipoPagoJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Infrastructure.Persistence.Jpa.TipoPagoJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Infrastructure.Persistence.Jpa.TipoPagoJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository
public class TipoPagoDatasourceImpl implements ITipoPagoDatasource {

    private final TipoPagoJpaRepository tipoPagoJpaRepository;

    public TipoPagoDatasourceImpl(TipoPagoJpaRepository tipoPagoJpaRepository) {
        this.tipoPagoJpaRepository = tipoPagoJpaRepository;
    }

    @Override
    public TipoPago crear(TipoPago tipoPago) {
        TipoPagoJpaModel model = TipoPagoJpaMapper.toPersistence(tipoPago);
        return TipoPagoJpaMapper.toDomain(tipoPagoJpaRepository.save(model));
    }

    @Override
    public TipoPago actualizar(TipoPagoId tipoPagoId, TipoPago tipoPago) {
        TipoPagoJpaModel model = tipoPagoJpaRepository.findByIdAndBorradoFalse(aUuid(tipoPagoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de pago con id '" + tipoPagoId.getValue() + "' no existe"));

        TipoPagoJpaMapper.copiarCambios(tipoPago, model);

        return TipoPagoJpaMapper.toDomain(tipoPagoJpaRepository.save(model));
    }

    @Override
    public Optional<TipoPago> obtenerPorId(TipoPagoId tipoPagoId) {
        return tipoPagoJpaRepository.findByIdAndBorradoFalse(aUuid(tipoPagoId.getValue()))
                .map(TipoPagoJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<TipoPago>> obtenerTodos(ObtenerTiposPagoRequest obtenerTiposPagoRequest) {
        int skip = obtenerTiposPagoRequest.getSkip();
        int limit = obtenerTiposPagoRequest.getLimit();
        String search = obtenerTiposPagoRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<TipoPagoJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? tipoPagoJpaRepository.findByBorradoFalse(pageable)
                : tipoPagoJpaRepository.findByBorradoFalseAndNombreContainingIgnoreCase(search.trim(), pageable);

        List<TipoPago> datos = pagina.getContent().stream()
                .map(TipoPagoJpaMapper::toDomain)
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
    public void eliminar(TipoPagoId tipoPagoId) {
        TipoPagoJpaModel model = tipoPagoJpaRepository.findByIdAndBorradoFalse(aUuid(tipoPagoId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de pago con id '" + tipoPagoId.getValue() + "' no existe"));

        TipoPago tipoPago = TipoPagoJpaMapper.toDomain(model);
        tipoPago.eliminar();

        TipoPagoJpaMapper.copiarCambios(tipoPago, model);
        tipoPagoJpaRepository.save(model);
    }

    @Override
    public Optional<TipoPago> obtenerPorNombre(String nombre) {
        return tipoPagoJpaRepository.findFirstByNombreIgnoreCaseAndBorradoFalse(nombre)
                .map(TipoPagoJpaMapper::toDomain);
    }

    @Override
    public List<TipoPago> obtenerVivos() {
        return tipoPagoJpaRepository.findByBorradoFalse().stream()
                .map(TipoPagoJpaMapper::toDomain)
                .collect(Collectors.toList());
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
