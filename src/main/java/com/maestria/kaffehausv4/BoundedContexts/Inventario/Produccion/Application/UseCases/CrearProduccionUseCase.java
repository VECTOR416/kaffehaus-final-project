package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.UseCases;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories.ILoteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.RegistrarSalidaDeInventarioRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.UseCases.RegistrarSalidaDeInventarioUseCase;
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
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Registra una produccion por lotes (MTS) y todo el inventario que mueve, en una transaccion:
 *
 * 1. Guarda la produccion, con su diferencia derivada.
 * 2. Consume los insumos de la receta, escalados a la cantidad esperada, con un solo plan de
 *    consumo (logica.md PRODUCCION-06, MOVIMIENTO-19): de existencias los productos base y de sus
 *    lotes los productos por unidades, con un movimiento 'Produccion' vinculado a la produccion
 *    por cada existencia o linea tocada.
 * 3. Si salieron unidades, crea un lote de produccion (sin proveedor ni costo) con una linea de
 *    'cantidadReal' unidades disponibles y su caducidad, y recalcula el stock del terminado
 *    (PRODUCCION-12). No crea existencia ni movimiento de entrada: el terminado se maneja por
 *    unidades, y la venta o la receta que lo use las descuenta de ese lote.
 *
 * No calcula dinero (GENERAL-11). Si falta un insumo, la transaccion revierte entera: no queda
 * produccion, consumo ni lote a medias.
 */
@Service
@Transactional
public class CrearProduccionUseCase {
    private final IProduccionRepository produccionRepository;
    private final IRecetaRepository recetaRepository;
    private final IProductoRepository productoRepository;
    private final IUsuarioRepository usuarioRepository;
    private final ILoteRepository loteRepository;
    private final RegistrarSalidaDeInventarioUseCase registrarSalidaDeInventarioUseCase;
    private final RecalcularStockUseCase recalcularStockUseCase;

    public CrearProduccionUseCase(
            IProduccionRepository produccionRepository,
            IRecetaRepository recetaRepository,
            IProductoRepository productoRepository,
            IUsuarioRepository usuarioRepository,
            ILoteRepository loteRepository,
            RegistrarSalidaDeInventarioUseCase registrarSalidaDeInventarioUseCase,
            RecalcularStockUseCase recalcularStockUseCase) {
        this.produccionRepository = produccionRepository;
        this.recetaRepository = recetaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.loteRepository = loteRepository;
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

        consumirInsumos(produccionCreada, receta, usuarioId);

        if (produccionCreada.produjoUnidades()) {
            registrarProductoTerminado(produccionCreada, productoObtenido, usuarioId, crearProduccionRequest.getFechaVencimiento());
        }

        return ProduccionResponse.createFromAggregate(produccionCreada);
    }

    /** Todos los ingredientes juntos, para que la salida arme un solo plan de consumo (GENERAL-14). */
    private void consumirInsumos(Produccion produccion, Receta receta, UsuarioId usuarioId) {
        BigDecimal factor = produccion.factorSobre(receta.getCantidad());
        RegistrarSalidaDeInventarioRequest salida = RegistrarSalidaDeInventarioRequest.deProduccion(
                usuarioId.getValue(),
                produccion.getId().getValue(),
                "Consumo de la produccion '" + receta.getTitulo() + "'",
                produccion.getFechaProduccion());
        for (IngredienteReceta ingrediente : receta.getIngredientesActivos()) {
            salida.con(ingrediente.getProductoId().getValue(), ingrediente.getCantidad().multiply(factor));
        }
        registrarSalidaDeInventarioUseCase.execute(salida);
    }

    /** El terminado entra como unidades disponibles de su lote de produccion (logica.md PRODUCCION-12). */
    private void registrarProductoTerminado(Produccion produccion, Producto productoObtenido, UsuarioId usuarioId, LocalDate fechaVencimiento) {
        loteRepository.crear(Lote.registrarProduccion(
                produccion.getFechaProduccion(),
                produccion.getId(),
                usuarioId,
                productoObtenido.getId(),
                produccion.getCantidadReal(),
                fechaVencimiento));
        recalcularStockUseCase.execute(productoObtenido.getId().getValue());
    }
}
