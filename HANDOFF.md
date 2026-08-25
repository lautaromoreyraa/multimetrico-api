# Handoff — multimetrico-api

Estado vivo del proyecto. Se actualiza cada vez que una pieza queda terminada.
El **qué** y el **por qué** están en `PLAN.md`; acá va el **cómo viene**.

Última actualización: 2026-08-25, madrugada. Todo el alcance salvo seguridad.

---

## Semáforo

| Módulo | Estado | Tests | Notas |
|---|---|---|---|
| `pliego` | ✅ terminado | 26 | Dominio, motor de tramos, validación y API |
| `padron` | ✅ terminado | 22 | Resolución de nombres y aprendizaje de alias |
| `medicion` | ✅ terminado | 46 | Historia por corte; el vigente es el corte más reciente |
| `multimetrico` | ✅ terminado | (idem) | Incluye el tramo de arriba, accionable |
| `importacion` | ✅ terminado | 8 | Perfil configurable; ninguna fila se pierde |
| `persistencia` | ✅ terminado | 23 | MySQL + Flyway; los tests corren contra H2 en modo MySQL |
| `seguridad` | ⬜ pendiente | — | |
| frontend | ✅ terminado | **0** | En `../multimetrico-frontend`. Cinco pantallas, sin mocks. Sin tests y sin revisión visual |

**Total actual: 125 tests, en verde.**

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
el frontend, "Mi multimétrico" tiene hoy un selector de asesor que hay que
reemplazar por el legajo del token.

**2. Dos fallas conocidas del backend, chicas y con dueño claro.**

- **`Metrica.Unidad` no la expone ninguna respuesta.** El enum lo tiene —TMO en
  `SEGUNDOS`, las otras cuatro en `PORCENTAJE`— pero ni `PliegoResponse.EscalaResponse`
  ni `FilaResponse.CeldaResponse` lo mandan, así que el frontend mantiene un mapa
  propio en `src/lib/format.ts` que duplica algo que el servidor ya sabe. Agregarlo
  a esos dos DTOs y borrar el mapa.
- **`cuadra: true` en una importación `RECHAZADA`.** Un informe rechazado tiene
  todo en cero y cero más cero cierra, así que el campo que existe para responder
  "¿entró todo?" dice que sí cuando no entró nada. El frontend lo esquiva mirando
  `estado`, pero es una fea de la API.

**3. Nadie miró los píxeles de la versión actual del frontend.** El agente que la
terminó no tenía la extensión de Chrome conectada: verificó cada payload contra
los tipos y el build pasa, pero las pantallas cambiaron mucho desde la última vez
que alguien las vio renderizadas. **Es la revisión pendiente más barata y la más
probable de encontrar algo.**

**4. El frontend no tiene un solo test.** No hay infraestructura montada.

---

## Cómo dejarlo andando para mirarlo

Con el contenedor arriba y la app corriendo, la base ya tiene los datos de la
verificación de persistencia: pliego **2026-08 / RESIDENCIAL** y un padrón con dos
equipos, **DIEGO SALAZAR** y **MARTINA REYES**. Hay que usar esos nombres de líder
en el selector; con cualquier otro la API responde 404 y lista los que sí existen.

---

## Hecho

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

- **Sin autenticación.** Ver "Por dónde seguir".
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
