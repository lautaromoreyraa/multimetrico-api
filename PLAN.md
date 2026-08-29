# Plan — multimetrico-api

Documento de requerimientos y contrato compartido. **Todo agente que trabaje en
este proyecto lo lee primero.** Si algo de acá contradice al código, gana este
documento; si el código descubrió que el documento estaba mal, se actualiza el
documento y se deja dicho por qué.

---

## 1. El problema

En una operación de atención al cliente, el desempeño de cada asesor se mide con
cinco métricas. Cada una se convierte en un *alcance* pasándola por una tabla de
tramos. El documento que define esos tramos es el **pliego**, y cambia todos los
meses.

Hoy todo eso vive en una planilla de cálculo de cincuenta hojas, que arma un
supervisor y consultan los asesores para ver cómo vienen. El proceso semanal es:

1. La empresa publica archivos de Excel, uno por métrica, con **todos los equipos**.
2. Cada supervisor los abre y **filtra por su propio nombre** para quedarse con su gente.
3. Pega los números en su planilla, que aplica los tramos del pliego del mes.
4. La planilla arma el **multimétrico**: una fila por asesor con todos sus números.
5. Los asesores lo consultan.

### Los defectos verificados de ese proceso

Se leyó la planilla real fila por fila. No son problemas de prolijidad:

- **El pliego se pisa.** Hay una sola hoja de pliego, la del mes vigente. Los
  anteriores se sobrescribieron, así que **el cálculo de un mes viejo no se puede
  reproducir**.
- **Los tramos permiten huecos y superposiciones.** Se escriben con los dos
  extremos incluidos y el número ya redondeado. En el pliego real hay una banda de
  TMO que va de 669 a 681 junto a otra que arranca en 680: **680 y 681 caen en las
  dos**. Y un FCR de 80,04% no cae en ninguna.
- **Cinco columnas de la hoja de incentivos mezclan texto y número** en la misma
  columna, así que cualquier suma descarta filas en silencio. Es la columna de la
  plata.
- **Los archivos empalman por nombre, no por legajo.** El nombre llega cortado a
  un largo fijo y a veces con la codificación rota. Lo que no coincide no aparece
  en el multimétrico y nadie se entera.
- **Cada métrica tiene su propia fecha de corte** pero el multimétrico las junta
  sin decirlo.

---

## 2. Alcance

### Entra

- Pliego versionado por período, con validación que rechaza tramos rotos.
- Padrón de asesores por período, con líder, y resolución de nombres a legajo.
- Importación de las planillas semanales mediante **perfiles configurables**.
- Mediciones con su fecha de corte, guardando la historia dentro del mes.
- Multimétrico: la vista consolidada, por equipo y por asesor.
- Autenticación: el asesor ve su fila, el supervisor ve su equipo.
- Frontend de lectura.

### No entra

- **El cálculo de incentivos.** Los importes llegan calculados desde la empresa y
  se adjuntan tal cual. El sistema los muestra, no los calcula.
- Integración con los sistemas de origen. La entrada es siempre un archivo.

### Restricción que no se negocia

**Este proyecto no contiene ni contendrá datos reales de ninguna operación.** La
planilla que lo inspiró tiene DNI, números de cuenta, teléfonos de clientes,
comentarios de clientes, nombres de empleados y sus sueldos. Nada de eso entra al
repositorio, ni como fixture, ni como ejemplo, ni "temporalmente".

Todo dato de prueba es sintético y generado por código. Los umbrales que aparecen
en los tests son inventados: tienen la forma de los reales, no sus valores.
`.gitignore` bloquea `*.xlsx`, `*.xls` y `*.csv`.

---

## 3. Decisiones de diseño ya tomadas

No se revisan sin motivo. Están explicadas en el javadoc de cada clase.

| Decisión | Dónde | Por qué |
|---|---|---|
| Tramos como intervalos semiabiertos `[desde, hasta)` | `Banda` | Hace imposibles por construcción los huecos y las superposiciones |
| Extremos abiertos con `null` | `Banda` | Un FCR perfecto de 100% no se cae de la escala |
| `BigDecimal` con `compareTo`, nunca `equals` | todo el dominio | Los valores caen justo sobre los bordes, que es donde el punto flotante falla |
| El validador devuelve **todas** las violaciones | `ValidadorDePliego` | Quien carga el pliego lo corrige de una pasada |
| El pliego cargado **no se pisa** (409) | `PliegoService` | Si se pisa, el mes deja de ser reproducible |
| El padrón **sí** se puede reemplazar | `Padron` | Es una lista de gente que cambia dentro del mes; no es un compromiso |
| No se resuelven nombres por parecido | `ResolutorDeNombres` | Adivinar mal atribuye a una persona las métricas de otra, sin rastro |
| El prefijo no exige borde de palabra | `NombreNormalizado` | El truncado real corta a mitad de palabra; lo que da seguridad es la unicidad |
| El formato de archivo es dato, no código | `importacion` | No hay acceso a los archivos de origen y van a cambiar |

