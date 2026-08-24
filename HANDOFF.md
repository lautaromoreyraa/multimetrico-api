# Handoff — multimetrico-api

Estado vivo del proyecto. Se actualiza cada vez que una pieza queda terminada.
El **qué** y el **por qué** están en `PLAN.md`; acá va el **cómo viene**.

Última actualización: 2026-08-24

---

## Semáforo

| Módulo | Estado | Tests | Notas |
|---|---|---|---|
| `pliego` | ✅ terminado | 26 | Dominio, motor de tramos, validación y API |
| `padron` | ✅ terminado | 22 | Resolución de nombres y aprendizaje de alias |
| `medicion` | ⏳ en curso | — | Agente `backend-mediciones` |
| `multimetrico` | ⏳ en curso | — | Agente `backend-mediciones` |
| `importacion` | ⏳ en curso | — | Agente `backend-importacion` |
| `persistencia` | ⬜ pendiente | — | Después de que estabilicen los módulos de arriba |
| `seguridad` | ⬜ pendiente | — | |
| frontend | ⏳ en curso | — | Agente `frontend`, en `../multimetrico-frontend` |

**Total actual: 48 tests, en verde.**

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

---

## Decisiones que se corrigieron sobre la marcha

Se dejan anotadas porque la intuición tira para el otro lado y alguien va a querer
"arreglarlas" de vuelta.

**El prefijo de un nombre no exige borde de palabra.** Al principio sí lo exigía, y
un test lo tumbó: el truncado real corta a mitad de palabra (`AGUIRRE FLAVIA Y`),
así que la regla rechazaba justo el caso más común. Lo que da seguridad es la
unicidad, no dónde cae el corte.

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
- **Sin persistencia**: todo en memoria.
- **Sin autenticación**: cualquiera ve todo.
- La campaña existe en el pliego pero todavía no la usa nadie más.

---

## Para el que sigue

1. Leé `PLAN.md`, sobre todo la sección 3 (decisiones tomadas) y la 4 (convenciones).
2. No inventes datos de prueba a partir de ninguna planilla real. Todo sintético.
3. Antes de dar algo por terminado, verificalo contra la API corriendo, no solo
   con tests.
4. Actualizá este archivo.
