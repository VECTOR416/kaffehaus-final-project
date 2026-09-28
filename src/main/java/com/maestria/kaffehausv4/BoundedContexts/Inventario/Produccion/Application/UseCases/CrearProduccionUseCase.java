package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.UseCases;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarSalidaDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Repositories.IMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.RegistrarSalidaDeInventarioUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.CrearProduccionRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ProduccionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Repositories.IProduccionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.Aggregates.Produccion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.UseCases.RecalcularStockUseCase;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories.ITipoMovimientoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Registra una produccion por lotes (MTS) y todo el inventario que mueve, en una transaccion:
 *
 * 1. Guarda la produccion, con su diferencia derivada.
 * 2. Consume los insumos de la receta, escalados a la cantidad esperada, desde sus
 *    existencias. Cada consumo queda como movimiento vinculado a la produccion.
 * 3. Si salieron unidades, el producto terminado no se suma a 'producto.stock': abre un lote
 *    de produccion (sin proveedor) con una linea a costo unitario = costo de los insumos /
 *    cantidad real, y una existencia con esa cantidad y su caducidad corta.
 * 4. Registra la entrada de esa existencia como movimiento vinculado a la produccion.
 *
 * Desde ese momento el producto terminado se vende en el POS o se consume como insumo de otra
 * receta por el mismo camino que cualquier producto de stock directo.
 *
 * Si falta stock de un insumo, la transaccion revierte entera: no queda produccion, consumo
 * ni lote a medias.
 */
@Service
@Transactional
public class CrearProduccionUseCase {
    // Filas de catalogo que el flujo resuelve por nombre (ver databasev4.sql, seccion 7).
    private static final String TIPO_CONSUMO_PRODUCCION = "Consumo produccion";
    private static final String TIPO_INGRESO_PRODUCCION = "Ingreso produccion";
    private static final String ESTADO_ABIERTA = "Abierta";

    private final IProduccionRepository produccionRepository;
    private final IRecetaRepository recetaRepository;
    private final IProductoRepository productoRepository;
    private final IUsuarioRepository usuarioRepository;
    private final ILoteRepository loteRepository;
    private final IExistenciaRepository existenciaRepository;
    private final IEstadoExistenciaRepository estadoExistenciaRepository;
    private final IMovimientoRepository movimientoRepository;
    private final ITipoMovimientoRepository tipoMovimientoRepository;
    private final RegistrarSalidaDeInventarioUseCase registrarSalidaDeInventarioUseCase;
    private final RecalcularStockUseCase recalcularStockUseCase;

    public CrearProduccionUseCase(
            IProduccionRepository produccionRepository,
            IRecetaRepository recetaRepository,
            IProductoRepository productoRepository,
            IUsuarioRepository usuarioRepository,
            ILoteRepository loteRepository,
            IExistenciaRepository existenciaRepository,
            IEstadoExistenciaRepository estadoExistenciaRepository,
            IMovimientoRepository movimientoRepository,
            ITipoMovimientoRepository tipoMovimientoRepository,
            RegistrarSalidaDeInventarioUseCase registrarSalidaDeInventarioUseCase,
            RecalcularStockUseCase recalcularStockUseCase) {
        this.produccionRepository = produccionRepository;
        this.recetaRepository = recetaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.loteRepository = loteRepository;
        this.existenciaRepository = existenciaRepository;
        this.estadoExistenciaRepository = estadoExistenciaRepository;
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.registrarSalidaDeInventarioUseCase = registrarSalidaDeInventarioUseCase;
        this.recalcularStockUseCase = recalcularStockUseCase;
    }

    public ProduccionResponse execute(CrearProduccionRequest crearProduccionRequest) {
        RecetaId recetaId = new RecetaId(crearProduccionRequest.getIdReceta());
        Receta receta = recetaRepository.obtenerPorId(recetaId)
                .orElseThrow(() -> DomainException.throwException(404, "La receta con id '" + crearProduccionRequest.getIdReceta() + "' no existe"));
        Producto productoObtenido = productoRepository.obtenerPorId(receta.getProductoObtenidoId())
                .orElseThrow(() -> DomainException.throwException(404, "El producto que obtiene la receta no existe"));
        if (!productoObtenido.isEsStockDirecto()) {
            throw DomainException.throwException(409,
                    "El producto '" + productoObtenido.getNombre() + "' se prepara bajo demanda (MTO): se descuenta al venderse y no se produce por lotes");
        }
        UsuarioId usuarioId = new UsuarioId(crearProduccionRequest.getIdUsuario());
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> DomainException.throwException(404, "El usuario con id '" + crearProduccionRequest.getIdUsuario() + "' no existe"));

