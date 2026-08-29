# multimetrico-api

Centraliza las métricas semanales de un equipo de atención al cliente y arma el
**multimétrico**: la fila donde cada asesor ve cómo viene en cada indicador.

## El problema

En una operación de atención, el desempeño de cada asesor se mide con varias
métricas —tiempo medio de operación, NPS, resolución en el primer contacto,
actividad comercial, bajas— y cada una se convierte en un *alcance* pasándola por
una tabla de tramos. El documento que define esos tramos es el **pliego**, y
cambia todos los meses.

Hoy eso vive en una planilla de cálculo de cincuenta hojas. Tiene tres problemas
que no son de prolijidad:

**El pliego se pisa.** Hay una sola hoja de pliego: la del mes vigente. Los
anteriores se sobrescribieron, así que el cálculo de un mes viejo no se puede
reproducir — aplicarle los tramos de hoy a los datos de marzo da otro número.

**Los tramos se escriben mal y nadie se entera.** Se anotan con los dos extremos
incluidos y con el número ya redondeado para mostrar: `0–642`, después `643–655`;
`79,5%–80,0%`, después `80,1%–81,1%`. Eso deja huecos —un valor de 80,04% no cae
en ningún tramo— y permite superposiciones —un tramo que termina en 681 junto a
otro que arranca en 680—. La planilla no rechaza ninguna de las dos: resuelve por
la primera coincidencia, en silencio.

**No se puede saber por qué cambió un número.** Si los objetivos se mueven todos
los meses, un asesor puede tener el mismo NPS en julio y en agosto y cobrar
distinto. La planilla muestra el resultado, no la regla que lo produjo.

## Cómo lo resuelve

**El pliego es una entidad versionada.** Está atado a su período y no se pisa.
Recalcular marzo usa el pliego de marzo, siempre. De ahí sale la lectura que la
planilla no puede dar: separar cuánto se movió el asesor de cuánto se movió la
vara.

**Los tramos son intervalos semiabiertos `[desde, hasta)`.** El `hasta` de uno es
el `desde` del siguiente, así que los huecos y las superposiciones dejan de ser
posibles por construcción. Los extremos van abiertos (`null`), de modo que un FCR
perfecto de 100% no se caiga de la escala.

**El pliego se valida al cargarlo y se rechaza entero.** Devuelve la lista
completa de problemas —no el primero— para poder corregirlo de una pasada:
superposiciones, huecos, escalas que no cubren los extremos, pesos que no
coinciden con el mejor tramo, métricas duplicadas.

**Cada resultado viaja con su tramo.** No "tu NPS te dio 2,0%", sino "tu NPS de
55,4% cayó en `[53,2%, 57,3%)`, que paga 2,0%". Un número que se puede discutir.

## Correr

```bash
docker compose up -d               # MySQL en el 3307
./mvnw spring-boot:run             # la API en el 8082
node scripts/datos-de-ejemplo.mjs  # un mes cargado, para poder mirar algo

cd ../multimetrico-frontend && npm run dev   # la pantalla, en el 5174
```

`./mvnw test` corre los 134 tests y **no necesita la base levantada**: usan H2 en
modo de compatibilidad MySQL con las mismas migraciones de Flyway. La aplicación
sí la necesita.

El tercer comando es el que separa ver la pantalla de ver un 404: la primera
pantalla es la tabla del equipo entero, y sin pliego, padrón y mediciones no hay
nada que mostrar.

Carga **de enero al mes en curso** —quince asesores, dos líderes, las cinco
métricas, un pliego y un padrón por mes— en la campaña `CONVERGENTE`. Varios
meses y no uno solo porque "Mis métricas" compara contra el mes anterior. No
carga meses futuros: serían mediciones con una fecha de corte que todavía no
ocurrió. Para otro rango, `node scripts/datos-de-ejemplo.mjs 2026-01 2026-12`.

Tres cosas que hace a propósito y no son ruido:

- **Cuatro cargas por métrica y por mes**, una por semana. Cada carga es una
  medición nueva y no un `UPDATE`, que es lo que deja reconstruir por qué cambió
  un número dentro del mes.
- **Un solo corte vigente por métrica.** Cada archivo trae a todo el equipo con
  la misma fecha, así que en un momento dado no puede haber dos fechas
  conviviendo en una columna. El día sí cambia entre métricas, porque depende de
  cuándo mande cada archivo la empresa.
- En el último mes, **tres personas sin archivo** en alguna métrica —y salteadas
  en las cuatro semanas, no sólo en la última—, que es lo que ejercita la regla
  de que una métrica sin archivo bonifica cero.

Correrlo dos veces es inofensivo.

El contenedor puede existir apagado de una sesión anterior: si `compose up`
protesta que el nombre está en uso, `docker start multimetrico-mysql` lo revive
**con sus datos**. `docker compose down -v` lo borra de cero.

## Endpoints

| Método | Ruta | Para qué |
|---|---|---|
| `POST` | `/api/pliegos/revision` | Revisa un pliego sin guardarlo |
| `POST` | `/api/pliegos` | Carga el pliego del mes (valida y rechaza si está roto) |
| `GET` | `/api/pliegos` | Lista los pliegos cargados |
| `GET` | `/api/pliegos/{aaaa-mm}?campania=` | Trae uno |
| `POST` | `/api/pliegos/{aaaa-mm}/evaluacion?campania=` | Evalúa mediciones contra ese pliego |

Un pliego con un tramo superpuesto:

