# Handoff — multimetrico-api

Estado vivo del proyecto. Se actualiza cada vez que una pieza queda terminada.
El **qué** y el **por qué** están en `PLAN.md`; acá va el **cómo viene**.

Última actualización: 2026-08-28, noche. **El MVP anda de punta a punta.**
Falta seguridad y nada más.

La última ronda cambió reglas de producto, no sólo pantallas. Lo importante
está en "Lo que cambió de criterio"; leelo antes de tocar el multimétrico.

---

## Semáforo

| Módulo | Estado | Tests | Notas |
|---|---|---|---|
| `pliego` | ✅ terminado | 30 | Motor de tramos, validación y API |
| `padron` | ✅ terminado | 24 | Resolución de nombres y aprendizaje de alias |
| `medicion` | ✅ terminado | 46 | Historia por corte; el vigente es el corte más reciente |
| `multimetrico` | ✅ terminado | (idem) | Incluye el tramo de arriba, accionable |
| `importacion` | ✅ terminado | 8 | Perfil configurable; ninguna fila se pierde |
| `persistencia` | ✅ terminado | 24 | MySQL + Flyway; los tests corren contra H2 en modo MySQL |
| `seguridad` | ⬜ pendiente | — | Lo único que falta del alcance |
| frontend | ✅ terminado | **0** | En `../multimetrico-frontend`. Cinco pantallas, sin mocks. Sin tests, pero **ya revisado en el navegador** |

**Total actual: 134 tests, en verde.**

## Lo que cambió de criterio

Tres decisiones que estaban escritas al revés en el código y en este archivo. No
las "arregles" de vuelta.

**1. Una métrica sin archivo bonifica cero.** Antes el proyecto defendía lo
contrario —"un 9% con dos archivos sin llegar no es lo mismo que un 9%
completo"— y por eso la fila quedaba marcada incompleta y salía de los
promedios. Operativamente no se sostiene: si al día 10 no llegó el NPS, el NPS
no bonifica, y esa persona cobra lo que cobra. `alcanceTotal` **ya se comportaba
así** —sumar sólo lo presente es idéntico a sumar cero por lo ausente—, así que
no hubo que tocar el cálculo: lo que estaba mal era la lectura alrededor.

Lo que **sí** se mantiene es que `faltantes` viaje aparte de `metricas`. Es lo
único que deja distinguir "no hay archivo" de "hay un valor que paga cero", y una
vez aplastados no se separan más. La pantalla muestra el valor vacío y el cero
sólo en la columna de lo que paga.

**2. Una métrica tiene una sola fecha de corte por mes.** Cada métrica llega en
su propio archivo, ese archivo trae a todo el equipo con el mismo corte, y el día
varía entre métricas y entre meses según cuándo lo mande la empresa. Se cayó todo
lo que trataba los "cortes mezclados" como algo normal: el aviso de arriba de la
tabla y la fecha por celda. El encabezado de cada métrica dice su fecha y con eso
alcanza.

**Ojo**: el backend no lo valida. `ResumenDeEquipo.cortesPorMetrica` sigue siendo
un `Set<LocalDate>` y la pantalla toma el primero. Quien lo garantiza hoy es el
seed, que manda **un lote por métrica**. Si alguna vez una importación parte una
métrica en dos cortes, la pantalla va a mostrar sólo uno sin avisar. Convertirlo
en una validación de verdad es trabajo pendiente.

**3. Las mediciones no tienen campaña.** No es nuevo, pero costó una hora
descubrirlo: `Medicion` se identifica por período, legajo y métrica, así que las
mediciones cargadas para `RESIDENCIAL` son **las mismas** que lee el multimétrico
de `CONVERGENTE` del mismo mes. Dos seeds con campañas distintas sobre el mismo
período se pisan, y gana el corte más reciente. Si vas a sembrar otra campaña,
usá otro período o vaciá la base.

---

## El MVP, andando

Las cinco pantallas se abrieron en Chrome contra la API corriendo y la base de
verdad. Ninguna tira error de consola y las cinco muestran datos reales del
backend.

Para dejarlo así de nuevo, en este orden:

```bash
docker compose up -d               # MySQL en el 3307
./mvnw spring-boot:run             # la API en el 8082
node scripts/datos-de-ejemplo.mjs  # el mes cargado
cd ../multimetrico-frontend && npm run dev
```

