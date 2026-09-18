package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.PasoReceta;

/**
 * Mapea los pasos del agregado Receta.
 *
 * toPersistence recibe el id de la receta por parametro: la entidad ya no guarda la referencia
 * al padre, porque dentro del agregado la relacion es la contencion.
 */
public class PasoRecetaJpaMapper {
    private PasoRecetaJpaMapper() {
    }

    public static PasoRecetaJpaModel toPersistence(PasoReceta pasoReceta, UUID idReceta) {
        PasoRecetaJpaModel model = new PasoRecetaJpaModel();
        model.setId(pasoReceta.getId() != null ? UUID.fromString(pasoReceta.getId().getValue()) : UUID.randomUUID());
        model.setPasos(pasoReceta.getPasos());
        model.setDescripcion(pasoReceta.getDescripcion());
        model.setIdReceta(idReceta);
        model.setBorrado(pasoReceta.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(PasoReceta pasoReceta, PasoRecetaJpaModel model) {
        model.setPasos(pasoReceta.getPasos());
        model.setDescripcion(pasoReceta.getDescripcion());
        model.setBorrado(pasoReceta.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    /** Aparca el paso en un numero negativo, que ningun paso vivo usa. Ver RecetaDatasourceImpl. */
    public static void aparcarNumeroDePaso(PasoRecetaJpaModel model, int posicion) {
        model.setPasos(-(posicion + 1));
    }

    public static PasoReceta toDomain(PasoRecetaJpaModel model) {
        return PasoReceta.restaurar(
                model.getId().toString(),
                model.getPasos(),
                model.getDescripcion(),
                model.getBorrado() != null && model.getBorrado());
    }
}