---

## 4. Convenciones de código

- **Java 21, Spring Boot 3.5.4.** Sin Lombok: `record` para todo lo inmutable.
- **Arquitectura en capas** con el dominio adentro.
- **Los DTOs se quedan en el borde** (`web/`). Las entidades no salen del dominio.
- **Repositorios como interfaz** con implementación aparte. Hoy en memoria,
  mañana JPA, sin tocar el dominio.
- **Nombres del dominio en español** — pliego, banda, alcance, padrón, asesor,
  medición — porque son los términos del negocio.
- **Los comentarios explican por qué, no qué.**
- **Los tests nombran el comportamiento**, no el método.
- El puerto es **8082**.

---

## 5. Módulos

### 5.1 `pliego` — HECHO

| Método | Ruta | Respuesta |
|---|---|---|
| `POST` | `/api/pliegos/revision` | `200` con la lista de violaciones (vacía si está sano) |
| `POST` | `/api/pliegos` | `201` / `422` con todas las violaciones / `409` si ya existe |
| `GET` | `/api/pliegos` | Lista |
| `GET` | `/api/pliegos/{aaaa-mm}?campania=` | Uno / `404` |
| `POST` | `/api/pliegos/{aaaa-mm}/evaluacion?campania=` | Evalúa mediciones sueltas |

Métricas: `TMO`, `NPS`, `FCR`, `ACTIVIDAD_COMERCIAL`, `BAJA_CONVERGENTE`.

### 5.2 `padron` — HECHO

| Método | Ruta | Respuesta |
|---|---|---|
| `POST` | `/api/padrones` | `201` |
| `GET` | `/api/padrones/{aaaa-mm}?lider=` | La operación entera, o un equipo |
| `GET` | `/api/padrones/{aaaa-mm}/lideres` | Lista de líderes |
| `POST` | `/api/padrones/{aaaa-mm}/resolucion` | Informe de resolución |
| `POST` | `/api/padrones/{aaaa-mm}/alias` | Confirma un alias |

Estrategias, en orden: `LEGAJO`, `NOMBRE_EXACTO`, `ALIAS_CONFIRMADO`, `PREFIJO_UNICO`.

**Invariante:** `resueltos + pendientes == cantidad de nombres que entraron`.

### 5.3 `medicion` — HECHO

`Medicion(legajo, metrica, periodo, valor, fechaDeCorte, origenImportacion)`

- **Se guarda la historia dentro del mes.** Cada carga semanal es una medición
  nueva, no un `UPDATE`. Permite ver la evolución y explicar por qué cambió un número.
- **El valor vigente de una métrica es el de la fecha de corte más reciente.**
  Con empate, gana la última cargada: dos mediciones del mismo corte son el mismo
  archivo subido dos veces y la segunda es la corrección.
- **Nunca se inventa un valor.** Una métrica sin medición está *faltante* y viaja
  aparte de `alcances`. Eso es lo único que permite distinguir "no hay archivo"
  de "hay un valor que paga cero", y una vez aplastados no se separan más.

  Ojo con la lectura: lo que **no** se inventa es el *valor medido*. Lo que esa
  métrica **cobra** sí es cero — ver 5.4.

| Método | Ruta |
|---|---|
| `POST` | `/api/mediciones` — alta en lote |
| `GET` | `/api/mediciones/{aaaa-mm}?legajo=&metrica=` — historia |

### 5.4 `multimetrico` — HECHO

`FilaDeMultimetrico(asesor, alcances[], alcanceTotal, faltantes[], cortesPorMetrica)`

- Para un período y un líder: toma el equipo del padrón, busca la medición vigente
  de cada métrica, la evalúa contra el pliego **de ese período** y arma la fila.
- **Cada métrica lleva su "actualizado al".** Es lo que la planilla pierde al
  juntar métricas de cortes distintos.
- **Una métrica sin archivo bonifica cero.** Si al día 10 no llegó el NPS, el
  NPS no paga, y la fila entra en los promedios del equipo como cualquier otra.
  `alcanceTotal` suma sólo lo presente, que es idéntico a sumar cero por lo
  ausente: el cálculo no necesita saber de esta regla.
- Las faltantes igual viajan aparte, para que quien muestre la fila pueda dejar el
  valor vacío en vez de escribir un 0 que afirmaría que se midió cero.
- **Una métrica tiene un solo corte vigente por mes.** Cada archivo trae a todo el
  equipo con la misma fecha, así que dentro de una columna no puede haber dos
  fechas conviviendo. **Todavía no está validado en el backend.**

