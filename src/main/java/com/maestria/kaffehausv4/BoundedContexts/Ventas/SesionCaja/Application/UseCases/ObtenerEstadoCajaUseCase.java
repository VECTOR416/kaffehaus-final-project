package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.UseCases;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Application.Repositories.IUsuarioRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Dtos.EstadoCajaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Application.Repositories.ISesionCajaRepository;

/**
 * Si hay un turno abierto, desde cuando, el nombre de su responsable y si se abrio un dia anterior
 * (logica.md SESIONCAJA-08, SESIONCAJA-17).
 */
@Service
@Transactional(readOnly = true)
public class ObtenerEstadoCajaUseCase {
    private final ISesionCajaRepository sesionCajaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IPersonaRepository personaRepository;

    public ObtenerEstadoCajaUseCase(
            ISesionCajaRepository sesionCajaRepository,
            IUsuarioRepository usuarioRepository,
            IPersonaRepository personaRepository) {
        this.sesionCajaRepository = sesionCajaRepository;
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
    }

    public EstadoCajaResponse execute() {
        LocalDate hoy = LocalDate.now();
        return sesionCajaRepository.obtenerAbierta()
                .map(sesionCaja -> EstadoCajaResponse.createFromAggregate(sesionCaja,
                        usuarioRepository.obtenerPorId(sesionCaja.getUsuarioId())
                                .flatMap(usuario -> personaRepository.obtenerPorId(usuario.getPersonaId()))
                                .map(persona -> persona.getNombres() + " " + persona.getApellidos())
                                .orElse(null),
                        hoy))
                .orElseGet(() -> EstadoCajaResponse.createFromAggregate(null, null, hoy));
    }
}
