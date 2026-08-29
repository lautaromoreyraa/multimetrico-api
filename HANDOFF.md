# Handoff — multimetrico-api

Estado vivo del proyecto. El **qué** y el **por qué** están en `PLAN.md`; acá va
el **cómo viene** y, sobre todo, **qué no volver a romper**.

Última actualización: 2026-08-28, noche.

**El backend está completo y subido.** Entró la seguridad, y con eso el alcance
original quedó cerrado.

Leer es público y escribir pide sesión. Las tres pantallas de carga viven ahora
en un **panel de administración**.

Antes de tocar el multimétrico, leé "Reglas de producto". Varias invierten lo que
el código defendía hace dos días, y los comentarios viejos ya no están pero la
intuición sigue tirando para el otro lado.

---

## Semáforo

| Módulo | Estado | Tests | Notas |
|---|---|---|---|
| `pliego` | ✅ | 30 | Motor de tramos, validación y API |
| `padron` | ✅ | 24 | Resolución de nombres y aprendizaje de alias |
| `medicion` | ✅ | 46 | Historia por corte; el vigente es el corte más reciente |
| `multimetrico` | ✅ | (idem) | Incluye el tramo de arriba, accionable |
| `importacion` | ✅ | 8 | Perfil configurable; ninguna fila se pierde |
| `persistencia` | ✅ | 24 | MySQL + Flyway; los tests corren contra H2 en modo MySQL |
| `seguridad` | ✅ | 13 | JWT. Leer es público, escribir es de ADMIN |
| frontend | ✅ | **0** | `../multimetrico-frontend`. Cinco pantallas, sin mocks, revisadas en el navegador. Sin un solo test |

**147 tests, en verde.**

## Los dos repos

Los dos son **privados** en GitHub, rama `master`:

- `https://github.com/lautaromoreyraa/multimetrico-api`
- `https://github.com/lautaromoreyraa/multimetrico-frontend`

Dos convenciones que hay que respetar:

- **Los mensajes de commit no llevan firma de agente.** Ninguna. La historia se
  limpió a mano una vez con `filter-branch`; no la vuelvas a ensuciar.
- **`credential.helper = manager` está configurado sólo en estos dos repos**, no
  en la config global. El primer push de una sesión abre el navegador para
  autorizar.

---

## Cómo correrlo

```bash
docker compose up -d               # MySQL en el 3307
./mvnw spring-boot:run             # la API en el 8082
node scripts/datos-de-ejemplo.mjs  # los datos, sin esto la pantalla da 404

cd ../multimetrico-frontend && npm run dev   # la pantalla, en el 5174
```

`./mvnw test` corre los 147 y **no necesita la base levantada**: usan H2 en modo
de compatibilidad MySQL con las mismas migraciones de Flyway. La aplicación sí la
necesita.

**La configuración pasó de YAML a `.properties`** y se partió en dos:
`application.properties` no tiene ni un valor concreto —todo es `${VAR:default}`—
y los valores de la máquina viven en `application-local.properties`, que **no se
commitea**. Hay un `.example` al lado; copialo la primera vez.

Sin `JWT_SECRETO` (o su equivalente en el archivo local) **la aplicación no
arranca**, a propósito. Ver "Quién ve qué".

Cosas que cuestan un rato si no las sabés:

- **Docker Desktop tiene que estar levantado**, no sólo instalado. Sin él,
  `compose up` falla con un error de *named pipe* que no dice eso.
- El contenedor puede existir apagado de una sesión anterior: si `compose up`
  protesta que el nombre está en uso, `docker start multimetrico-mysql` lo revive
  **con sus datos**. `docker compose down -v` lo borra de cero.
- Vite está fijado al **5174** en `vite.config.ts`. Si ese puerto está ocupado
  toma el siguiente y lo imprime al arrancar; mirá la salida antes de asumir.