        Produccion produccion = Produccion.crear(
                crearProduccionRequest.getCantidadEsperada(),
                crearProduccionRequest.getCantidadReal(),
                crearProduccionRequest.getMotivoDiferencia(),
                crearProduccionRequest.getFechaProduccion(),
                recetaId);

        Produccion produccionCreada = produccionRepository.crear(produccion);

        BigDecimal costoInsumos = consumirInsumos(produccionCreada, receta, usuarioId);

        if (produccionCreada.produjoUnidades()) {
            abrirProductoTerminado(produccionCreada, receta, productoObtenido, usuarioId, costoInsumos, crearProduccionRequest.getFechaVencimiento());
        }

        return ProduccionResponse.createFromAggregate(produccionCreada);
    }

    private BigDecimal consumirInsumos(Produccion produccion, Receta receta, UsuarioId usuarioId) {
        BigDecimal factor = produccion.factorSobre(receta.getCantidad());

        BigDecimal costo = BigDecimal.ZERO;
        for (IngredienteReceta ingrediente : receta.getIngredientesActivos()) {
            costo = costo.add(registrarSalidaDeInventarioUseCase.execute(RegistrarSalidaDeInventarioRequest.crear(
                    ingrediente.getProductoId().getValue(),
                    ingrediente.getCantidad().multiply(factor),
                    TIPO_CONSUMO_PRODUCCION,
                    TIPO_CONSUMO_PRODUCCION,
                    usuarioId.getValue(),
                    produccion.getId().getValue(),
                    "Consumo de la produccion '" + receta.getTitulo() + "'",
                    produccion.getFechaProduccion())).getCostoTotal());
        }
        return costo;
    }

    private void abrirProductoTerminado(
            Produccion produccion,
            Receta receta,
            Producto productoObtenido,
            UsuarioId usuarioId,
            BigDecimal costoInsumos,
            LocalDate fechaVencimiento) {
        EstadoExistencia abierta = estadoExistenciaRepository.obtenerPorNombre(ESTADO_ABIERTA)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia '" + ESTADO_ABIERTA + "' no esta registrado"));
        TipoMovimiento ingreso = tipoMovimientoRepository.obtenerPorNombre(TIPO_INGRESO_PRODUCCION)
                .orElseThrow(() -> DomainException.throwException(404, "El tipo de movimiento '" + TIPO_INGRESO_PRODUCCION + "' no esta registrado"));

        BigDecimal cantidadReal = produccion.getCantidadReal();
        BigDecimal costoUnitario = costoInsumos.divide(cantidadReal, 2, RoundingMode.HALF_UP);

        Lote lote = loteRepository.crear(Lote.registrarProduccion(
                produccion.getFechaProduccion(),
                produccion.getId(),
                usuarioId,
                productoObtenido.getId(),
                cantidadReal,
                costoUnitario,
                fechaVencimiento));
        DetalleLote detalle = lote.getDetallesActivos().get(0);

        Existencia existencia = existenciaRepository.crear(Existencia.crear(
                cantidadReal,
                cantidadReal,
                null,
                fechaVencimiento,
                produccion.getFechaProduccion(),
                detalle.getId(),
                abierta.getId(),
                productoObtenido.getUnidadActualId(),
                usuarioId));

        movimientoRepository.crear(Movimiento.crear(
                cantidadReal,
                produccion.getFechaProduccion(),
                "Entrada de la produccion '" + receta.getTitulo() + "'",
                existencia.getId(),
                ingreso.getId(),
                usuarioId,
                produccion.getId()));

        recalcularStockUseCase.execute(productoObtenido.getId().getValue());
    }
}
