/**
 * Carga un mes completo de datos de ejemplo contra la API corriendo.
 *
 *   node scripts/datos-de-ejemplo.mjs            # 2026-08 / RESIDENCIAL
 *   node scripts/datos-de-ejemplo.mjs 2026-09 MOVIL
 *
 * Existe porque el MVP no se puede mirar con la base vacia: la primera pantalla
 * es una tabla del equipo entero y sin pliego responde 404, sin padron no tiene
 * filas y sin mediciones tiene filas con todo en blanco. Esto deja las tres
 * cosas cargadas de una.
 *
 * TODO EL CONTENIDO ES INVENTADO. El PLAN seccion 2 lo exige y aca se cumple: no
 * entra ni un dato de ninguna operacion real, ni como ejemplo ni de prueba. Los
 * umbrales tienen la forma de los reales -el orden de magnitud, la cantidad de
 * tramos- pero no sus valores, y los nombres no son de nadie.
 *
 * Los valores se generan con un pseudoazar sembrado por legajo, asi que correr
 * el script dos veces produce exactamente los mismos numeros. Eso importa: un
 * seed que cambia en cada corrida hace que "lo que veo hoy" no sea comparable
 * con la captura de ayer.
 */

const API = process.env.MULTIMETRICO_API ?? 'http://localhost:8082'
const PERIODO = process.argv[2] ?? '2026-08'
const CAMPANIA = process.argv[3] ?? 'RESIDENCIAL'

/* -- el pliego ------------------------------------------------------------ */

/**
 * El mismo "pliego sano" que ofrece el editor del frontend.
 *
 * Pasa el validador del backend: intervalos semiabiertos que empalman exactos,
 * extremo abierto arriba y abajo en cada escala, y el peso de cada una igual a
 * lo que paga su mejor banda. El alcance maximo da 13.
 */
const PLIEGO = {
  periodo: PERIODO,
  campania: CAMPANIA,
  escalas: [
    {
      metrica: 'TMO',
      peso: '3.5',
      bandas: [
        { desde: null, hasta: '620', resultado: '3.5' },
        { desde: '620', hasta: '645', resultado: '2.8' },
        { desde: '645', hasta: '670', resultado: '2.0' },
        { desde: '670', hasta: '695', resultado: '1.0' },
        { desde: '695', hasta: null, resultado: '0' },
      ],
    },
    {
      metrica: 'NPS',
      peso: '3.5',
      bandas: [
        { desde: null, hasta: '45', resultado: '0' },
        { desde: '45', hasta: '50', resultado: '1.0' },
        { desde: '50', hasta: '55', resultado: '2.0' },
        { desde: '55', hasta: '60', resultado: '2.8' },
        { desde: '60', hasta: null, resultado: '3.5' },
      ],
    },
    {
      metrica: 'FCR',
      peso: '3.0',
      bandas: [
        { desde: null, hasta: '76', resultado: '0' },
        { desde: '76', hasta: '79.5', resultado: '1.0' },
        { desde: '79.5', hasta: '80.1', resultado: '1.8' },
        { desde: '80.1', hasta: '82', resultado: '2.4' },
        { desde: '82', hasta: null, resultado: '3.0' },
      ],
    },
    {
      metrica: 'ACTIVIDAD_COMERCIAL',
      peso: '2.0',
      bandas: [
        { desde: null, hasta: '60', resultado: '0' },
        { desde: '60', hasta: '75', resultado: '0.8' },
        { desde: '75', hasta: '90', resultado: '1.4' },
        { desde: '90', hasta: null, resultado: '2.0' },
      ],
    },
    {
      metrica: 'BAJA_CONVERGENTE',
      peso: '1.0',
      bandas: [
        { desde: null, hasta: '1.2', resultado: '1.0' },
        { desde: '1.2', hasta: '2', resultado: '0.6' },
        { desde: '2', hasta: '3', resultado: '0.3' },
        { desde: '3', hasta: null, resultado: '0' },
      ],
    },
  ],
}

/* -- el padron ------------------------------------------------------------ */

const LIDERES = ['MARTINA REYES', 'DIEGO SALAZAR']

const NOMBRES = [
  ['10412', 'AGUIRRE FLAVIA YAMILA'],
  ['10419', 'BENITEZ RAMIRO ESTEBAN'],
  ['10427', 'CABRERA LUCIA MARIEL'],
  ['10433', 'DOMINGUEZ NAHUEL ALEJO'],
  ['10441', 'ESPINDOLA CAROLINA BELEN'],
  ['10448', 'FERREYRA MATIAS EZEQUIEL'],
  ['10455', 'GONZALEZ AILEN SOFIA'],
  ['10460', 'HERRERA JOAQUIN IGNACIO'],
  ['10468', 'IBARRA MICAELA ROCIO'],
  ['10474', 'JUAREZ FEDERICO ANDRES'],
  ['10481', 'LEIVA VALENTINA PAZ'],
  ['10489', 'MEDINA GONZALO RUBEN'],
  ['10495', 'NUNEZ AGUSTINA LUZ'],
  ['10502', 'OJEDA SEBASTIAN GABRIEL'],
  ['10510', 'AGUIRRE FACUNDO NICOLAS'],
]

// Ocho para el primer lider y siete para el segundo: equipos de tamano distinto,
// que es lo que obliga a que la pantalla no asuma una grilla pareja.
const ASESORES = NOMBRES.map(([legajo, nombre], i) => ({
  legajo,
  nombre,
  lider: i < 8 ? LIDERES[0] : LIDERES[1],
}))

