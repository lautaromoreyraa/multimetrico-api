-- Esquema inicial del multimetrico.
--
-- El DDL es deliberadamente portable: no lleva ENGINE, ni DEFAULT CHARSET, ni
-- tipos propios de un motor. La razon es que este mismo archivo lo corre Flyway
-- contra MySQL 8 en produccion y contra H2 en modo MySQL durante los tests. Si
-- hubiera dos archivos, el esquema que prueban los tests dejaria de ser el que
-- corre de verdad, que es exactamente lo que se quiere evitar. MySQL 8 ya usa
-- InnoDB y utf8mb4 por defecto y el compose lo fija ademas a nivel de servidor.

-- ---------------------------------------------------------------------------
-- Los periodos van como texto 'aaaa-mm'
--
-- YearMonth no tiene tipo nativo en SQL. Se guarda como el mismo texto que usa
-- la API ('2026-08') y no como entero ni como el dia 1 de un DATE: ordena
-- cronologicamente al ordenar alfabeticamente, se lee sin traducir en cualquier
-- SELECT, y no inventa un dia que el dominio no tiene. LocalDate e Instant si
-- tienen tipo nativo, asi que van en DATE y DATETIME(6).
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------------------
-- Todos los DECIMAL llevan precision y escala explicitas: DECIMAL(18,6)
--
-- Un DECIMAL sin escala en MySQL es DECIMAL(10,0), o sea que redondea a entero
-- en silencio. Todo el calculo de este proyecto compara valores contra bordes de
-- banda con compareTo, asi que un 80,04 guardado como 80 no rompe nada visible:
-- simplemente cae en otro tramo y paga distinto. Seis decimales cubren los
-- porcentajes y los segundos con margen de sobra.
-- ---------------------------------------------------------------------------

CREATE TABLE pliego (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    periodo        VARCHAR(7)   NOT NULL,
    campania       VARCHAR(120) NOT NULL,
    -- La clave normalizada (trim + mayusculas) va en su propia columna en vez de
    -- confiar en la collation: MySQL compara sin distinguir mayusculas y H2 no,
    -- y una busqueda que anda en produccion y falla en los tests -o al reves- es
    -- peor que una columna de mas.
    campania_clave VARCHAR(120) NOT NULL,
    CONSTRAINT uk_pliego_periodo_campania UNIQUE (periodo, campania_clave)
);

CREATE TABLE escala (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    pliego_id BIGINT        NOT NULL,
    metrica   VARCHAR(40)   NOT NULL,
    peso      DECIMAL(18,6) NOT NULL,
    orden     INT           NOT NULL,
    CONSTRAINT fk_escala_pliego FOREIGN KEY (pliego_id) REFERENCES pliego (id) ON DELETE CASCADE
);

CREATE INDEX ix_escala_pliego ON escala (pliego_id, orden);

CREATE TABLE banda (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    escala_id BIGINT        NOT NULL,
    -- NULL es el extremo abierto del dominio: un FCR perfecto de 100% no se cae
    -- de la escala. No se reemplaza por un centinela tipo -99999.
    desde     DECIMAL(18,6) NULL,
    hasta     DECIMAL(18,6) NULL,
    resultado DECIMAL(18,6) NOT NULL,
    orden     INT           NOT NULL,
    CONSTRAINT fk_banda_escala FOREIGN KEY (escala_id) REFERENCES escala (id) ON DELETE CASCADE
);

CREATE INDEX ix_banda_escala ON banda (escala_id, orden);

CREATE TABLE padron (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    periodo VARCHAR(7) NOT NULL,
    CONSTRAINT uk_padron_periodo UNIQUE (periodo)
);

CREATE TABLE asesor (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    padron_id BIGINT       NOT NULL,
    legajo    VARCHAR(40)  NOT NULL,
    nombre    VARCHAR(160) NOT NULL,
    lider     VARCHAR(160) NOT NULL,
    orden     INT          NOT NULL,
    CONSTRAINT fk_asesor_padron FOREIGN KEY (padron_id) REFERENCES padron (id) ON DELETE CASCADE
);

-- Sin UNIQUE (padron_id, legajo) a proposito: quien rechaza el legajo repetido
-- es el constructor de Padron, con un mensaje que nombra a las dos personas. Una
-- unique aca compararia segun la collation del motor, o sea distinto en MySQL
-- que en H2, y volveria la regla dependiente de donde corre.
CREATE INDEX ix_asesor_padron ON asesor (padron_id, orden);