```bash
curl -X POST localhost:8082/api/pliegos -H 'Content-Type: application/json' -d '{
  "periodo": "2026-09", "campania": "CONVERGENTES",
  "escalas": [{ "metrica": "TMO", "peso": 3, "bandas": [
    { "desde": null, "hasta": 681,  "resultado": 3 },
    { "desde": 680,  "hasta": null, "resultado": 0 }
  ]}]
}'
```

```
422  las bandas [-∞, 681) y [680, +∞) se pisan entre 680 y 681:
     un valor ahí cae en las dos
```

## El padrón

Los archivos de métricas **no traen legajo**: traen el nombre escrito por otro
sistema, cortado a un largo fijo y a veces con la codificación rota. Toda la
cadena empalma por ese texto, así que cuando un nombre no coincide, la persona
simplemente no aparece en el multimétrico — y se entera cuando reclama.

El padrón le pone identidad a eso. Guarda la operación entera con el líder de
cada asesor, de modo que **el filtrado por equipo lo hace el sistema**, no una
persona abriendo cada archivo y filtrando por su propio nombre todas las semanas.

Para resolver un nombre se prueban cuatro vías, de la más segura a la menos, y la
primera que da resultado gana: legajo, nombre exacto normalizado, alias ya
confirmado, y prefijo único.

**Lo que no hace es adivinar.** Hay una medida de parecido, pero se usa solo para
sugerirle candidatos a una persona. En un cálculo que termina en cuánto cobra
alguien, resolver por similitud significa poder atribuirle a una persona las
métricas de otra, y sin dejar rastro de que fue una suposición. Tampoco resuelve
cuando hay empate: `SOSA ANA` es principio de `SOSA ANA MARIA` y de
`SOSA ANABELA ROSA`, así que queda pendiente con los dos candidatos.

Cuando una persona confirma un caso, **el sistema lo aprende**: el alias queda
guardado contra el legajo y no vuelve a preguntar por ese texto nunca más.

La propiedad que sostiene todo: `resueltos + pendientes` es siempre igual a la
cantidad de filas que entraron. Nada se descarta en silencio.

```
POST /api/padrones/2026-08/resolucion
{ "nombres": ["AGUIRRE FLAVIA Y", "u300", "SOSA ANA", "PEREZ QUIENSABE"] }
```

```json
{ "total": 4, "resueltos": 2, "pendientes": 2, "completa": false,
  "porEstrategia": { "PREFIJO_UNICO": 1, "LEGAJO": 1 },
  "sinResolver": [
    { "texto": "SOSA ANA", "motivo": "el nombre viene cortado y coincide con más de un asesor",
      "candidatos": [ {"legajo":"u300","nombre":"SOSA ANA MARIA"},
                      {"legajo":"u400","nombre":"SOSA ANABELA ROSA"} ] },
    { "texto": "PEREZ QUIENSABE", "motivo": "no se parece a nadie del padrón", "candidatos": [] }
  ]}
```

| Método | Ruta | Para qué |
|---|---|---|
| `POST` | `/api/padrones` | Carga el padrón del período |
| `GET` | `/api/padrones/{aaaa-mm}?lider=` | La operación entera, o un equipo |
| `GET` | `/api/padrones/{aaaa-mm}/lideres` | Los líderes del período |
| `POST` | `/api/padrones/{aaaa-mm}/resolucion` | Cruza nombres contra el padrón, sin importar nada |
| `POST` | `/api/padrones/{aaaa-mm}/alias` | Una persona confirma un texto; el sistema lo aprende |

## Estado

**El MVP anda de punta a punta.** Pliego, padrón, mediciones con su historia,
multimétrico de equipo y de asesor, importación por perfil configurable y
persistencia en MySQL con Flyway. 134 tests en verde, y las cinco pantallas del
frontend verificadas en el navegador contra la API corriendo.

La primera pantalla es el multimétrico del equipo: una fila por asesor y, por
cada métrica, **dos columnas** —el valor medido y lo que ese valor paga—, con la
fecha de corte de esa métrica en el encabezado y el promedio del equipo fijo al
pie.

Una métrica sin archivo **bonifica cero**: es lo que se cobra si el archivo no
llegó. Lo que no se rellena es el valor medido, que se muestra vacío — escribir
un 0 ahí diría que se midió cero, que es una afirmación sobre el desempeño de
alguien que nadie hizo.

Los tres estados de una bonificación se leen de un barrido, por color y también
por el número: **suma** en acento, **cero** en ámbar y **resta** en rojo. El cero
va en ámbar y no en gris porque no es la ausencia de un dato: es un resultado, y
encima el que conviene ir a mirar.

La segunda pantalla, **Mis métricas**, compara a una persona contra el mes
anterior y muestra al pie cómo se movió cada métrica dentro del mes, carga por
carga, con la vigente marcada.

**Lo que falta es autenticación** (`PLAN.md` §5.6). Hoy cualquiera ve la fila de
cualquiera, en la API y en la pantalla, y el selector de asesor de "Mi
multimétrico" tendría que ser el legajo del token. El control tiene que ser **por
fila y no por endpoint**: un asesor que pide la fila de otro recibe 403, nunca la
fila.

## Sobre los datos

Este proyecto está modelado sobre un proceso real, pero **no contiene datos de
ninguna operación**. Los umbrales que aparecen en los tests son inventados: los
de una empresa son objetivos internos y no van en un repositorio. El sistema se
construye y se prueba con datos sintéticos.
