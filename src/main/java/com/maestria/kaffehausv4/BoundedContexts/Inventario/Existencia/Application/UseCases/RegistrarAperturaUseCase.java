package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Application.Repositories.IConversionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.RegistrarAperturaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Operacion interna: abre una unidad cerrada de un detalle de lote y la convierte en una
 * existencia del producto base (inventario.md INV-10). No tiene endpoint.
 *
 * En una sola transaccion: bloquea la linea (INV-52), resuelve el producto base y el factor
 * de conversion de la unidad de la presentacion a la del producto base (INV-06, INV-09),
 * descuenta la unidad de 'cantidad_restante' a traves de la raiz Lote, crea la existencia
 * 'Abierta' con ese contenido, registra el movimiento 'Apertura' y recalcula el stock del
 * producto base.
 *
 * No decide que linea se abre: eso lo resuelve quien la invoca (INV-14).
 */
@Service
@Transactional
public class RegistrarAperturaUseCase {
    // Filas de catalogo que el flujo resuelve por nombre (ver databasev4.sql, seccion 7).
    private static final String ESTADO_ABIERTA = "Abierta";

    /** Escala de NUMERIC(12, 4) en 'existencias'. */
    private static final int ESCALA_CONTENIDO = 4;

    private final ILoteRepository loteRepository;
    private final IProductoRepository productoRepository;
    private final IConversionRepository conversionRepository;
    private final IUnidadRepository unidadRepository;
    private final IExistenciaRepository existenciaRepository;
    private final IEstadoExistenciaRepository estadoExistenciaRepository;
    private final IMovimientoRepository movimientoRepository;
    private final ITipoMovimientoRepository tipoMovimientoRepository;
    private final RecalcularStockUseCase recalcularStockUseCase;

    public RegistrarAperturaUseCase(
            ILoteRepository loteRepository,
            IProductoRepository productoRepository,
            IConversionRepository conversionRepository,
            IUnidadRepository unidadRepository,
            IExistenciaRepository existenciaRepository,
            IEstadoExistenciaRepository estadoExistenciaRepository,
            IMovimientoRepository movimientoRepository,
            ITipoMovimientoRepository tipoMovimientoRepository,
            RecalcularStockUseCase recalcularStockUseCase) {
        this.loteRepository = loteRepository;
        this.productoRepository = productoRepository;
        this.conversionRepository = conversionRepository;
        this.unidadRepository = unidadRepository;
        this.existenciaRepository = existenciaRepository;
        this.estadoExistenciaRepository = estadoExistenciaRepository;
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.recalcularStockUseCase = recalcularStockUseCase;
    }

    public ExistenciaResponse execute(RegistrarAperturaRequest request) {
        Lote lote = loteRepository.obtenerPorDetalleIdParaActualizar(new DetalleLoteId(request.getIdDetalleLote()))
                .orElseThrow(() -> DomainException.throwException(404, "El detalle de lote con id '" + request.getIdDetalleLote() + "' no existe"));
        DetalleLote detalle = lote.obtenerDetalle(request.getIdDetalleLote());

        if (!request.isPermitirVencido() && detalle.estaVencido(request.getFecha().toLocalDate())) {
            throw DomainException.throwException(409,
                    "El detalle de lote con id '" + request.getIdDetalleLote() + "' esta vencido; registra su vencimiento en lugar de abrirlo");
        }

        Producto presentacion = productoRepository.obtenerPorId(detalle.getProductoId())
                .orElseThrow(() -> DomainException.throwException(404, "El producto del detalle de lote no existe"));
        Producto base = presentacion.esPresentacion()
                ? productoRepository.obtenerPorId(presentacion.productoBaseId())
                        .orElseThrow(() -> DomainException.throwException(404, "El producto base de '" + presentacion.getNombre() + "' no existe"))
                : presentacion;
        if (base.esPresentacion()) {
            throw DomainException.throwException(409,
                    "El producto base de '" + presentacion.getNombre() + "' es a su vez una presentacion; la jerarquia admite un solo nivel");
        }

        BigDecimal contenido = contenidoPorUnidad(presentacion, base);

        lote.abrirUnidad(request.getIdDetalleLote());
        loteRepository.actualizar(lote.getId(), lote);

        EstadoExistencia abierta = estadoExistenciaRepository.obtenerPorNombre(ESTADO_ABIERTA)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia '" + ESTADO_ABIERTA + "' no esta registrado"));
        TipoMovimiento ajuste = tipoMovimientoRepository.obtenerPorNombre(TipoMovimiento.AJUSTE)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento '" + TipoMovimiento.AJUSTE + "' no esta registrado"));
        UsuarioId usuarioId = new UsuarioId(request.getIdUsuario());

        Existencia existencia = existenciaRepository.crear(Existencia.crear(
                contenido,
                contenido,
                null,
                detalle.getFechaVencimiento(),
                request.getFecha(),
                detalle.getId(),
                abierta.getId(),
                base.getUnidadActualId(),
                usuarioId));

        movimientoRepository.crear(Movimiento.crear(
                contenido,
                request.getFecha(),
                "Apertura de '" + presentacion.getNombre() + "'",
                existencia.getId(),
                ajuste.getId(),
                usuarioId,
                null));

        recalcularStockUseCase.execute(base.getId().getValue());

        Unidad unidad = unidadRepository.obtenerPorId(base.getUnidadActualId())
                .orElseThrow(() -> DomainException.throwException(404, "La unidad del producto base no existe"));
        return ExistenciaResponse.createFromAggregate(existencia, abierta, unidad);
    }

    /** Contenido de una unidad cerrada expresado en la unidad del producto base (INV-09). */
    private BigDecimal contenidoPorUnidad(Producto presentacion, Producto base) {
        UnidadId origen = presentacion.getUnidadActualId();
        UnidadId destino = base.getUnidadActualId();

        BigDecimal factor = Conversion.factorEntre(origen, destino, conversionRepository.obtenerVivas())
                .orElseThrow(() -> DomainException.throwException(409,
                        "No existe conversion de '" + nombreDe(origen) + "' a '" + nombreDe(destino) + "'"));

        BigDecimal contenido = factor.setScale(ESCALA_CONTENIDO, RoundingMode.HALF_UP);
        if (contenido.signum() <= 0) {
            throw DomainException.throwException(409,
                    "La conversion de '" + nombreDe(origen) + "' a '" + nombreDe(destino) + "' deja un contenido nulo");
        }
        return contenido;
    }

    private String nombreDe(UnidadId unidadId) {
        return unidadRepository.obtenerPorId(unidadId).map(Unidad::getUnidad).orElse(unidadId.getValue());
    }
}
