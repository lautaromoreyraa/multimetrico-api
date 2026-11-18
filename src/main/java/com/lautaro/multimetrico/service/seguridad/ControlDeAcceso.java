package com.lautaro.multimetrico.service.seguridad;

import com.lautaro.multimetrico.dto.seguridad.Credencial;
import java.util.Optional;

public interface ControlDeAcceso {

    Optional<Credencial> quienPide();
}