- **El frontend recuerda el recorte en `localStorage`**, bajo
  `multimetrico.contexto`. Si la primera pantalla abre filtrada por un equipo o
  en un mes raro, es eso y no un bug. Se limpia borrando esa clave.

### Qué deja el seed

De **enero al mes en curso**, campaña `CONVERGENTE`, con su pliego y su padrón
por mes. Dos equipos: **MARTINA REYES** (ocho) y **DIEGO SALAZAR** (siete); son
los dos únicos líderes y con cualquier otro la API responde 404 listando los que
sí existen.

Acepta un rango: `node scripts/datos-de-ejemplo.mjs 2026-01 2026-12`. Por defecto
no pasa del mes actual, porque más allá serían mediciones con una fecha de corte
que todavía no ocurrió.

Tres decisiones del seed que **no son ruido** y que conviene no "simplificar":

- **Cuatro cargas por métrica y por mes**, una por semana. Es lo que le da algo
  que mostrar al historial intra-mes; con una sola carga la serie tiene un punto
  y no cuenta nada. La última semana **es** el valor de cierre, exacto: las
  anteriores se sacuden alrededor y se le acercan, así el número que finalmente
  cuenta no depende de cuántas semanas se cargaron.
- **Los valores salen de un pseudoazar sembrado por legajo y mes**, y cada asesor
  tiene una línea de base propia que pesa el doble que el movimiento del mes. Si
  cada mes fuera un sorteo independiente nadie "vendría mejorando" y la
  comparación contra otro mes no diría nada.
- **Tres personas sin archivo en el último mes**, salteadas en las cuatro semanas
  y no sólo en la última. Ver "Un corte por métrica" abajo: si aparecieran en las
  primeras y faltaran en la última, su vigente sería el corte viejo y la columna
  terminaría con dos fechas conviviendo.

**Las mediciones no tienen campaña.** `Medicion` se identifica por período,
legajo y métrica, así que las mediciones de una campaña son **las mismas** que
lee el multimétrico de otra en el mismo mes. Dos seeds con campañas distintas
sobre el mismo período se pisan y gana el corte más reciente. Si vas a sembrar
otra campaña, usá otro período o vaciá la base. Esto costó una hora.

---

## Por dónde seguir

**1. El deploy**, que quedó a medias: la configuración está lista y verificada
—ver `DEPLOY.md`— pero nadie corrió `railway up` todavía.

**2. El corte único por métrica no está validado en el backend.** Ver abajo. Hoy
lo garantiza el seed y nada más.

**3. El frontend no tiene un solo test.** No hay infraestructura montada.

**4. No hay pantalla para crear usuarios.** Se hace con `POST /api/usuarios`
desde una cuenta de administrador.

**5. La deuda de la lista de más abajo.**

---

## Reglas de producto

Las tres primeras cambiaron hace poco e invierten lo que el código defendía. Las
demás vienen de más atrás. Todas están acá porque la intuición tira para el otro
lado y alguien va a querer "arreglarlas" de vuelta.

### Una métrica sin archivo bonifica cero

El proyecto defendía lo contrario —"un 9% con dos archivos sin llegar no es lo
mismo que un 9% completo"— y por eso la fila quedaba marcada incompleta y salía
de los promedios. Operativamente no se sostiene: si al día 10 no llegó el NPS, el
NPS no bonifica y esa persona cobra lo que cobra.

**El cálculo nunca hizo falta tocarlo**: `alcanceTotal` ya sumaba sólo lo
presente, que es idéntico a sumar cero por lo ausente. Lo que estaba mal era la
lectura alrededor.

Lo que **sí** se mantiene es que `faltantes` viaje aparte de `alcances`. Es lo
único que distingue "no hay archivo" de "hay un valor que paga cero", y una vez
aplastados no se separan más. En la pantalla eso se ve como el valor vacío y el
cero sólo en la columna de lo que paga: escribir un 0 en la columna del valor
diría que se midió cero, que es una afirmación sobre el desempeño de alguien que
nadie hizo.