**`scripts/datos-de-ejemplo.mjs` es la pieza que hace mirable el proyecto.**
Sin él la primera pantalla responde 404 y parece rota cuando lo único que pasa es
que no hay nada cargado.

Carga **de enero al mes en curso** en la campaña `CONVERGENTE`, con su pliego y
su padrón por mes. Varios meses porque "Mis métricas" compara contra el anterior;
no carga meses futuros, que serían mediciones con una fecha de corte que todavía
no ocurrió. Acepta un rango: `node scripts/datos-de-ejemplo.mjs 2026-01 2026-12`.

Tres decisiones del seed que no son ruido:

- Los valores salen de un pseudoazar **sembrado por legajo y mes**, así que dos
  corridas dan los mismos números y la captura de ayer sigue siendo comparable.
- Cada asesor tiene una **línea de base propia** que pesa el doble que el
  movimiento del mes. Si cada mes fuera un sorteo independiente, la comparación
  contra el mes anterior no diría nada: nadie "viene mejorando", cada mes sería
  otra persona.
- **Un lote por métrica**, no uno solo con todo adentro. Es lo que garantiza una
  sola fecha de corte por métrica, y es como llegan los archivos de verdad.

En el último mes deja tres personas sin archivo en alguna métrica: es lo que
ejercita la regla de que una métrica sin archivo bonifica cero.

Ojo con dos cosas que costaron un rato:

- **Docker Desktop tiene que estar levantado**, no solo instalado. Sin él,
  `docker compose up` falla con un error de named pipe que no dice eso.
- **El `localStorage` del frontend se acuerda del equipo elegido.** La clave es
  `multimetrico.contexto`. Una sesión vieja había dejado `lider: "MARTINA REYES"`
  y la primera pantalla abría filtrada en ocho de los quince asesores, que se lee
  como si faltara gente. El valor por defecto es `""` —toda la operación— y está
  bien; si al abrir aparece un equipo elegido, es la memoria del navegador, no un
  bug.

---

## La estructura del backend cambió

El proyecto pasó de estar organizado **por módulo** (`pliego/`, `padron/`,
`medicion/`, `multimetrico/`, `importacion/`, `persistencia/`, cada uno con su
`domain/` y su `web/`) a estar organizado **por capa**:

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

**Lo que se fue con el cambio.** Era una arquitectura hexagonal: records de
dominio inmutables, puertos `*Repository` propios y adaptadores `*EnBase` que
traducían entidad ↔ dominio. Ahora hay un modelo único: la entidad JPA **es** el
dominio, `repository/` son `JpaRepository` directos y los adaptadores no existen.
Los cálculos que vivían adentro de los records —evaluar contra el pliego,
desempatar la medición vigente, armar la fila y su tramo de arriba— se mudaron a
los `*ServiceImpl`, y los resultados de esos cálculos son records planos en
`dto/`.

**El contrato HTTP no se movió.** Se verificó contra la base con los datos que ya
estaban cargados desde antes del cambio: el `GET /api/pliegos` devuelve el mismo
JSON byte a byte, y el multimétrico de equipo sale con su resumen, sus cortes por
métrica y el `detalle` del tramo de arriba redactado igual. El frontend no
necesitó tocarse.

Dos respuestas sí cambiaron, a propósito:

- Una planilla que no coincide con su perfil ahora devuelve **422 con cuerpo de
  error** en `/api/perfiles/inspeccion`, donde antes se escapaba como 500 pelado.
- `POST /api/pliegos` normaliza los decimales igual que el `GET`. Antes el POST
  devolvía `80.0` y el GET `80` para el mismo pliego; ahora los dos dicen `80`.

### Tres cosas que costaron y conviene no repetir

1. **`MultipleBagFetchException`.** Con `open-in-view: false` la entidad viaja
   hasta el mapper con la transacción ya cerrada, así que las lecturas de pliego
   y padrón llevan `@EntityGraph`. Hibernate se niega a traer dos *bags* —listas
   sin columna de índice— en la misma consulta, **incluso cuando uno cuelga del
   otro**. Por eso `Pliego.escalas` y `Escala.bandas` son `Set` y no `List`.
2. **La duplicación silenciosa.** Aun sin la excepción, el `join` contra las
   bandas multiplica las filas de escala. Con listas, el `alcanceMaximo` —que es
   la suma de los pesos— pasaba de 10,5 a 28 sin ningún error a la vista. El
   `Set` lo deduplica solo, y es la razón de peso de las dos.
