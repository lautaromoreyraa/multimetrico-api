-- Los importes que cobra cada asesor en un mes.
--
-- Mismo criterio de portabilidad que V1 y V2: sin ENGINE, sin CHARSET y sin
-- tipos propios de un motor, porque este archivo lo corre Flyway contra MySQL 8
-- en produccion y contra H2 en modo MySQL durante los tests.

-- ---------------------------------------------------------------------------
-- El importe llega calculado, no se calcula aca
--
-- Es la decision mas vieja del proyecto y conviene no volver a discutirla: el
-- motor de incentivos se cayo del alcance al principio porque los importes
-- llegan ya resueltos desde la empresa y se adjuntan tal cual. Esta tabla guarda
-- lo que dice esa planilla y nada mas; no hay ninguna regla que derive un
-- importe de un alcance.
--
-- Consecuencia practica: no hay forma de "recalcular" incentivos. Si el numero
-- esta mal, se corrige el archivo y se vuelve a cargar.
-- ---------------------------------------------------------------------------
CREATE TABLE incentivo (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    periodo      VARCHAR(7)    NOT NULL,
    legajo       VARCHAR(40)   NOT NULL,
    legajo_clave VARCHAR(40)   NOT NULL,

    -- DECIMAL(18,2): son pesos. Dos decimales y no seis como los alcances,
    -- porque acá no hay comparaciones contra bordes de banda: es plata y se
    -- muestra como llega. La escala explicita igual, porque un DECIMAL sin ella
    -- en MySQL es DECIMAL(10,0) y redondea a entero en silencio.
    importe      DECIMAL(18,2) NOT NULL,

    -- De que carga vino. Sirve para saber que archivo trajo un numero cuando
    -- alguien lo discute.
    origen       VARCHAR(160)  NULL,

    creado_en    DATETIME(6)   NOT NULL
);

-- ---------------------------------------------------------------------------
-- Sin UNIQUE (periodo, legajo), y es a proposito
--
-- Cada carga inserta, igual que las mediciones: el importe de un mes puede
-- corregirse subiendo el archivo de nuevo, y el anterior queda. Vale la misma
-- razon que en medicion -cuando alguien pregunta por que su numero cambio, el
-- viejo tiene que estar en algun lado- y el mismo contrato: **gana el ultimo
-- cargado**, y eso se resuelve por id, nunca por fecha.
--
-- El indice esta pensado para esa consulta: todos los del periodo, en orden de
-- carga.
-- ---------------------------------------------------------------------------
CREATE INDEX ix_incentivo_periodo ON incentivo (periodo, legajo_clave, id);