### Leer es público, escribir pide sesión

**La línea está en el verbo, no en el recurso.** Todos los `GET` son públicos y
todo lo que modifica datos pide una cuenta de `ADMIN`. La única excepción es
`/api/usuarios`: listarlos también es leer, pero es leer quién tiene acceso.

**Qué se resignó.** El PLAN §5.6 pedía control **por fila** —que un asesor viera
sólo la suya— y llegó a estar implementado. Se sacó cuando la lectura pasó a ser
pública: hoy cualquiera que tenga la URL ve la tabla entera. A cambio, la pantalla
se muestra sin pedirle nada a nadie.

Lo que sostiene esa decisión es que **los datos son sintéticos**. El día que
entren números de una operación real, esto tiene que volver a cerrarse. No es
rehacerlo: `Rol` sigue distinguiendo ASESOR de SUPERVISOR, `Usuario` sigue
guardando el equipo, y el control era una llamada desde `MultimetricoServiceImpl`
antes de leer nada. Está anotado en el javadoc de `ControlDeAcceso`.

> **El orden de las reglas importa y es fácil de romper.** Spring evalúa de
> arriba abajo y se queda con la primera que coincide, así que
> `/api/usuarios/**` tiene que ir **antes** del `permitAll` de los GET. Cuando
> estaban al revés, listar usuarios quedaba abierto. Hay un test que lo afirma, y
> es el que lo encontró.

**Tres decisiones que parecen de más y no lo son:**

1. **El secreto de firma no tiene default.** Con default, el día que alguien se
   olvida de configurarlo en producción no se entera: los tokens se firman con la
   clave que está en el repositorio y cualquiera se fabrica uno de administrador.
   Sin `JWT_SECRETO` la aplicación no arranca y dice exactamente qué hacer.
2. **El login no dice qué falló.** Legajo inexistente y contraseña equivocada dan
   el mismo mensaje, y además se compara contra un hash de descarte cuando el
   usuario no existe, para que las dos respuestas tarden parecido. Sin eso, la
   diferencia de tiempo dice lo mismo que el mensaje que se decidió no dar.
3. **El admin inicial se crea desde el entorno, no desde una migración.** Una
   migración tendría que traer el hash escrito en un archivo que va al
   repositorio. Sin `ADMIN_PASSWORD` no se crea nada y se avisa en el log: un
   `admin/admin` por defecto es peor que una base a la que todavía no se entra.

**Lo que el token trae y lo que eso implica.** Rol y equipo viajan firmados, así
que no hay una consulta por request. La contracara: si a alguien le cambian el rol
o el equipo, su token viejo sigue diciendo lo anterior hasta que vence (ocho
horas). Es un riesgo asumido; si hace falta revocar en el momento, `Credencial`
pasa a leerse de la base.

### Un corte por métrica, varias cargas por mes

Cada métrica llega en su propio archivo, ese archivo trae a **todo el equipo**
con el mismo corte, y el día varía entre métricas según cuándo lo mande la
empresa. Por eso dentro de una columna no puede haber dos fechas conviviendo.

Que haya varias cargas por mes no lo contradice: cada carga semanal cubre a todos,
así que la vigente de todos es la del último archivo. **Lo que rompe la propiedad
es una carga que cubra sólo a una parte del equipo.**

> **No está validado.** `ResumenDeEquipo.cortesPorMetrica` sigue siendo un
> `Set<LocalDate>` y la pantalla toma el primero. Si una importación real parte
> una métrica en dos cortes, la pantalla muestra uno solo **sin avisar**.
> Convertirlo en una validación de verdad es trabajo pendiente.

### El código de color es uno solo

Vive en `frontend/src/lib/tono.ts` y se aplica en cuatro lugares: el aporte de
cada métrica, el total de la fila, el promedio del equipo y **lo que paga cada
banda en el editor de pliego**.

