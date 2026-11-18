package com.lautaro.multimetrico.dto.seguridad;

import com.lautaro.multimetrico.enumeration.Rol;

public record UsuarioResponse(String legajo, String nombre, Rol rol, String lider, boolean activo) {
}
