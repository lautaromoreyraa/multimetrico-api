package com.lautaro.multimetrico.service.seguridad;

import com.lautaro.multimetrico.domain.Usuario;
import com.lautaro.multimetrico.dto.seguridad.Credencial;
import java.util.Optional;

public interface ServicioDeTokens {

    String emitirPara(Usuario usuario);

    Optional<Credencial> leer(String token);

    long minutosDeVigencia();
}
