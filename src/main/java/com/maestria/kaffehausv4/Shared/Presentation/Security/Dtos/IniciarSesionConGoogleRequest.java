package com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos;

/** Credencial de Google: el id_token que el cliente obtuvo del proveedor. */
public record IniciarSesionConGoogleRequest(String googleToken) {}
