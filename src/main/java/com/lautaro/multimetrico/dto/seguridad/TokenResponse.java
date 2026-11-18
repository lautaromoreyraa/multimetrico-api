package com.lautaro.multimetrico.dto.seguridad;

import com.lautaro.multimetrico.enumeration.Rol;
import java.time.Instant;

public record TokenResponse(
        String token,
        String legajo,
        String nombre,
        Rol rol,
        String lider,
        Instant vence) {
}