CREATE TABLE alias_de_nombre (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- Ya viene normalizado por NombreNormalizado: mayusculas, sin acentos y solo
    -- A-Z 0-9 y espacio. Por eso aca si alcanza con una unique comun.
    nombre VARCHAR(200) NOT NULL,
    legajo VARCHAR(40)  NOT NULL,
    CONSTRAINT uk_alias_nombre UNIQUE (nombre)
);

CREATE INDEX ix_alias_legajo ON alias_de_nombre (legajo);

-- ---------------------------------------------------------------------------
-- El id de medicion ES el orden de carga
--
-- Dos mediciones del mismo corte son el mismo archivo subido dos veces, y la
-- segunda es la correccion: gana la ultima cargada. El desempate lo decide el
-- orden en que entraron, no la fecha de corte. Por eso el autoincremental no es
-- una clave tecnica cualquiera: es el dato que ordena la historia, y todos los
-- indices lo llevan al final para que el ORDER BY id salga del indice.
--
-- Un ORDER BY fecha_de_corte aca invierte el desempate y el multimetrico
-- empieza a mostrar el numero viejo sin que falle nada.
-- ---------------------------------------------------------------------------
CREATE TABLE medicion (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    legajo             VARCHAR(40)   NOT NULL,
    -- El legajo en mayusculas, por el mismo motivo que campania_clave: los
    -- archivos de origen escriben 'u100' y 'U100' y la busqueda no puede
    -- depender de la collation del motor.
    legajo_clave       VARCHAR(40)   NOT NULL,
    metrica            VARCHAR(40)   NOT NULL,
    periodo            VARCHAR(7)    NOT NULL,
    valor              DECIMAL(18,6) NOT NULL,
    fecha_de_corte     DATE          NOT NULL,
    -- Referencia por id a la importacion que la trajo, sin FOREIGN KEY: una
    -- medicion cargada a mano por la API no tiene origen, y borrar el historial
    -- de importaciones no puede llevarse puestas las mediciones que produjo.
    origen_importacion VARCHAR(36)   NULL
);

CREATE INDEX ix_medicion_periodo ON medicion (periodo, id);
CREATE INDEX ix_medicion_periodo_legajo ON medicion (periodo, legajo_clave, id);

CREATE TABLE perfil_de_importacion (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre            VARCHAR(120) NOT NULL,
    nombre_clave      VARCHAR(120) NOT NULL,
    metrica           VARCHAR(40)  NOT NULL,
    hoja              VARCHAR(160) NULL,
    fila_encabezados  INT          NOT NULL,
    columna_asesor    VARCHAR(160) NOT NULL,
    columna_valor     VARCHAR(160) NOT NULL,
    columna_lider     VARCHAR(160) NULL,
    separador_decimal VARCHAR(20)  NOT NULL,
    escala_de_numero  VARCHAR(20)  NOT NULL,
    CONSTRAINT uk_perfil_nombre UNIQUE (nombre_clave)
);

-- ---------------------------------------------------------------------------
-- El informe de una importacion no se desarma en tablas
--
-- Rechazos, pendientes, discrepancias y desajustes son las cuatro listas de un
-- informe que se lee siempre entero y de una sola importacion: nadie pregunta
-- "todos los rechazos por valor no numerico del ano". Cuatro tablas mas cuatro
-- joins para eso solo compran la posibilidad de una consulta que el producto no
-- tiene, y a cambio congelan la forma de un reporte que va a cambiar cada vez
-- que aparezca un motivo nuevo.
--
-- Lo que si son columnas es lo que se consulta o se agrega: estado, periodo,
-- metrica, y los dos contadores del informe. La invariante "importadas +
-- rechazadas + pendientes == filas leidas" la reconstruye el record Informe al
-- leer, y revienta si el JSON y los contadores no coinciden.
-- ---------------------------------------------------------------------------
CREATE TABLE importacion (
    id              VARCHAR(36) PRIMARY KEY,
    perfil          VARCHAR(120) NOT NULL,
    metrica         VARCHAR(40)  NOT NULL,
    periodo         VARCHAR(7)   NOT NULL,
    fecha_de_corte  DATE         NOT NULL,
    estado          VARCHAR(20)  NOT NULL,
    filas_leidas    INT          NOT NULL,
    importadas      INT          NOT NULL,
    rechazadas_json LONGTEXT     NULL,
    pendientes_json LONGTEXT     NULL,
    discrepancias_json LONGTEXT  NULL,
    desajustes_json LONGTEXT     NULL,
    iniciada        DATETIME(6)  NOT NULL,
    terminada       DATETIME(6)  NULL
);

CREATE INDEX ix_importacion_iniciada ON importacion (iniciada);
CREATE INDEX ix_importacion_periodo ON importacion (periodo);
