# Handoff — multimetrico-api

Estado vivo del proyecto. Se actualiza cada vez que una pieza queda terminada.
El **qué** y el **por qué** están en `PLAN.md`; acá va el **cómo viene**.

Última actualización: 2026-08-24 (ola 1 completa)

---

## Semáforo

| Módulo | Estado | Tests | Notas |
|---|---|---|---|
| `pliego` | ✅ terminado | 26 | Dominio, motor de tramos, validación y API |
| `padron` | ✅ terminado | 22 | Resolución de nombres y aprendizaje de alias |
| `medicion` | ✅ terminado | 46 | Historia por corte; el vigente es el corte más reciente |
| `multimetrico` | ✅ terminado | (idem) | Incluye el tramo de arriba, accionable |
| `importacion` | ✅ terminado | 8 | Perfil configurable; ninguna fila se pierde |
| `persistencia` | ⬜ pendiente | — | Después de que estabilicen los módulos de arriba |
| `seguridad` | ⬜ pendiente | — | |
| frontend | 🟡 parcial | 0 | En `../multimetrico-frontend`. Compila; falta UI de perfiles y login |

**Total actual: 102 tests, en verde.**

---

## Cómo correrlo

```bash
./mvnw spring-boot:run   # :8082
./mvnw test
```

Todavía no hay base: los repositorios son en memoria, así que al reiniciar se
pierde todo lo cargado. Es a propósito hasta que el modelo se estabilice.

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

- **Un alias sobre un nombre ambiguo puede quedar mal para siempre.** Confirmar
  `SOSA ANA` contra un legajo lo ata globalmente, y en otro archivo podría ser la
  otra persona. Lo correcto es que un pendiente ambiguo se resuelva por archivo y
  no como alias global. **Se arregla cuando entre `importacion`**, que es donde
  tiene sentido.
- **`medicion` no valida legajos contra el padrón, a propósito**: quien traduce
  nombres a legajos es `importacion`. Consecuencia: una medición con un legajo
  fuera del padrón queda guardada y **no aparece en ningún multimétrico**, porque
  la tabla se arma recorriendo el equipo. Se ataja en `importacion`.
- **El frontend re-deriva el tramo de arriba** en `lib/tramos.ts` aunque el
  backend ya lo expone en `FilaResponse`. Son dos implementaciones de la misma
  regla de negocio y se van a despegar. Quedó así porque el frontend se construyó
  en paralelo, contra un PLAN escrito antes de esa decisión.
- **`ManejadorDeErrores` es global pero vive en `pliego/web/`.** Funciona para
  todos los módulos; convendría moverlo a un paquete compartido.
- **Sin persistencia**: todo en memoria.
- **Sin autenticación**: cualquiera ve todo.
- La campaña existe en el pliego pero todavía no la usa nadie más.

---

## Trampas conocidas al escribir tests

**`@SpringBootTest` comparte el contexto de Spring entre clases de test**, y con él
los repositorios en memoria. Las mediciones se acumulan y un pliego cargado no se
puede pisar. **Cada clase de test tiene que usar su propio período** (`YearMonth`)
para no chocar con las demás. Ya rompió tests dos veces.

**El aislamiento por período hace falta por test, no solo por clase.** Dos métodos
de la misma clase que carguen mediciones del mismo asesor y período se ven entre
sí. `ImportacionServiceTest` usa un mes distinto en cada `@BeforeEach`.

**El shell de este entorno es Git Bash sobre Windows** y los heredocs largos se
comen las barras invertidas y truncan los archivos largos. Para escribir Java con
regex o clases de más de ~150 líneas, no uses heredoc.

---

## Para el que sigue

1. Leé `PLAN.md`, sobre todo la sección 3 (decisiones tomadas) y la 4 (convenciones).
2. No inventes datos de prueba a partir de ninguna planilla real. Todo sintético.
3. Antes de dar algo por terminado, verificalo contra la API corriendo, no solo
   con tests.
4. Actualizá este archivo.