3. **`BigDecimal.equals` mira la escala.** Un `61.0` guardado vuelve como `61`
   porque `DecimalUtil` le saca los ceros de más. Los tests tienen que comparar
   con `isEqualByComparingTo`, nunca por igualdad — es la misma trampa contra la
   que el dominio viene advirtiendo desde el principio.

---

## Cómo correrlo

```bash
docker compose up -d     # MySQL en el 3307
./mvnw spring-boot:run   # :8082
./mvnw test              # no necesita la base levantada

cd ../multimetrico-frontend && npm run dev   # la pantalla
```

El contenedor puede existir apagado de una sesión anterior: si `compose up`
protesta que el nombre está en uso, es eso — `docker start multimetrico-mysql`
lo revive **con sus datos**. `docker compose down -v` lo borra de cero.

Vite elige el primer puerto libre desde el 5173, y en esta máquina el 5173 y el
5174 los ocupan los dev servers del portfolio. Mirá lo que imprime al arrancar.

Ya hay base: lo que se carga sobrevive al reinicio. La aplicación necesita MySQL
para arrancar; **los tests no**, que corren contra H2 en modo de compatibilidad
MySQL con las mismas migraciones de Flyway.

---

## Por dónde seguir

En orden de impacto:

**1. Seguridad (PLAN §5.6).** Es lo único que falta del alcance original y lo más
visible para quien evalúe el proyecto. Hoy cualquiera ve la fila de cualquiera,
en el backend y en la pantalla. Lo importante es que el control es **por fila, no
por endpoint**: un asesor que pide la fila de otro recibe 403, nunca la fila. En
el frontend, "Mis métricas" tiene hoy un selector de asesor que hay que
reemplazar por el legajo del token.

**2. El frontend no tiene un solo test.** No hay infraestructura montada. Es lo
más grande que queda después de seguridad.

**3. La deuda de la lista de abajo**, que sigue toda en pie salvo las tres que se
tacharon.

---

## Cómo dejarlo andando para mirarlo

Los cuatro comandos de "El MVP, andando" y listo. El seed deja la campaña
`CONVERGENTE` con dos equipos, **MARTINA REYES** (ocho) y **DIEGO SALAZAR**
(siete). Son los dos únicos líderes: con cualquier otro nombre la API responde
404 y lista los que sí existen.

El frontend abre en `2026-08 / CONVERGENTE`, que es lo que carga el seed por
defecto. Si cambiás el período a un mes sin datos vas a ver el 404 del pliego,
que es correcto y no un bug.

---

## Hecho

### Cerrado en esta sesión

**Las dos fallas conocidas del backend, arregladas y con test.**

- **`Metrica.Unidad` ahora viaja en la API.** La agregaron
  `PliegoResponse.EscalaResponse` y `FilaResponse.CeldaResponse`, y el mapa de
  métrica a unidad que el frontend mantenía de memoria en `src/lib/format.ts` se
  borró: `conUnidad` ahora recibe la `Unidad` en vez de deducirla de la métrica.
  Verificado contra la API corriendo —TMO sale `SEGUNDOS`, las otras cuatro
  `PORCENTAJE`— y en el navegador, donde el TMO se lee "616 s" y no "616%".
- **`cuadra` es `false` en una importación `RECHAZADA`.** La corrección vive en
  `ImportacionMapperImpl` y **no** en `Informe`: el invariante del informe sigue
  siendo cierto —las tres bolsas suman las filas leídas, y en una rechazada las
  cuatro cifras son cero—, lo que se corrige es la lectura que publica la API.
  Son dos preguntas distintas y el test nuevo afirma las dos.

**La revisión visual, hecha.** Las cinco pantallas se abrieron en Chrome contra la
API corriendo. Encontró una sola cosa real: **la tabla del equipo imprimía el
nombre crudo del enum** en los encabezados —`ACTIVIDAD_COMERCIAL`,
`BAJA_CONVERGENTE`—. `nombreDeMetrica()` existía en `format.ts` y lo usaban las
otras cuatro pantallas; la única que no era justo aquella donde el nombre es el
rótulo de la columna. Es exactamente la clase de cosa que ningún test de tipos
encuentra y que se ve en dos segundos mirando.

### `pliego`

Dominio del pliego con el motor de tramos y la validación que rechaza pliegos
rotos. Verificado contra la API: un pliego con la banda `[-∞, 681)` junto a
`[680, +∞)` devuelve `422` con *"las bandas se pisan entre 680 y 681"*; un pliego
ya cargado devuelve `409`; un FCR de 80,04% —que en la planilla no caía en ningún
tramo— resuelve a `[79.5, 80.1)`.

