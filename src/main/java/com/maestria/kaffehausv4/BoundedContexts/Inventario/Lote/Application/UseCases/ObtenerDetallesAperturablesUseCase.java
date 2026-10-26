package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.UseCases;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Repositories.IConversionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.DetalleAperturableResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerDetallesAperturablesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Lista, para un producto base, que empaques cerrados se pueden abrir para obtenerlo
 * (inventario.md INV-13): una fila por presentacion (y por el propio producto si se compro
 * suelto), con el detalle que toca abrir segun FEFO. El usuario elige una fila y la abre con
 * AbrirDetalleLoteUseCase; el sistema nunca elige por el (INV-34).
 */
@Service
@Transactional(readOnly = true)
public class ObtenerDetallesAperturablesUseCase {
    private final IProductoRepository productoRepository;
    private final ILoteRepository loteRepository;
    private final IConversionRepository conversionRepository;

    public ObtenerDetallesAperturablesUseCase(
            IProductoRepository productoRepository,
            ILoteRepository loteRepository,
            IConversionRepository conversionRepository) {
        this.productoRepository = productoRepository;
        this.loteRepository = loteRepository;
        this.conversionRepository = conversionRepository;
    }

    public List<DetalleAperturableResponse> execute(ObtenerDetallesAperturablesRequest request) {
        Producto base = productoRepository.obtenerPorId(new ProductoId(request.getIdProducto()))
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + request.getIdProducto() + "' no existe"));
        if (base.esPresentacion()) {
            throw DomainException.throwException(400,
                    "El producto '" + base.getNombre() + "' es una presentacion; consulta por su producto base");
        }

        // Se abren presentaciones: el producto base no se compra suelto (logica.md LOTE-13, LOTE-14).
        List<Producto> fuentes = new ArrayList<>(productoRepository.obtenerPresentaciones(base.getId()));

        List<Lote> lotes = loteRepository.obtenerPorProductos(fuentes.stream().map(Producto::getId).toList());
        List<Conversion> conversiones = conversionRepository.obtenerVivas();
        LocalDate hoy = LocalDate.now();

        List<Fila> filas = new ArrayList<>();
        for (Producto fuente : fuentes) {
            List<DetalleLote> enOrden = Lote.aperturablesEnOrden(lotes, fuente.getId(), hoy);
            if (enOrden.isEmpty()) {
                continue;
            }

            DetalleLote siguiente = enOrden.get(0);
            Lote lote = Lote.dueno(lotes, siguiente);
            BigDecimal cerradas = enOrden.stream().map(DetalleLote::getCantidadRestante).reduce(BigDecimal.ZERO, BigDecimal::add);
            Optional<BigDecimal> contenido = Conversion.factorEntre(fuente.getUnidadActualId(), base.getUnidadActualId(), conversiones);

            filas.add(new Fila(lote, siguiente, DetalleAperturableResponse.createFromAggregate(
                    lote,
                    siguiente,
                    fuente,
                    base,
                    cerradas,
                    contenido.orElse(null),
                    contenido.map(factor -> lote.costoPorUnidadDeContenido(siguiente.getId(), factor)).orElse(null))));
        }

        // La primera fila es lo que conviene abrir segun FEFO; el usuario puede elegir otra.
        return filas.stream()
                .sorted(Comparator.comparing((Fila fila) -> fila.detalle.getFechaVencimiento(), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(fila -> fila.lote.getFechaCompra(), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(fila -> fila.detalle.getId().getValue()))
                .map(fila -> fila.respuesta)
                .toList();
    }

    private static final class Fila {
        private final Lote lote;
        private final DetalleLote detalle;
        private final DetalleAperturableResponse respuesta;

        private Fila(Lote lote, DetalleLote detalle, DetalleAperturableResponse respuesta) {
            this.lote = lote;
            this.detalle = detalle;
            this.respuesta = respuesta;
        }
    }
}