| estado | color |
|---|---|
| suma | verde |
| cero | ámbar |
| resta | rojo |

El cero va en ámbar y no en gris porque no es la ausencia de un dato: es un
resultado, y encima el que conviene ir a mirar. **El color nunca viaja solo** —el
cero se escribe `0,0%` y el negativo lleva su signo—, así que los tres estados se
distinguen sin depender de separarlos por color.

### Las de antes

**El prefijo de un nombre no exige borde de palabra.** Al principio sí lo exigía y
un test lo tumbó: el truncado real corta a mitad de palabra (`AGUIRRE FLAVIA Y`),
así que la regla rechazaba justo el caso más común. Lo que da seguridad es la
unicidad, no dónde cae el corte.

**Empate de fecha de corte: gana la última cargada.** Dos mediciones del mismo
corte son el mismo archivo subido dos veces, y la segunda es la corrección.
Quedarse con la primera anularía justo la corrección que alguien subió. Esto hace
del *orden de carga* parte del contrato: la consulta ordena por id, **nunca** por
fecha de corte.

**Y hay que defenderlo con más de un test.** Se probó rompiendo el `ORDER BY id` a
propósito y el test del empate de corte **siguió pasando**: con las dos fechas
iguales el motor devolvió las filas en el orden en que ya estaban. Los que se
cayeron son los dos que cargan cortes distintos y desordenados. Si se borra
alguno de esos dos, la trampa queda sin red.

**Un líder sin equipo devuelve 404, no una tabla vacía.** Un 200 con cero filas se
lee como "no hay datos cargados" y manda a buscar el problema al lado equivocado.
El mensaje lista los líderes que sí existen.

**`@Async` no hacía nada.** `recibir()` llamaba a `procesar()` dentro del mismo
bean, y esa autoinvocación esquiva el proxy de Spring: corría sincrónico mientras
la anotación se leía como si funcionara. El procesamiento vive ahora en
`ProcesadorDeImportaciones`, que se inyecta y por lo tanto sí pasa por el proxy.
**Si alguien vuelve a mover ese método adentro del servicio, el asincronismo
desaparece en silencio.**

**El motor de incentivos se cayó del alcance.** Al principio el proyecto apuntaba a
calcular los importes. No va: llegan calculados desde la empresa y se adjuntan tal
cual. Lo valioso es el multimétrico.

---

## La estructura del backend

Organizado **por capa**, no por módulo:

```
config/       properties, auditoría JPA, el conversor de período
domain/       las entidades JPA
enumeration/  los enums del dominio
repository/   las interfaces de Spring Data
dto/          records agrupados por caso de uso, en subcarpetas
mapper/       interfaz + impl/, escritos a mano
service/      interfaz + impl/, una subcarpeta por servicio
controller/   los REST controllers
util/         PeriodoUtil, DecimalUtil, NumeroUtil, JsonUtil, NombreNormalizado
exception/    las excepciones y el manejador global
```

Antes era hexagonal: records de dominio inmutables, puertos `*Repository` propios
y adaptadores `*EnBase` que traducían entidad ↔ dominio. **Eso ya no existe.** Hay
un modelo único —la entidad JPA **es** el dominio—, `repository/` son
`JpaRepository` directos, y los cálculos que vivían en los records se mudaron a
los `*ServiceImpl`; sus resultados son records planos en `dto/`.

### Tres cosas que costaron y conviene no repetir

1. **`MultipleBagFetchException`.** Con `open-in-view: false` la entidad viaja
   hasta el mapper con la transacción cerrada, así que las lecturas de pliego y
   padrón llevan `@EntityGraph`. Hibernate se niega a traer dos *bags* —listas sin
   columna de índice— en la misma consulta, **incluso cuando uno cuelga del
   otro**. Por eso `Pliego.escalas` y `Escala.bandas` son `Set` y no `List`.