/* -- las mediciones ------------------------------------------------------- */

/**
 * Pseudoazar sembrado: mismo legajo y misma metrica dan siempre el mismo valor.
 *
 * Es un hash entero chico, no un generador serio. Lo unico que se le pide es
 * repartir sin agrupar todo en el mismo tramo y ser reproducible entre
 * corridas y entre maquinas.
 */
function sembrado(semilla) {
  let h = 2166136261
  for (const c of semilla) {
    h ^= c.charCodeAt(0)
    h = Math.imul(h, 16777619)
  }
  // >>> 0 para sacarle el signo: Math.imul devuelve un entero de 32 bits con
  // signo y aca solo se quiere la fraccion.
  return ((h >>> 0) % 10000) / 10000
}

/**
 * Los rangos de los que se sortea cada metrica.
 *
 * Van a proposito mas anchos que un solo tramo del pliego: lo que hace legible
 * la tabla es que las filas caigan en bandas distintas, con gente arriba, gente
 * al borde y gente abajo. Un equipo entero en la misma banda no muestra nada.
 */
const RANGOS = {
  TMO: [598, 712, 0],
  NPS: [41, 68, 1],
  FCR: [74, 84, 1],
  ACTIVIDAD_COMERCIAL: [55, 96, 1],
  BAJA_CONVERGENTE: [0.4, 3.4, 1],
}

function valorDe(legajo, metrica) {
  const [min, max, decimales] = RANGOS[metrica]
  const bruto = min + sembrado(`${legajo}:${metrica}`) * (max - min)
  return bruto.toFixed(decimales)
}

const CORTE = `${PERIODO}-22`
const CORTE_VIEJO = `${PERIODO}-15`

/**
 * Lo que falta a proposito.
 *
 * Un MVP donde todas las celdas tienen numero no muestra la mitad del proyecto.
 * Las dos ausencias de abajo son las dos situaciones que la planilla esconde y
 * que esta tabla existe para hacer visibles:
 *
 *  - `SIN_MEDICION`: a dos personas no les llego el archivo de FCR y a una el de
 *    baja convergente. Sus filas quedan incompletas y el total, explicitamente
 *    parcial: no se rellena con cero.
 *  - `CORTE_ATRASADO`: dos personas quedaron afuera del ultimo archivo de NPS y
 *    siguen mostrando el numero de la semana anterior. Eso hace que la columna
 *    tenga dos fechas de corte, que es lo que dispara el aviso de arriba de la
 *    tabla.
 */
const SIN_MEDICION = { FCR: ['10433', '10495'], BAJA_CONVERGENTE: ['10460'] }
const CORTE_ATRASADO = { NPS: ['10419', '10502'] }

function mediciones() {
  const items = []
  for (const metrica of Object.keys(RANGOS)) {
    for (const { legajo } of ASESORES) {
      if ((SIN_MEDICION[metrica] ?? []).includes(legajo)) continue
      const atrasado = (CORTE_ATRASADO[metrica] ?? []).includes(legajo)
      items.push({
        legajo,
        metrica,
        valor: valorDe(legajo, metrica),
        // La fecha por item pisa la del lote. Es el mismo escape que usa el
        // importador cuando sube varias metricas con cortes distintos.
        fechaDeCorte: atrasado ? CORTE_VIEJO : null,
      })
    }
  }
  return { periodo: PERIODO, fechaDeCorte: CORTE, origenImportacion: null, mediciones: items }
}

/* -- el envio ------------------------------------------------------------- */

async function postear(ruta, cuerpo, que) {
  const respuesta = await fetch(`${API}${ruta}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(cuerpo),
  })

  const texto = await respuesta.text()
  if (respuesta.ok) {
    console.log(`  ok   ${que} (HTTP ${respuesta.status})`)
    return true
  }

  // El 409 no es un fallo del script: es que el dato ya estaba. Correrlo dos
  // veces tiene que ser inofensivo, porque la forma normal de usarlo es
  // correrlo cuando no se sabe si la base esta cargada.
  if (respuesta.status === 409) {
    console.log(`  ya estaba   ${que} (HTTP 409)`)
    return true
  }

  console.error(`  FALLO   ${que} (HTTP ${respuesta.status})`)
  console.error(`  ${texto}`)
  return false
}

async function main() {
  console.log(`Cargando datos de ejemplo en ${API} para ${PERIODO} / ${CAMPANIA}\n`)

  try {
    await fetch(`${API}/api/pliegos`)
  } catch {
    console.error(`No se pudo contactar la API en ${API}.`)
    console.error('Levantala con:  docker compose up -d  &&  ./mvnw spring-boot:run')
    process.exit(1)
  }

  let bien = true
  bien = (await postear('/api/pliegos', PLIEGO, 'pliego')) && bien
  bien = (await postear('/api/padrones', { periodo: PERIODO, asesores: ASESORES }, 'padron')) && bien
  bien = (await postear('/api/mediciones', mediciones(), 'mediciones')) && bien

  if (!bien) process.exit(1)

  console.log(`\nListo. La pantalla de bonificaciones ya tiene ${ASESORES.length} filas.`)
  console.log(`Abrila con el periodo ${PERIODO} y la campania ${CAMPANIA}.`)
}

main()