| Método | Ruta |
|---|---|
| `GET` | `/api/multimetrico/{aaaa-mm}?lider=&campania=` |
| `GET` | `/api/multimetrico/{aaaa-mm}/asesores/{legajo}?campania=` |

### 5.5 `importacion` — HECHO

**El formato de los archivos de origen es desconocido y va a cambiar**, así que el
mapeo es configuración en tiempo de ejecución, no código.

`PerfilDeImportacion(nombre, metrica, hoja, filaEncabezados, columnaAsesor, columnaValor, columnaLider, formatoNumero)`

1. `POST` del archivo → responde al toque con un id. No bloquea.
2. Procesa: lee según el perfil, resuelve cada nombre contra el padrón, arma mediciones.
3. **Nada se descarta en silencio.** Cada fila termina en importada, rechazada
   (con número de fila y motivo) o pendiente de resolución.
4. **Si el archivo no coincide con su perfil**, la importación se rechaza entera
   diciendo qué esperaba y qué encontró.

| Método | Ruta |
|---|---|
| `POST` | `/api/perfiles` |
| `GET` | `/api/perfiles` |
| `POST` | `/api/perfiles/inspeccion` — sube un archivo y devuelve columnas con ejemplos |
| `POST` | `/api/importaciones?perfil=&periodo=&corte=` |
| `GET` | `/api/importaciones/{id}` — estado, rechazos y pendientes |

Librería: Apache POI para `.xlsx`.

### 5.6 `seguridad` — POR HACER

- `ASESOR`: ve **solo su fila**. Acceso por fila, no por endpoint.
- `SUPERVISOR`: ve su equipo, carga pliegos, padrones e importaciones.
- JWT, contraseñas con BCrypt.
- Un asesor que pide la fila de otro recibe `403`, nunca la fila.

### 5.7 `persistencia` — HECHO

- MySQL con **Flyway** para el esquema. `compose.yaml` levanta la base en el
  **3307**, para no chocar con un MySQL instalado en la máquina.
- Las implementaciones JPA entraron detrás de las interfaces que ya existían, en
  `persistencia/`. El dominio no se tocó: los records siguen intactos y las
  entidades JPA son clases aparte con su mapeo de ida y vuelta.
- Los tests corren contra **H2 en modo MySQL**, con las mismas migraciones. No
  hace falta Docker ni base levantada para `./mvnw test`.

Tres cosas que no se pueden aflojar, cada una anotada en el código:

- **Las mediciones se ordenan por `id`, nunca por `fecha_de_corte`.** El id es el
  orden de carga, que es lo que desempata dos mediciones del mismo corte.
- **Todo `DECIMAL` lleva precisión y escala** (`DECIMAL(18,6)`). Sin escala, MySQL
  redondea a entero y los valores dejan de caer en la banda que les toca.
- **Las claves de búsqueda van en columnas normalizadas** (`campania_clave`,
  `legajo_clave`, `nombre_clave`) en vez de depender de la collation, que compara
  distinto en MySQL que en H2.

### 5.8 Frontend — HECHO

Proyecto aparte: `../multimetrico-frontend`. React + Vite + TypeScript.

1. **Multimétrico** (supervisor): el equipo entero, con dos columnas por métrica
   —el valor medido y lo que paga— y el promedio del líder fijo al pie.
2. **Mis métricas** (asesor): sus métricas contra otro mes a elección, y cómo se
   movió cada una dentro del mes.
3. **Pliego**: cargar el del mes y **ver los errores de validación**. Es la
   pantalla que demuestra el proyecto.
4. **Importación**: subir el archivo semanal y ver el informe.
5. **Perfiles**: definir cómo se lee un archivo nuevo.

**Es un frontend de lectura y carga, no un dashboard.** Sin gráficos de torta. El
valor está en el backend; la pantalla tiene que dejarlo ver.

---

## 6. Orden de trabajo

1. ~~`medicion` + `multimetrico`~~ — el producto empieza a existir
2. ~~`importacion`~~ — deja de depender de cargar a mano
3. ~~`persistencia`~~ — sobrevive a un reinicio
4. ~~Frontend~~ — se puede mostrar
5. **`seguridad` — cada uno ve lo suyo.** Lo único que queda.

El frontend se adelantó al orden original y fue lo correcto: mostrar la tabla es
lo que hizo aparecer los problemas de criterio que ningún test iba a encontrar.

---

## 7. Definición de terminado

- Tests que nombran comportamiento, incluidos los bordes.
- `./mvnw test` pasa entero.
- Verificado contra la API corriendo, no solo con tests.
- Las decisiones no obvias, explicadas en el código.
- Si toca pantalla, **mirado en el navegador**. La única falla real que encontró
  la primera revisión visual había pasado el `tsc` y el build sin problema.
- `HANDOFF.md` actualizado.
- Commits **sin firma de agente**.
