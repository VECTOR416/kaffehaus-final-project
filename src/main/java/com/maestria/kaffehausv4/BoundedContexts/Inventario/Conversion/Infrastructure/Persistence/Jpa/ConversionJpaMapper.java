package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Domain.Aggregates.Conversion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;

public class ConversionJpaMapper {
    private ConversionJpaMapper() {
    }

    public static ConversionJpaModel toPersistence(Conversion conversion) {
        ConversionJpaModel model = new ConversionJpaModel();
        model.setId(conversion.getId() != null ? UUID.fromString(conversion.getId().getValue()) : UUID.randomUUID());
        model.setFactorConversion(conversion.getFactorConversion());
        model.setEsBorrable(conversion.isEsBorrable());
        model.setIdUnidadOrigen(UUID.fromString(conversion.getUnidadOrigenId().getValue()));
        model.setIdUnidadDestino(UUID.fromString(conversion.getUnidadDestinoId().getValue()));
        model.setBorrado(conversion.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Conversion conversion, ConversionJpaModel model) {
        model.setFactorConversion(conversion.getFactorConversion());
        model.setEsBorrable(conversion.isEsBorrable());
        model.setIdUnidadOrigen(UUID.fromString(conversion.getUnidadOrigenId().getValue()));
        model.setIdUnidadDestino(UUID.fromString(conversion.getUnidadDestinoId().getValue()));
        model.setBorrado(conversion.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Conversion toDomain(ConversionJpaModel model) {
        return Conversion.restaurar(
                model.getId().toString(),
                model.getFactorConversion(),
                model.getEsBorrable(),
                new UnidadId(model.getIdUnidadOrigen().toString()),
                new UnidadId(model.getIdUnidadDestino().toString()),
                model.getBorrado() != null && model.getBorrado());
    }
}