2. **La duplicación silenciosa.** Aun sin la excepción, el `join` contra las
   bandas multiplica las filas de escala. Con listas, el `alcanceMaximo` —la suma
   de los pesos— pasaba de 10,5 a 28 sin ningún error a la vista. El `Set` lo
   deduplica solo, y es la razón de peso de las dos.
3. **`BigDecimal.equals` mira la escala.** Un `61.0` guardado vuelve como `61`
   porque `DecimalUtil` le saca los ceros de más. Los tests comparan con
   `isEqualByComparingTo`, nunca por igualdad.

---

## Trampas del entorno

**El shell es Git Bash sobre Windows** y los heredocs largos se comen las barras
invertidas y truncan los archivos. Para escribir Java con regex o clases de más
de ~150 líneas, no uses heredoc.

**No pases por el shell contenido con `${...}` ni con backticks.** Un
`node -e "..."` entre comillas dobles deja que bash los expanda antes de que node
los vea: una plantilla de TypeScript quedó como `` ` s` `` —con la interpolación
comida— y la pantalla mostró "%" en vez de "616 s". Compiló y pasó el `tsc`
igual, porque sintácticamente es válido. Para editar archivos con plantillas,
markdown con backticks o regex, **escribí el script a un archivo y corrélo**.

**Los finales de línea están mezclados.** `src/types.ts` y varios `.tsx` son CRLF;
los `.java` son LF. Un reemplazo exacto escrito con `\n` no engancha en los CRLF y
falla sin decir por qué. Normalizá a LF para buscar y devolvé el archivo a lo que
era al escribir, o el diff se llena de líneas que no cambiaron.

## Trampas al escribir tests

**`@SpringBootTest` comparte el contexto de Spring —y con él la base H2— entre
todas las clases de test.** Las mediciones se acumulan y un pliego cargado no se
puede pisar. La base vive lo que vive el contexto (`DB_CLOSE_DELAY=-1`).

**Cada test necesita su propio período**, no cada clase. Dos métodos de la misma
clase que carguen mediciones del mismo asesor y período se ven entre sí.
`ImportacionServiceTest` usa un mes distinto en cada `@BeforeEach`. Los tests de
`repository/` usan 2040 en adelante para no cruzarse con nadie. Ya rompió tests
dos veces.

---

## Deuda conocida

- **Sin autenticación**: cualquiera ve todo. Ver "Por dónde seguir".
- **El corte único por métrica no lo valida el backend.** Ver "Reglas de
  producto".
- **`ddl-auto: none`**: nadie valida las entidades contra el esquema al arrancar.
  La red es que los tests ejercitan todas las tablas. Hueco real, asumido.
- **`historia(periodo)` trae el mes entero a memoria.** Con cincuenta personas no
  molesta; con la operación completa y varios meses hay que paginar o mover el
  agrupado por legajo a un `group by`.
- **Las listas del informe de importación van serializadas en columnas de texto**,
  no en tablas: no se pueden consultar por SQL. A sabiendas — es un reporte que se
  lee entero y de una sola importación (`ListasComoJson`). Si alguna vez hace
  falta "todos los rechazos por tal motivo del año", eso es una tabla nueva, no un
  `LIKE` sobre el JSON.
- **`medicion` no valida legajos contra el padrón, a propósito**: quien traduce
  nombres a legajos es `importacion`. Consecuencia: una medición con un legajo
  fuera del padrón queda guardada y **no aparece en ningún multimétrico**, porque
  la tabla se arma recorriendo el equipo.
- **H2 no es MySQL.** Los tests corren las mismas migraciones, pero H2 no verifica
  collations, ni el límite de tamaño de fila de InnoDB, ni el manejo de zonas del
  driver. Lo que podía morder se sacó del motor y se puso en columnas explícitas
  (`campania_clave`, `legajo_clave`, `nombre_clave`), pero el resto no está
  cubierto. **Antes de dar por buena una consulta nueva, probala contra el
  compose.**
