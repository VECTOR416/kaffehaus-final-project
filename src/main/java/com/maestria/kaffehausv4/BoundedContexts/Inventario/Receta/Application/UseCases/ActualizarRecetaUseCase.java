package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Application.Repositories.IProductoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.Aggregates.Producto;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.ActualizarIngredienteRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.ActualizarPasoRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.ActualizarRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.RecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Modificacion de una receta existente: encabezado, ingredientes y pasos en una sola
 * transaccion.
 *
 * Las dos colecciones son independientes entre si y cada una se concilia por id: el elemento
 * con id se corrige, el que no lo trae se agrega y el ausente se da de baja logica. Se
 * concilia y no se reemplaza porque el sistema no usa borrado fisico.
 *
 * En los pasos hay una regla adicional: el orden del array es la secuencia resultante. Por eso
 * cada paso que sobrevive se mueve al final segun se recorre el array; al terminar, la
 * secuencia coincide con el payload aunque se mezclen pasos existentes y nuevos.
 */
@Service
@Transactional
public class ActualizarRecetaUseCase {
    private final IRecetaRepository recetaRepository;
    private final IProductoRepository productoRepository;

    public ActualizarRecetaUseCase(
            IRecetaRepository recetaRepository,
            IProductoRepository productoRepository) {
        this.recetaRepository = recetaRepository;
        this.productoRepository = productoRepository;
    }

    public RecetaResponse execute(String recetaId, ActualizarRecetaRequest actualizarRecetaRequest) {
        RecetaId id = new RecetaId(recetaId);

        Receta receta = recetaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La receta con id '" + recetaId + "' no existe"));

        ProductoId productoObtenidoId = null;
        if (actualizarRecetaRequest.getIdProductoObtenido() != null) {
            productoObtenidoId = new ProductoId(actualizarRecetaRequest.getIdProductoObtenido());
            productoRepository.obtenerPorId(productoObtenidoId)
                    .orElseThrow(() -> DomainException.throwException(404, "El producto con id '" + actualizarRecetaRequest.getIdProductoObtenido() + "' no existe"));
        }

        receta.actualizar(
                actualizarRecetaRequest.getCantidad(),
                actualizarRecetaRequest.getTitulo(),
                productoObtenidoId);

        if (actualizarRecetaRequest.getIngredientes() != null) {
            conciliarIngredientes(receta, actualizarRecetaRequest.getIngredientes());
        }
        if (actualizarRecetaRequest.getPasos() != null) {
            conciliarPasos(receta, actualizarRecetaRequest.getPasos());
        }

        receta.validarRegistrable();
        CrearRecetaUseCase.exigirSinCiclos(receta, productoRepository, recetaRepository);

        Receta recetaActualizada = recetaRepository.actualizar(id, receta);

        return RecetaResponse.createFromAggregate(recetaActualizada);
    }

    private void conciliarIngredientes(Receta receta, List<ActualizarIngredienteRecetaRequest> ingredientes) {
        Set<String> idsRecibidos = ingredientes.stream()
                .map(ActualizarIngredienteRecetaRequest::getId)
                .filter(idIngrediente -> idIngrediente != null)
                .collect(Collectors.toSet());

        List<String> idsVivos = receta.getIngredientesActivos().stream()
                .map(ingrediente -> ingrediente.getId().getValue())
                .collect(Collectors.toList());

        // Primero las bajas: liberan el producto y permiten volver a darlo de alta en la misma
        // peticion sin chocar con la invariante de ingrediente repetido.
        for (String idVivo : idsVivos) {
            if (!idsRecibidos.contains(idVivo)) {
                receta.removerIngrediente(idVivo);
            }
        }

        for (ActualizarIngredienteRecetaRequest ingrediente : ingredientes) {
            if (ingrediente.getId() == null) {
                agregarIngredienteNuevo(receta, ingrediente);
            } else {
                corregirIngredienteExistente(receta, ingrediente);
            }
        }
    }

    private void agregarIngredienteNuevo(Receta receta, ActualizarIngredienteRecetaRequest ingrediente) {
        if (ingrediente.getIdProducto() == null || ingrediente.getCantidad() == null) {
            throw DomainException.throwException(400,
                    "Un ingrediente nuevo requiere 'idProducto' y 'cantidad'");
        }

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

    private void corregirIngredienteExistente(Receta receta, ActualizarIngredienteRecetaRequest ingrediente) {
        // Cambiar el producto de un ingrediente es cambiar de insumo: se da de baja y se agrega
        // otro, para que la invariante de no repetidos siga siendo comprobable.
        if (ingrediente.getIdProducto() != null && !ingrediente.getIdProducto().equals(productoActualDe(receta, ingrediente.getId()))) {
            throw DomainException.throwException(400,
                    "No se puede cambiar el producto de un ingrediente existente; da de baja la linea y agrega una nueva");
        }

        receta.actualizarIngrediente(ingrediente.getId(), ingrediente.getCantidad());
    }

    private String productoActualDe(Receta receta, String ingredienteRecetaId) {
        return receta.getIngredientesActivos().stream()
                .filter(ingrediente -> ingrediente.getId().getValue().equals(ingredienteRecetaId))
                .map(IngredienteReceta::getProductoId)
                .map(ProductoId::getValue)
                .findFirst()
                .orElseThrow(() -> DomainException.throwException(404,
                        "El ingrediente con id '" + ingredienteRecetaId + "' no pertenece a esta receta"));
    }

    private void conciliarPasos(Receta receta, List<ActualizarPasoRecetaRequest> pasos) {
        Set<String> idsRecibidos = pasos.stream()
                .map(ActualizarPasoRecetaRequest::getId)
                .filter(idPaso -> idPaso != null)
                .collect(Collectors.toSet());

        List<String> idsVivos = receta.getPasosActivos().stream()
                .map(paso -> paso.getId().getValue())
                .collect(Collectors.toList());

        for (String idVivo : idsVivos) {
            if (!idsRecibidos.contains(idVivo)) {
                receta.removerPaso(idVivo);
            }
        }

        // El recorrido en orden reconstruye la secuencia: cada paso que sobrevive se lleva al
        // final, y cada paso nuevo nace al final.
        for (ActualizarPasoRecetaRequest paso : pasos) {
            if (paso.getId() == null) {
                if (paso.getDescripcion() == null) {
                    throw DomainException.throwException(400, "Un paso nuevo requiere 'descripcion'");
                }
                receta.agregarPaso(paso.getDescripcion());
            } else {
                receta.actualizarPaso(paso.getId(), paso.getDescripcion());
                receta.moverPasoAlFinal(paso.getId());
            }
        }
    }
}
