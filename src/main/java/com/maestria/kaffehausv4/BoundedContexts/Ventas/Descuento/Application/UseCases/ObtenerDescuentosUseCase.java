package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.DescuentoResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Dtos.ObtenerDescuentosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Application.Repositories.IDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Domain.Aggregates.Descuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Repositories.ITipoDescuentoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerDescuentosUseCase {
    private final IDescuentoRepository descuentoRepository;
    private final ITipoDescuentoRepository tipoDescuentoRepository;

    public ObtenerDescuentosUseCase(
            IDescuentoRepository descuentoRepository,
            ITipoDescuentoRepository tipoDescuentoRepository) {
        this.descuentoRepository = descuentoRepository;
        this.tipoDescuentoRepository = tipoDescuentoRepository;
    }

    public PaginatedResponse<List<DescuentoResponse>> execute(ObtenerDescuentosRequest obtenerDescuentosRequest) {
        PaginatedResponse<List<Descuento>> resultado = descuentoRepository.obtenerTodos(obtenerDescuentosRequest);

        List<DescuentoResponse> descuentos = resultado.getData().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                descuentos,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }

    private DescuentoResponse toResponse(Descuento descuento) {
        TipoDescuento tipoDescuento = tipoDescuentoRepository.obtenerPorId(descuento.getTipoDescuentoId())
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de descuento asociado a el descuento no existe"));
        return DescuentoResponse.createFromAggregate(descuento, tipoDescuento);
    }
}
