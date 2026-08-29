package com.lautaro.multimetrico.service.seguridad;

import com.lautaro.multimetrico.domain.Usuario;
import com.lautaro.multimetrico.dto.seguridad.Credencial;
import java.util.Optional;

/**
 * Emite y lee los tokens.
 *
 * <p>Es una interfaz por la misma razón que el resto de los servicios del
 * proyecto, pero acá hay una de más: el día que los tokens dejen de firmarse con
 * una clave compartida —porque entra un proveedor de identidad, por ejemplo— lo
 * único que cambia es la implementación. El filtro y el controller hablan contra
 * estos dos métodos.
 */
public interface ServicioDeTokens {

    /** El token firmado de un usuario, listo para mandar en el Authorization. */
    String emitirPara(Usuario usuario);

    /**
     * Lee un token y devuelve quién dice ser, o vacío si no se le puede creer.
     *
     * <p>Devuelve {@code Optional} y no lanza a propósito: que llegue un token
     * vencido o falsificado no es una condición excepcional, es el caso normal de
     * un endpoint público en internet. El filtro lo trata como "no autenticado" y
     * sigue; una excepción ahí obligaría a atraparla en el camino de todas las
     * requests.
     */
    Optional<Credencial> leer(String token);
}