- **Flyway avisa que MySQL 8.4 es más nuevo de lo que tiene probado** (llega a
  8.1). Es un `WARN`, no un error. Si molesta, bajar la imagen a `mysql:8.0`.
- **El rojo del código de color no se ve con el pliego de ejemplo**: ninguna banda
  paga menos de cero. Se verificó a mano escribiendo un `-1,5` en el editor.
- **En "Mis métricas", la fecha bajo cada métrica es la del mes actual.** Con la
  columna izquierda ahora seleccionable, esa fecha convive con dos meses en
  pantalla. Se leyó como aceptable porque cada columna tiene su encabezado, pero
  es candidata a mover si alguien se confunde.
- La campaña existe en el pliego pero todavía no la usa nadie más.

---

## Hecho

### `pliego`

Motor de tramos y validación que rechaza pliegos rotos. Verificado contra la API:
un pliego con la banda `[-∞, 681)` junto a `[680, +∞)` devuelve `422` con *"las
bandas se pisan entre 680 y 681"*; uno ya cargado devuelve `409`; un FCR de 80,04%
—que en la planilla no caía en ningún tramo— resuelve a `[79.5, 80.1)`.

### `padron`

Padrón por período con el líder de cada asesor, y resolución de nombres a legajo.
Verificado contra la API con los seis casos de los datos reales: nombre truncado a
mitad de palabra, nombre sin acentos, legajo directo, prefijo ambiguo,
codificación rota y nombre inexistente. Tras confirmar un alias, el mismo archivo
pasa de 3 resueltos a 4.

### `medicion` + `multimetrico`

Cada carga semanal es una medición nueva, no un `UPDATE`. El vigente es el del
corte más reciente, y **la fecha viaja pegada al valor** en `MedicionesVigentes`,
así que no se puede sacar el número sin su corte —que es justo lo que la planilla
pierde—.

El multimétrico recorre **del padrón hacia las mediciones**, no al revés: un
asesor sin datos aparece igual en vez de desaparecer.

`TramoDeArriba` calcula cuánto le falta a cada uno para el tramo siguiente: lo
único accionable de la tabla. Dos sutilezas resueltas: el tramo de arriba es el
que **paga más**, no el siguiente en la lista (el sentido de la métrica decide la
dirección); y como las bandas son `[desde, hasta)`, subiendo se alcanza el borde y
bajando hay que cruzarlo, así que la condición (`ALCANZAR` / `BAJAR_DE`) viaja
aparte de la distancia en vez de inventar un epsilon.

### `importacion`

El mapeo de columnas es **configuración en tiempo de ejecución**, no código: nadie
tiene acceso a los archivos que publica la empresa y van a cambiar. La inspección
lee el archivo, muestra los encabezados con ejemplos, y una persona arma el perfil
mirándolo. El sistema no adivina.

Verificado con un `.xlsx` sintético: 6 filas leídas → 2 importadas, 2 rechazadas
(con número de fila y motivo), 2 pendientes (con candidatos). Un archivo con la
columna renombrada se rechaza entero: *"esperaba un encabezado 'VALOR', encontré:
ASESOR, RESULTADO, LIDER"*.

Los pendientes ambiguos se resuelven **por importación, no como alias global** —la
deuda que quedaba del padrón—. Resolver `SOSA ANA` a `i300` produjo su medición y
dejó a `i400` sin ninguna.

Una importación `RECHAZADA` responde `cuadra: false`. El invariante del `Informe`
sigue siendo cierto —las tres bolsas suman las filas leídas, y en una rechazada
las cuatro cifras son cero—; lo que se corrige en el mapper es la lectura que
publica la API, porque "¿entró todo?" y "¿cierran las cuentas?" son dos preguntas
distintas.

### `persistencia`