### `padron`

Padrón por período con el líder de cada asesor, y resolución de nombres a legajo.
Verificado contra la API con los seis casos que aparecen en los datos reales:
nombre truncado a mitad de palabra, nombre sin acentos, legajo directo, prefijo
ambiguo, codificación rota y nombre inexistente. Tras confirmar un alias, el mismo
archivo pasa de 3 resueltos a 4.

### `medicion` + `multimetrico`

La historia se guarda: cada carga semanal es una medición nueva, no un `UPDATE`.
El valor vigente es el del corte más reciente, y **la fecha viaja pegada al
valor** en `MedicionesVigentes`, así que no se puede sacar el número sin su corte
—que es justo lo que la planilla pierde—.

El multimétrico recorre **del padrón hacia las mediciones**, no al revés: un
asesor sin datos aparece igual, con todo faltante, en vez de desaparecer.

`TramoDeArriba` calcula cuánto le falta a cada asesor para el tramo siguiente. Es
lo único accionable de toda la tabla. Dos sutilezas resueltas: el tramo de arriba
es el que **paga más**, no el siguiente en la lista (el sentido de la métrica
decide la dirección); y como las bandas son `[desde, hasta)`, subiendo se alcanza
el borde y bajando hay que cruzarlo, así que la condición (`ALCANZAR` /
`BAJAR_DE`) viaja aparte de la distancia en vez de inventar un epsilon.

### `importacion`

El mapeo de columnas es **configuración en tiempo de ejecución**, no código:
nadie tiene acceso a los archivos que publica la empresa y van a cambiar. La
inspección lee el archivo, muestra los encabezados con ejemplos, y una persona
arma el perfil mirándolo. El sistema no adivina.

Verificado contra la API con un `.xlsx` sintético: 6 filas leídas → 2 importadas,
2 rechazadas (con número de fila y motivo), 2 pendientes (con candidatos),
`cuadra: true`. Un archivo con la columna renombrada se rechaza entero:
*"esperaba un encabezado 'VALOR', encontré: ASESOR, RESULTADO, LIDER"*.

Los pendientes ambiguos se resuelven **por importación, no como alias global** —
la deuda que quedaba del padrón. Resolver `SOSA ANA` a `i300` produjo su medición
y dejó a `i400` sin ninguna.

---

### `persistencia`

MySQL con Flyway. Las implementaciones JPA entraron detrás de las interfaces que
ya existían —`PliegoRepository`, `PadronRepository`, `RegistroDeAlias`,
`MedicionRepository`, `PerfilRepository`, `ImportacionRepository`— y viven en
`persistencia/`, separadas del dominio: los records no se tocaron y las entidades
JPA son clases aparte con su mapeo de ida y vuelta.

Verificado contra MySQL de verdad, no solo con tests: se cargó un pliego, un
padrón y cuatro cargas de mediciones, se reinició la aplicación dos veces y todo
seguía ahí, incluido el desempate del mismo corte y una importación entera con su
informe.

**Los repositorios en memoria ya no existen como beans.** Cuatro se borraron;
`MedicionEnMemoria` y `AliasEnMemoria` se mudaron a `src/test` porque dos tests
unitarios los usan sin levantar Spring. No quedaron detrás de un perfil a
propósito: un perfil que elige implementación es una forma de que la aplicación
ande en los tests y no en producción.

---

## Decisiones que se corrigieron sobre la marcha

Se dejan anotadas porque la intuición tira para el otro lado y alguien va a querer
"arreglarlas" de vuelta.

**El prefijo de un nombre no exige borde de palabra.** Al principio sí lo exigía, y
un test lo tumbó: el truncado real corta a mitad de palabra (`AGUIRRE FLAVIA Y`),
así que la regla rechazaba justo el caso más común. Lo que da seguridad es la
unicidad, no dónde cae el corte.

**Empate de fecha de corte: gana la última cargada.** Dos mediciones del mismo
corte son el mismo archivo subido dos veces, y la segunda es la corrección.
Quedarse con la primera anularía justo la corrección que alguien subió. Esto
convierte el *orden de carga* en parte del contrato de `MedicionRepository`: una
implementación JPA tiene que ordenar por id o instante de alta, **nunca** por
fecha de corte.

