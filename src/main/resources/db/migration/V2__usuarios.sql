-- Usuarios y roles.
--
-- Mismo criterio de portabilidad que V1: sin ENGINE, sin CHARSET y sin tipos
-- propios de un motor, porque este archivo lo corre Flyway contra MySQL 8 en
-- produccion y contra H2 en modo MySQL durante los tests.

-- ---------------------------------------------------------------------------
-- El legajo es la identidad, y es el mismo que el del padron
--
-- No se agrega un "username" aparte. La pregunta que la aplicacion tiene que
-- contestar en cada request es "de quien es esta fila", y esa se contesta con el
-- legajo; un identificador distinto obligaria a traducir en cada control de
-- acceso, que es exactamente donde no conviene tener un paso de mas.
--
-- legajo_clave existe por la misma razon que en medicion: la comparacion se hace
-- sobre una columna ya normalizada en Java (mayusculas) en vez de depender de la
-- collation del motor, que no es la misma en MySQL que en H2.
-- ---------------------------------------------------------------------------
CREATE TABLE usuario (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    legajo       VARCHAR(40)  NOT NULL,
    legajo_clave VARCHAR(40)  NOT NULL,
    nombre       VARCHAR(160) NOT NULL,

    -- BCrypt produce 60 caracteres. Se deja 100 por si algun dia se migra a otro
    -- algoritmo: el prefijo {id} de Spring Security ocupa lugar.
    password     VARCHAR(100) NOT NULL,

    -- ASESOR, SUPERVISOR o ADMIN. Se guarda el nombre y no el ordinal, igual que
    -- todos los enums del proyecto: el ordinal cambia de significado si alguien
    -- agrega un rol en el medio de la lista.
    rol          VARCHAR(20)  NOT NULL,

    -- Solo para SUPERVISOR: de que equipo es lider. Es el mismo texto que
    -- asesor.lider, que es como el padron nombra a los lideres.
    lider        VARCHAR(160) NULL,

    activo       BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en    DATETIME(6)  NOT NULL,

    CONSTRAINT uk_usuario_legajo UNIQUE (legajo_clave)
);
