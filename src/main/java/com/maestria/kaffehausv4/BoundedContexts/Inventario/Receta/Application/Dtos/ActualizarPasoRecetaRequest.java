package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos;

import java.util.Map;

import com.maestria.kaffehausv4.Shared.Application.Validation.Validador;

/**
 * Paso del payload de modificacion de una receta. Viaja anidado dentro de ActualizarRecetaRequest.
 *
 * El campo 'id' decide la operacion: si viene, el paso existente se corrige; si no, se
 * incorpora uno nuevo y entonces 'descripcion' pasa a ser obligatoria.
 *
 * Tampoco aqui se admite el numero de paso: el orden del array ES la secuencia. Mandar los
 * pasos en otro orden los reordena; omitir uno lo da de baja.
 */
public class ActualizarPasoRecetaRequest {
    private String id;
    private String descripcion;

    private ActualizarPasoRecetaRequest(
        String id,
        String descripcion
    ) {
        this.id = id;
        this.descripcion = descripcion;
    }

    public static ActualizarPasoRecetaRequest createFromRequest(Map<String, Object> request){
        String id = Validador.textoOpcional(request.get("id"), "id");
        String descripcion = Validador.textoOpcional(request.get("descripcion"), "descripcion");

        return new ActualizarPasoRecetaRequest(id, descripcion);
    }

    public String getId() {return id;}
    public String getDescripcion() {return descripcion;}
}