MySQL con Flyway, `ddl-auto: none`. Verificado contra MySQL de verdad y no sólo
con tests: se cargó un pliego, un padrón y cuatro cargas de mediciones, se
reinició la aplicación dos veces y todo seguía ahí, incluido el desempate del
mismo corte y una importación entera con su informe.

**No hay repositorios en memoria.** No quedaron detrás de un perfil a propósito:
un perfil que elige implementación es una forma de que la aplicación ande en los
tests y no en producción.

### frontend

Cinco pantallas, todas contra la API real y verificadas en el navegador.

**Multimétrico** — una fila por asesor y, por cada métrica, **dos columnas**: el
valor medido y lo que ese valor paga, del mismo tamaño. Son cosas distintas —616
segundos es el desempeño, +3,5% es lo que ese desempeño cobra— y cuando el pliego
cambia de mes se mueven por separado. El encabezado de cada métrica lleva su fecha
de corte, y el pie **fijo** es la contabilidad del líder: promedio de cada métrica
y de lo que paga cada una. El promedio del valor toma sólo a quienes tienen dato
—no se puede promediar un TMO que no existe— pero el del aporte toma a todos,
porque ahí el que no tiene archivo aporta cero de verdad.

**Mis métricas** — una persona contra otro mes: el comparado a la izquierda, el
actual a la derecha, y Δ al final. El mes se elige (el anterior por defecto) entre
los que tengan padrón. La elección **no sobrevive al cambio de período**: comparar
septiembre contra enero porque antes mirabas agosto casi nunca es lo que quisiste.
El Δ se pinta por el **sentido** de la métrica y no por el signo: bajar once
segundos de TMO es mejorar y bajar cinco puntos de NPS es empeorar.

Al pie, **cómo se movió cada métrica dentro del mes**, carga por carga y con la
vigente marcada. Es lo que contesta "¿por qué mi FCR bajó desde la semana
pasada?". Una métrica con una sola carga no se muestra: una serie de un punto no
cuenta ninguna historia.

**Pliego, Importación y Perfiles** — sin cambios de fondo. En el editor de pliego,
lo que paga cada banda se pinta con el código de color; se pinta el texto y no el
fondo, porque un input con fondo de color se lee como error de validación.

---

## Para el que sigue

1. Leé `PLAN.md`, sobre todo la sección 3 (decisiones tomadas) y la 4
   (convenciones).
2. Leé "Reglas de producto" y las dos secciones de trampas de este archivo. Cada
   punto costó un fallo real y en varios la intuición tira para el otro lado.
3. **No inventes datos de prueba a partir de ninguna planilla real.** Todo
   sintético. La planilla que originó el proyecto tiene DNI, cuentas, teléfonos de
   clientes y sueldos; no entra al repositorio ni como fixture ni "temporalmente".
4. Antes de dar algo por terminado, verificalo **contra la API corriendo**, no
   sólo con tests. Y si tocás pantalla, miralo en el navegador: la única falla
   real que encontró la primera revisión visual —el enum crudo en los encabezados—
   había pasado el `tsc` y el build sin problema.
5. Los commits van **sin firma de agente**.
6. Actualizá este archivo.

### Si vas a orquestar agentes

- **Dos agentes no pueden compartir el módulo Maven.** Corren `./mvnw test` sobre
  el mismo `target/` y se pisan; además, mientras uno escribe, el proyecto no
  compila y el otro no puede validar nada. Backend de a uno; el frontend sí va en
  paralelo porque es otro directorio.
- **Escribí el contrato antes de soltarlos.** `PLAN.md` existe por esto. Aun así
  el frontend duplicó el cálculo del tramo de arriba, porque se construyó contra
  un PLAN escrito antes de que el backend decidiera exponerlo.
- **Dales límites de archivo explícitos** y decíles que no toquen `pom.xml` ni
  `HANDOFF.md`: son los puntos donde el merge duele.
- **Verificá lo que reportan.** El número de tests y el build se chequean en diez
  segundos.
