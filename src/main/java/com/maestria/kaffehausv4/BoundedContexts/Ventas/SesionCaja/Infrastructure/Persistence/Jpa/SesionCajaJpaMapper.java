package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.MovimientoCaja;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.SesionCaja;

/** toDomain reconstruye la raiz junto con sus movimientos: no hay sesion de caja a medias. */
public class SesionCajaJpaMapper {
    private SesionCajaJpaMapper() {
    }

    public static SesionCajaJpaModel toPersistence(SesionCaja sesionCaja) {
        SesionCajaJpaModel model = new SesionCajaJpaModel();
        model.setId(sesionCaja.getId() != null ? UUID.fromString(sesionCaja.getId().getValue()) : UUID.randomUUID());
        model.setIdUsuario(UUID.fromString(sesionCaja.getUsuarioId().getValue()));
        model.setFechaApertura(sesionCaja.getFechaApertura());
        model.setMontoApertura(sesionCaja.getMontoApertura());
        model.setEstado(sesionCaja.getEstado());
        copiarCierre(sesionCaja, model);
        model.setBorrado(sesionCaja.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(SesionCaja sesionCaja, SesionCajaJpaModel model) {
        model.setEstado(sesionCaja.getEstado());
        copiarCierre(sesionCaja, model);
        model.setBorrado(sesionCaja.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    private static void copiarCierre(SesionCaja sesionCaja, SesionCajaJpaModel model) {
        model.setFechaCierre(sesionCaja.getFechaCierre());
        model.setMontoCierreDeclarado(sesionCaja.getMontoCierreDeclarado());
        model.setMontoCierreEsperado(sesionCaja.getMontoCierreEsperado());
        model.setDiferencia(sesionCaja.getDiferencia());
        model.setMotivoDiferencia(sesionCaja.getMotivoDiferencia());
    }

    public static SesionCaja toDomain(SesionCajaJpaModel model, List<MovimientoCajaJpaModel> movimientosModel) {
        List<MovimientoCaja> movimientos = movimientosModel != null
                ? movimientosModel.stream().map(MovimientoCajaJpaMapper::toDomain).collect(Collectors.toList())
                : Collections.emptyList();

        return SesionCaja.restaurar(
                model.getId().toString(),
                new UsuarioId(model.getIdUsuario().toString()),
                model.getFechaApertura(),
                model.getFechaCierre(),
                model.getMontoApertura(),
                model.getMontoCierreDeclarado(),
                model.getMontoCierreEsperado(),
                model.getDiferencia(),
                model.getMotivoDiferencia(),
                model.getEstado(),
                model.getBorrado() != null && model.getBorrado(),
                movimientos);
    }
}