**El orden de carga de las mediciones es un `ORDER BY id`, y hay que defenderlo
con más de un test.** La consulta que ordena la historia por id —y no por fecha
de corte— es lo que hace que gane la última cargada. Se probó rompiéndola a
propósito, y el test del empate de corte **siguió pasando**: con las dos fechas
iguales el motor devolvió las filas en el orden en que ya estaban. Los que se
cayeron son los dos que cargan cortes distintos y desordenados. Si alguna vez se
borra alguno de esos dos, la trampa queda sin red.

**Un líder sin equipo devuelve 404, no una tabla vacía.** Un 200 con cero filas se
lee como "no hay datos cargados" y manda a buscar el problema al lado equivocado.
El mensaje lista los líderes que sí existen.

**`@Async` no hacía nada.** `recibir()` llamaba a `procesar()` dentro del mismo
bean, y esa autoinvocación esquiva el proxy de Spring: la anotación se leía como
si funcionara y corría sincrónico. El procesamiento vive ahora en
`ProcesadorDeImportaciones`, que se inyecta y por lo tanto sí pasa por el proxy.
**Si alguien vuelve a mover ese método adentro del servicio, el asincronismo
desaparece en silencio.**

**El motor de incentivos se cayó del alcance.** Al principio el proyecto apuntaba a
calcular los importes. No va: llegan calculados desde la empresa y se adjuntan tal
cual. Lo valioso es el multimétrico.

---

## Deuda conocida

- **Sin autenticación.** Ver "Por dónde seguir". Es lo único del alcance
  original que queda sin hacer.
- ~~`Metrica.Unidad` no la expone ninguna respuesta~~ — resuelto, ver "Cerrado en
  esta sesión".
- ~~`cuadra: true` en una importación `RECHAZADA`~~ — resuelto, ídem.
- **El frontend guarda el recorte en `localStorage`** bajo
  `multimetrico.contexto`. Es deliberado —un F5 no devuelve a un supervisor al
  mes por defecto—, pero significa que **lo que ve alguien al abrir depende de su
  navegador**. Si la primera pantalla aparece filtrada por un equipo, es eso.
- **`ddl-auto: none` significa que nadie valida las entidades contra el esquema al
  arrancar.** La red que queda es que los tests ejercitan todas las tablas. Es un
  hueco real, asumido a sabiendas.
- **`historia(periodo)` trae el mes entero a memoria.** Con cincuenta personas no
  molesta; con la operación completa hay que paginar o agrupar en SQL.
- **Las listas del informe de importación van serializadas en columnas de texto**,
  no en tablas: no se pueden consultar por SQL. A sabiendas — son un reporte que
  se lee entero y de una sola importación.

- **Un alias sobre un nombre ambiguo puede quedar mal para siempre.** Confirmar
  `SOSA ANA` contra un legajo lo ata globalmente, y en otro archivo podría ser la
  otra persona. Lo correcto es que un pendiente ambiguo se resuelva por archivo y
  no como alias global. **Se arregla cuando entre `importacion`**, que es donde
  tiene sentido.
- **`medicion` no valida legajos contra el padrón, a propósito**: quien traduce
  nombres a legajos es `importacion`. Consecuencia: una medición con un legajo
  fuera del padrón queda guardada y **no aparece en ningún multimétrico**, porque
  la tabla se arma recorriendo el equipo. Se ataja en `importacion`.
- **`ManejadorDeErrores` es global pero vive en `pliego/web/`.** Funciona para
  todos los módulos; convendría moverlo a un paquete compartido.
- **Sin autenticación**: cualquiera ve todo.
- **H2 no es MySQL.** Los tests corren las mismas migraciones, pero H2 no
  verifica collations, ni el límite de tamaño de fila de InnoDB, ni el manejo de
  zonas del driver de MySQL. Las diferencias que podían morder se sacaron del
  motor y se pusieron en columnas explícitas (`campania_clave`, `legajo_clave`,
  `nombre_clave`), pero lo que no está cubierto es todo lo demás. **Antes de dar
  por buena cualquier consulta nueva, probala contra el compose.**
- **Flyway avisa que MySQL 8.4 es más nuevo de lo que tiene probado** (soporta
  hasta 8.1). Es un `WARN` al arrancar, no un error, y la migración se aplicó
  bien. Si algún día molesta, bajar la imagen a `mysql:8.0`.
- **Las listas del informe de una importación no se pueden consultar por SQL**:
  van serializadas en columnas de texto. Está explicado en `ListasComoJson`. Si
  alguna vez hace falta preguntar "todos los rechazos por tal motivo del año",
  eso es una tabla nueva, no un `LIKE` sobre el JSON.
