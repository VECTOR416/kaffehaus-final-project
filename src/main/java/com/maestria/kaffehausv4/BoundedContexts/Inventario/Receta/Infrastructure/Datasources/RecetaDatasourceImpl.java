package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Datasources;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Domain.ValueObjects.ProductoId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Datasources.IRecetaDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.ObtenerRecetasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.PasoReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.IngredienteRecetaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.IngredienteRecetaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.IngredienteRecetaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.PasoRecetaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.PasoRecetaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.PasoRecetaJpaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.RecetaJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.RecetaJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa.RecetaJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

/**
 * Persistencia del agregado Receta completo: encabezado, ingredientes y pasos.
 *
 * Maneja los tres JpaRepository porque las tres tablas pertenecen al mismo agregado; esto no
 * contradice la regla de no acoplar datasources entre agregados, ya que ingredientes_receta y
 * pasos_receta dejaron de ser agregados. La atomicidad la aporta la @Transactional del caso de
 * uso, que envuelve todas las escrituras en una sola transaccion de base de datos.
 *
 * Aqui no hay DELETE: el sistema no usa borrado fisico. Las bajas son UPDATE de 'borrado'.
 */
@Repository
public class RecetaDatasourceImpl implements IRecetaDatasource {

    private final RecetaJpaRepository recetaJpaRepository;
    private final IngredienteRecetaJpaRepository ingredienteRecetaJpaRepository;
    private final PasoRecetaJpaRepository pasoRecetaJpaRepository;

    public RecetaDatasourceImpl(
            RecetaJpaRepository recetaJpaRepository,
            IngredienteRecetaJpaRepository ingredienteRecetaJpaRepository,
            PasoRecetaJpaRepository pasoRecetaJpaRepository) {
        this.recetaJpaRepository = recetaJpaRepository;
        this.ingredienteRecetaJpaRepository = ingredienteRecetaJpaRepository;
        this.pasoRecetaJpaRepository = pasoRecetaJpaRepository;
    }

    @Override
    public Receta crear(Receta receta) {
        RecetaJpaModel model = RecetaJpaMapper.toPersistence(receta);
        RecetaJpaModel modelGuardado = recetaJpaRepository.save(model);

        List<IngredienteRecetaJpaModel> ingredientes = ingredienteRecetaJpaRepository.saveAll(
                receta.getIngredientes().stream()
                        .map(ingrediente -> IngredienteRecetaJpaMapper.toPersistence(ingrediente, modelGuardado.getId()))
                        .collect(Collectors.toList()));

        List<PasoRecetaJpaModel> pasos = pasoRecetaJpaRepository.saveAll(
                receta.getPasos().stream()
                        .map(paso -> PasoRecetaJpaMapper.toPersistence(paso, modelGuardado.getId()))
                        .collect(Collectors.toList()));

        return RecetaJpaMapper.toDomain(modelGuardado, ingredientes, pasos);
    }

    @Override
    public Receta actualizar(RecetaId recetaId, Receta receta) {
        UUID id = aUuid(recetaId.getValue());

        RecetaJpaModel model = recetaJpaRepository.findByIdAndBorradoFalse(id)
                .orElseThrow(() -> DomainException.throwException(404, "La receta con id '" + recetaId.getValue() + "' no existe"));

        RecetaJpaMapper.copiarCambios(receta, model);
        RecetaJpaModel modelGuardado = recetaJpaRepository.save(model);

        List<IngredienteRecetaJpaModel> ingredientes = ingredienteRecetaJpaRepository.saveAll(
                receta.getIngredientes().stream()
                        .map(ingrediente -> conciliarIngrediente(ingrediente, id))
                        .collect(Collectors.toList()));

        List<PasoRecetaJpaModel> pasos = guardarPasosRenumerados(receta.getPasos(), id);

        return RecetaJpaMapper.toDomain(modelGuardado, ingredientes, pasos);
    }

    /**
     * Guarda los pasos en dos fases.
     *
     * El indice unico parcial uq_receta_paso impide que dos pasos vivos de la misma receta
     * compartan numero, y una renumeracion colisiona consigo misma a medio camino: al pasar
     * (1,2,3) a (2,3,1) el UPDATE que escribe un 2 choca con el 2 que aun no se ha movido.
     * Por eso los pasos se aparcan primero en numeros negativos, que ningun paso vivo usa, se
     * fuerza el flush y despues se escriben los definitivos.
     */
    private List<PasoRecetaJpaModel> guardarPasosRenumerados(List<PasoReceta> pasos, UUID idReceta) {
        List<PasoRecetaJpaModel> modelos = pasos.stream()
                .map(paso -> conciliarPaso(paso, idReceta))
                .collect(Collectors.toList());

        for (int i = 0; i < modelos.size(); i++) {
            PasoRecetaJpaMapper.aparcarNumeroDePaso(modelos.get(i), i);
        }
        pasoRecetaJpaRepository.saveAll(modelos);
        pasoRecetaJpaRepository.flush();

        for (int i = 0; i < modelos.size(); i++) {
            PasoRecetaJpaMapper.copiarCambios(pasos.get(i), modelos.get(i));
        }

        return pasoRecetaJpaRepository.saveAll(modelos);
    }

    private IngredienteRecetaJpaModel conciliarIngrediente(IngredienteReceta ingrediente, UUID idReceta) {
        if (ingrediente.getId() == null) {
            return IngredienteRecetaJpaMapper.toPersistence(ingrediente, idReceta);
        }

        UUID idIngrediente = aUuid(ingrediente.getId().getValue());
        IngredienteRecetaJpaModel model = ingredienteRecetaJpaRepository.findById(idIngrediente)
                .orElseThrow(() -> DomainException.throwException(404,
                        "El ingrediente con id '" + ingrediente.getId().getValue() + "' no existe"));

        IngredienteRecetaJpaMapper.copiarCambios(ingrediente, model);

        return model;
    }

