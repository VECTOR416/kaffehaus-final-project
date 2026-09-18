package com.maestria.kaffehausv4.Shared.Presentation.Security.Dtos;

/** Credenciales del login clasico. Los campos se llaman en ingles porque asi los fija el contrato de la API. */
public record IniciarSesionRequest(String email, String password) {}
