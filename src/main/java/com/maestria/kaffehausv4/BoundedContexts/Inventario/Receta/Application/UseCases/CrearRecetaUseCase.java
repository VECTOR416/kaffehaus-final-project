package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.CrearRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.IngredienteRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.PasoRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.RecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Alta de una receta con sus ingredientes y sus pasos en una sola operacion.
 *
 * El caso de uso valida las referencias a Producto (el que se obtiene y cada insumo), arma el
 * agregado Receta completo incorporando ambas colecciones por los metodos de la raiz y lo
 * entrega una sola vez a IRecetaRepository. La @Transactional de la clase cubre las tres
 * tablas: si un ingrediente o un paso es invalido, la transaccion revierte entera y no queda
 * una receta a medio escribir.
 */
@Service
@Transactional
public class CrearRecetaUseCase {
    private final IRecetaRepository recetaRepository;
    private final IProductoRepository productoRepository;

    public CrearRecetaUseCase(
            IRecetaRepository recetaRepository,
            IProductoRepository productoRepository) {
        this.recetaRepository = recetaRepository;
        this.productoRepository = productoRepository;
    }

    public RecetaResponse execute(CrearRecetaRequest crearRecetaRequest) {
        ProductoId productoObtenidoId = new ProductoId(crearRecetaRequest.getIdProductoObtenido());
        productoRepository.obtenerPorId(productoObtenidoId)
                .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + crearRecetaRequest.getIdProductoObtenido() + "' no existe"));

        Receta receta = Receta.crear(
                crearRecetaRequest.getCantidad(),
                crearRecetaRequest.getTitulo(),
                productoObtenidoId);

        agregarIngredientes(receta, crearRecetaRequest.getIngredientes());
        agregarPasos(receta, crearRecetaRequest.getPasos());
        receta.validarRegistrable();
        exigirSinCiclos(receta, productoRepository, recetaRepository);

        Receta recetaCreada = recetaRepository.crear(receta);

        return RecetaResponse.createFromAggregate(recetaCreada);
    }

    /**
     * Incorpora los insumos. Cada producto se valida contra su propio agregado antes de
     * entregarselo a Receta, que a su vez rechaza repetir un producto ya presente.
     */
    private void agregarIngredientes(Receta receta, List<IngredienteRecetaRequest> ingredientes) {
        for (IngredienteRecetaRequest ingrediente : ingredientes) {
            ProductoId productoId = new ProductoId(ingrediente.getIdProducto());
            Producto producto = productoRepository.obtenerPorId(productoId)
                    .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + ingrediente.getIdProducto() + "' no existe"));
            // Las recetas se expresan en productos base (inventario.md INV-08).
            if (producto.esPresentacion()) {
                throw DomainException.throwException(400,
                        "El producto '" + producto.getNombre() + "' es una presentacion; la receta debe usar su producto base");
            }

            receta.agregarIngrediente(productoId, ingrediente.getCantidad());
        }
    }

    /**
     * Validaciones de logica.md RECETA-07 sobre la receta ya armada: el producto obtenido no es uno
     * de sus ingredientes (400) y la receta no forma un ciclo con las recetas vigentes de sus
     * ingredientes MTO, recorridas recursivamente (409 nombrando el ciclo). Solo se recorren los MTO
     * porque son los unicos que el plan de consumo explota (GENERAL-14); un MTS se descuenta de su
     * stock. La salida de inventario lo sigue comprobando como defensa (MOVIMIENTO-19).
     */
    static void exigirSinCiclos(Receta receta, IProductoRepository productoRepository, IRecetaRepository recetaRepository) {
        ProductoId obtenido = receta.getProductoObtenidoId();
        for (IngredienteReceta ingrediente : receta.getIngredientesActivos()) {
            if (ingrediente.getProductoId().getValue().equals(obtenido.getValue())) {
                throw DomainException.throwException(400, "El producto que obtiene la receta no puede ser uno de sus ingredientes");
            }
        }

        String nombreObtenido = productoRepository.obtenerPorId(obtenido).map(Producto::getNombre).orElse(obtenido.getValue());
        Set<String> visitados = new HashSet<>();
        for (IngredienteReceta ingrediente : receta.getIngredientesActivos()) {
            Deque<String> camino = new ArrayDeque<>(List.of(nombreObtenido));
            if (llegaA(ingrediente.getProductoId(), obtenido.getValue(), camino, visitados, productoRepository, recetaRepository)) {
                throw DomainException.throwException(409, "La receta forma un ciclo entre recetas: " + String.join(" -> ", camino));
            }
        }
    }

    /** Busca 'objetivo' bajando por las recetas vigentes de los MTO; deja en 'camino' el recorrido. */
    private static boolean llegaA(
            ProductoId actual,
            String objetivo,
            Deque<String> camino,
            Set<String> visitados,
            IProductoRepository productoRepository,
            IRecetaRepository recetaRepository) {
        Optional<Producto> producto = productoRepository.obtenerPorId(actual);
        if (producto.isEmpty()) {
            return false;
        }
        camino.addLast(producto.get().getNombre());
        if (actual.getValue().equals(objetivo)) {
            return true;
        }
        if (!producto.get().isEsStockDirecto() && visitados.add(actual.getValue())) {
            Optional<Receta> vigente = recetaRepository.obtenerPorProductoObtenido(actual);
            if (vigente.isPresent()) {
                for (IngredienteReceta ingrediente : vigente.get().getIngredientesActivos()) {
                    if (llegaA(ingrediente.getProductoId(), objetivo, camino, visitados, productoRepository, recetaRepository)) {
                        return true;
                    }
                }
            }
        }
        camino.removeLast();
        return false;
    }

    /** Incorpora los pasos en el orden del array: la raiz les asigna la secuencia 1..N. */
    private void agregarPasos(Receta receta, List<PasoRecetaRequest> pasos) {
        for (PasoRecetaRequest paso : pasos) {
            receta.agregarPaso(paso.getDescripcion());
        }
    }
}