    private PasoRecetaJpaModel conciliarPaso(PasoReceta paso, UUID idReceta) {
        if (paso.getId() == null) {
            return PasoRecetaJpaMapper.toPersistence(paso, idReceta);
        }

        UUID idPaso = aUuid(paso.getId().getValue());
        PasoRecetaJpaModel model = pasoRecetaJpaRepository.findById(idPaso)
                .orElseThrow(() -> DomainException.throwException(404,
                        "El paso con id '" + paso.getId().getValue() + "' no existe"));

        PasoRecetaJpaMapper.copiarCambios(paso, model);

        return model;
    }

    @Override
    public Optional<Receta> obtenerPorId(RecetaId recetaId) {
        UUID id = aUuid(recetaId.getValue());

        return recetaJpaRepository.findByIdAndBorradoFalse(id)
                .map(model -> RecetaJpaMapper.toDomain(
                        model,
                        ingredienteRecetaJpaRepository.findByIdRecetaAndBorradoFalse(id),
                        pasoRecetaJpaRepository.findByIdRecetaAndBorradoFalseOrderByPasosAsc(id)));
    }

    @Override
    public PaginatedResponse<List<Receta>> obtenerTodos(ObtenerRecetasRequest obtenerRecetasRequest) {
        int skip = obtenerRecetasRequest.getSkip();
        int limit = obtenerRecetasRequest.getLimit();
        String search = obtenerRecetasRequest.getSearch();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<RecetaJpaModel> pagina = (search == null || search.trim().isEmpty())
                ? recetaJpaRepository.findByBorradoFalse(pageable)
                : recetaJpaRepository.findByBorradoFalseAndTituloContainingIgnoreCase(search.trim(), pageable);

        List<UUID> idsReceta = pagina.getContent().stream()
                .map(RecetaJpaModel::getId)
                .collect(Collectors.toList());

        Map<UUID, List<IngredienteRecetaJpaModel>> ingredientesPorReceta = idsReceta.isEmpty()
                ? Collections.emptyMap()
                : ingredienteRecetaJpaRepository.findByIdRecetaInAndBorradoFalse(idsReceta).stream()
                        .collect(Collectors.groupingBy(IngredienteRecetaJpaModel::getIdReceta));

        Map<UUID, List<PasoRecetaJpaModel>> pasosPorReceta = idsReceta.isEmpty()
                ? Collections.emptyMap()
                : pasoRecetaJpaRepository.findByIdRecetaInAndBorradoFalseOrderByPasosAsc(idsReceta).stream()
                        .collect(Collectors.groupingBy(PasoRecetaJpaModel::getIdReceta));

        List<Receta> datos = pagina.getContent().stream()
                .map(model -> RecetaJpaMapper.toDomain(
                        model,
                        ingredientesPorReceta.getOrDefault(model.getId(), Collections.emptyList()),
                        pasosPorReceta.getOrDefault(model.getId(), Collections.emptyList())))
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
    public void eliminar(RecetaId recetaId) {
        UUID id = aUuid(recetaId.getValue());

        RecetaJpaModel model = recetaJpaRepository.findByIdAndBorradoFalse(id)
                .orElseThrow(() -> DomainException.throwException(404, "La receta con id '" + recetaId.getValue() + "' no existe"));

        // La baja la decide el agregado: se reconstruye entero, se le pide eliminar y se
        // escriben el encabezado y las dos colecciones que el marco.
        Receta receta = RecetaJpaMapper.toDomain(
                model,
                ingredienteRecetaJpaRepository.findByIdRecetaAndBorradoFalse(id),
                pasoRecetaJpaRepository.findByIdRecetaAndBorradoFalseOrderByPasosAsc(id));
        receta.eliminar();

        RecetaJpaMapper.copiarCambios(receta, model);
        recetaJpaRepository.save(model);

        ingredienteRecetaJpaRepository.saveAll(
                receta.getIngredientes().stream()
                        .map(ingrediente -> conciliarIngrediente(ingrediente, id))
                        .collect(Collectors.toList()));

        pasoRecetaJpaRepository.saveAll(
                receta.getPasos().stream()
                        .map(paso -> conciliarPaso(paso, id))
                        .collect(Collectors.toList()));
    }

    @Override
    public Optional<Receta> obtenerPorProductoObtenido(ProductoId productoId) {
        return recetaJpaRepository.findFirstByIdProductoObtenidoAndBorradoFalseOrderByFecCDesc(aUuid(productoId.getValue()))
                .flatMap(model -> obtenerPorId(new RecetaId(model.getId().toString())));
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }

    @Override
    public List<Receta> obtenerPorIngrediente(ProductoId productoId) {
        List<UUID> idsReceta = ingredienteRecetaJpaRepository.findByIdProductoAndBorradoFalse(aUuid(productoId.getValue())).stream()
                .map(IngredienteRecetaJpaModel::getIdReceta)
                .distinct()
                .collect(Collectors.toList());
        return idsReceta.stream()
                .map(idReceta -> obtenerPorId(new RecetaId(idReceta.toString())))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<Receta> obtenerVivasPorProductoObtenido(ProductoId productoId) {
        return recetaJpaRepository.findByIdProductoObtenidoAndBorradoFalseOrderByFecCDesc(aUuid(productoId.getValue())).stream()
                .map(model -> obtenerPorId(new RecetaId(model.getId().toString())))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }
}
