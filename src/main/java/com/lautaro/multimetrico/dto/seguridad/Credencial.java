package com.lautaro.multimetrico.dto.seguridad;

import com.lautaro.multimetrico.enumeration.Rol;

public record Credencial(String legajo, Rol rol, String lider) {

    public boolean esAdmin() {
        return rol == Rol.ADMIN;
    }

    public boolean esSupervisor() {
        return rol == Rol.SUPERVISOR;
    }
}