- **`historia(periodo)` trae el mes entero a memoria.** Con un equipo de
  cincuenta personas son unos cientos de filas y no molesta; con la operación
  completa y varios meses, va a haber que paginar o mover el agrupado por legajo
  a un `group by`.
- La campaña existe en el pliego pero todavía no la usa nadie más.

---

## Trampas conocidas al escribir tests

**`@SpringBootTest` comparte el contexto de Spring entre clases de test**, y con él
los repositorios en memoria. Las mediciones se acumulan y un pliego cargado no se
puede pisar. **Cada clase de test tiene que usar su propio período** (`YearMonth`)
para no chocar con las demás. Ya rompió tests dos veces.

**Ahora lo compartido es una base H2, no unos mapas.** Cambió el mecanismo pero no
la consecuencia: la base vive lo que vive el contexto de Spring
(`DB_CLOSE_DELAY=-1`) y se comparte entre todas las clases de test, así que **el
período propio por test sigue siendo obligatorio**. Los tests nuevos de
`persistencia/` usan 2040 en adelante para no cruzarse con nadie.

**El aislamiento por período hace falta por test, no solo por clase.** Dos métodos
de la misma clase que carguen mediciones del mismo asesor y período se ven entre
sí. `ImportacionServiceTest` usa un mes distinto en cada `@BeforeEach`.

**El shell de este entorno es Git Bash sobre Windows** y los heredocs largos se
comen las barras invertidas y truncan los archivos largos. Para escribir Java con
regex o clases de más de ~150 líneas, no uses heredoc.

**Y tampoco pases por el shell contenido con `${...}` o con backticks.** Un
`node -e "..."` entre comillas dobles deja que bash expanda las dos cosas antes
de que node las vea: una plantilla de TypeScript quedó como `` ` s` `` —con la
interpolación comida— y la pantalla mostró "%" en vez de "616 s". Compiló y pasó
el `tsc` igual, porque sintácticamente es válido. Para editar archivos con
plantillas, markdown con backticks o regex, **escribí el script a un archivo y
corrélo**, no lo pases inline.

**Los finales de línea del repo están mezclados.** `src/types.ts` y varios `.tsx`
son CRLF; los `.java` son LF. Un reemplazo exacto escrito con `\n` no engancha en
los CRLF y falla sin decir por qué. Normalizá a LF para buscar y devolvé el
archivo a lo que era al escribir, o el diff se llena de líneas que no cambiaron.

---

## Para el que sigue

1. Leé `PLAN.md`, sobre todo la sección 3 (decisiones tomadas) y la 4 (convenciones).
2. Leé "Decisiones que se corrigieron sobre la marcha" y "Trampas conocidas al
   escribir tests" de este archivo. Las dos listas existen porque cada punto costó
   un fallo real, y la intuición tira para el otro lado en varios.
3. No inventes datos de prueba a partir de ninguna planilla real. Todo sintético.
   La planilla que originó el proyecto tiene DNI, cuentas, teléfonos de clientes y
   sueldos; no entra al repositorio ni como fixture ni "temporalmente".
4. Antes de dar algo por terminado, verificalo contra la API corriendo, no solo
   con tests. Y si tocás pantalla, miralo en el navegador: es lo único que no
   verificó nadie todavía.
5. Actualizá este archivo.

### Si vas a orquestar agentes

Lo que se aprendió corriéndolos en esta sesión:

- **Dos agentes no pueden compartir el módulo Maven.** Los dos corren `./mvnw test`
  sobre el mismo `target/` y se pisan; además, mientras uno escribe, el proyecto no
  compila y el otro no puede validar nada. Backend de a uno; el frontend sí va en
  paralelo porque es otro directorio.
- **Escribí el contrato antes de soltarlos.** `PLAN.md` existe por esto. Aun así,
  el frontend duplicó el cálculo del tramo de arriba porque se construyó contra un
  PLAN escrito antes de que el backend decidiera exponerlo. Si dos agentes van en
  paralelo, lo que uno decida de más hay que avisárselo al otro.
- **Dales límites de archivo explícitos** y decíles que no toquen `pom.xml` ni
  `HANDOFF.md`: son los puntos donde el merge duele.
- **Verificá lo que reportan.** Vinieron honestos, pero el número de tests y el
  build se chequean en diez segundos.
