/**
 * Carga varios meses de datos de ejemplo contra la API corriendo.
 *
 *   node scripts/datos-de-ejemplo.mjs                  # 2026-01 .. el mes actual
 *   node scripts/datos-de-ejemplo.mjs 2026-01 2026-12  # un rango a mano
 *   node scripts/datos-de-ejemplo.mjs 2026-03 2026-03  # un solo mes
 *
 * Existe porque el MVP no se puede mirar con la base vacia: la primera pantalla
 * es una tabla del equipo entero y sin pliego responde 404, sin padron no tiene
 * filas y sin mediciones tiene filas con todo en blanco. Esto deja las tres
 * cosas cargadas, y para varios meses seguidos, que es lo que necesita la
 * comparacion contra el mes anterior de "Mis metricas".
 *
 * Por defecto llega hasta el mes en curso y no mas alla: cargar meses futuros
 * daria mediciones con fecha de corte que todavia no ocurrio, que es justo la
 * clase de dato que despues nadie sabe si creer.
 *
 * TODO EL CONTENIDO ES INVENTADO. El PLAN seccion 2 lo exige y aca se cumple: no
 * entra ni un dato de ninguna operacion real. Los umbrales tienen la forma de
 * los reales -el orden de magnitud, la cantidad de tramos- pero no sus valores,
 * y los nombres no son de nadie.
 */

import { libroDeUnaHoja } from './xlsx.mjs'

const API = process.env.MULTIMETRICO_API ?? 'http://localhost:8082'
const CAMPANIA = process.env.MULTIMETRICO_CAMPANIA ?? 'CONVERGENTE'

// Cargar datos es de administrador, asi que el script entra como uno. Los
// defaults son los del entorno de desarrollo; contra otra instalacion hay que
// pasarlos:
//
//   ADMIN_LEGAJO=admin ADMIN_PASSWORD=... node scripts/datos-de-ejemplo.mjs
const ADMIN_LEGAJO = process.env.ADMIN_LEGAJO ?? 'admin'
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD ?? 'admin'

/** Lo completa el login y viaja en cada POST. */
let token = null

/* -- que meses -------------------------------------------------------------- */

const hoy = new Date()
const MES_ACTUAL = `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}`

const DESDE = process.argv[2] ?? `${hoy.getFullYear()}-01`
const HASTA = process.argv[3] ?? MES_ACTUAL

function* meses(desde, hasta) {
  let [anio, mes] = desde.split('-').map(Number)
  const [anioFin, mesFin] = hasta.split('-').map(Number)
  while (anio < anioFin || (anio === anioFin && mes <= mesFin)) {
    yield `${anio}-${String(mes).padStart(2, '0')}`
    mes += 1
    if (mes > 12) {
      mes = 1
      anio += 1
    }
  }
}

/* -- el pliego -------------------------------------------------------------- */

/**
 * Las escalas del mes. Son las mismas todos los meses a proposito.
 *
 * En la operacion real el pliego cambia mes a mes, y el proyecto entero existe
 * para que eso no rompa la reproducibilidad. Pero para mirar la pantalla
 * conviene que no cambie: si se mueven los umbrales Y los valores, no se puede
 * saber si alguien mejoro o si le corrieron la vara. Con el pliego quieto, la
 * comparacion contra el mes anterior mide lo unico que se movio.
 *
 * Pasa el validador: intervalos semiabiertos que empalman exactos, extremo
 * abierto arriba y abajo en cada escala, y el peso de cada una igual a lo que
 * paga su mejor banda. El alcance maximo da 13.
 */
const ESCALAS = [
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
]

const METRICAS = ESCALAS.map((e) => e.metrica)

/* -- el padron -------------------------------------------------------------- */

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

/* -- los valores ------------------------------------------------------------ */

/**
 * Pseudoazar sembrado: la misma semilla da siempre el mismo numero.
 *
 * Es un hash entero chico, no un generador serio. Lo unico que se le pide es
 * repartir sin agrupar todo en el mismo tramo y ser reproducible entre corridas
 * y entre maquinas: un seed que cambia en cada corrida hace que "lo que veo hoy"
 * no sea comparable con la captura de ayer.
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
 * Los rangos de los que se sortea cada metrica: [min, max, decimales].
 *
 * Van a proposito mas anchos que un solo tramo del pliego. Lo que hace legible
 * la tabla es que las filas caigan en bandas distintas, con gente arriba, gente
 * al borde y gente abajo; un equipo entero en la misma banda no muestra nada.
 */
const RANGOS = {
  TMO: [598, 712, 0],
  NPS: [41, 68, 1],
  FCR: [74, 84, 1],
  ACTIVIDAD_COMERCIAL: [55, 96, 1],
  BAJA_CONVERGENTE: [0.4, 3.4, 1],
}

/** Cuantas veces al mes llega el archivo de una metrica. Una por semana. */
const SEMANAS = 4

/**
 * El valor de cierre de un asesor en una metrica y un mes.
 *
 * No es ruido puro mes a mes: se mezcla una linea de base propia del asesor
 * -que pesa el doble- con un movimiento del mes. Si cada mes fuera un sorteo
 * independiente, la comparacion contra el mes anterior no diria nada, porque
 * nadie "viene mejorando": cada mes seria una persona distinta. Asi, cada uno
 * tiene su nivel y se mueve alrededor.
 */
function valorDeCierre(legajo, metrica, periodo) {
  const [min, max] = RANGOS[metrica]
  const base = sembrado(`${legajo}:${metrica}`)
  const delMes = sembrado(`${legajo}:${metrica}:${periodo}`)
  return min + ((base * 2 + delMes) / 3) * (max - min)
}

/**
 * El valor de una semana concreta.
 *
 * Las semanas previas se sacuden alrededor del valor de cierre y se le van
 * acercando: la ultima **es** el cierre, exacto. Asi la metrica se mueve dentro
 * del mes -que es lo que el historial existe para mostrar- sin que el numero que
 * finalmente cuenta dependa de cuantas semanas se hayan cargado.
 */
function valorDe(legajo, metrica, periodo, semana) {
  const [min, max, decimales] = RANGOS[metrica]
  const cierre = valorDeCierre(legajo, metrica, periodo)
  if (semana === SEMANAS - 1) return cierre.toFixed(decimales)

  const desvio = sembrado(`${legajo}:${metrica}:${periodo}:s${semana}`) - 0.5
  const cuanto = (SEMANAS - 1 - semana) / (SEMANAS - 1)
  const crudo = cierre + desvio * cuanto * (max - min) * 0.22
  return Math.min(Math.max(crudo, min), max).toFixed(decimales)
}

/**
 * Las fechas de corte de una metrica en un mes: una por semana.
 *
 * Cada archivo trae a **todo el equipo** con el mismo corte, asi que en un
 * momento dado no puede haber dos fechas conviviendo dentro de una metrica: la
 * vigente de todos es la del ultimo archivo. Lo que si cambia es el dia entre
 * una metrica y otra, porque depende de cuando la empresa mande cada una.
 *
 * El primer dia cae entre el 1 y el 7 para que las cuatro semanas entren en
 * febrero tambien.
 */
function cortesDe(metrica, periodo) {
  const primero = 1 + Math.floor(sembrado(`corte:${metrica}:${periodo}`) * 7) // 1..7
  return Array.from({ length: SEMANAS }, (_, semana) => {
    const dia = primero + semana * 7
    return `${periodo}-${String(dia).padStart(2, '0')}`
  })
}

/**
 * Lo que falta a proposito, y solo en el ultimo mes del rango.
 *
 * Una metrica sin archivo **bonifica 0**: no se rellena con un valor inventado
 * ni se saca a la persona del promedio. Que pase solo en el mes en curso es lo
 * realista -los meses cerrados llegaron completos, el que esta corriendo
 * todavia no- y ademas deja la comparacion contra el mes anterior con algo que
 * mostrar: alguien que el mes pasado cobraba NPS y este mes todavia no.
 *
 * **Se los saltea en las cuatro semanas, no solo en la ultima.** Si aparecieran
 * en las primeras y faltaran en la ultima, su vigente seria el corte viejo y la
 * columna terminaria con dos fechas conviviendo, que es justo lo que no puede
 * pasar. Sin archivo en todo el mes, no tienen medicion y punto.
 */
const SIN_ARCHIVO = { NPS: ['10433', '10495'], BAJA_CONVERGENTE: ['10460'] }

function mediciones(periodo, metrica, semana, corte, esUltimoMes) {
  const sinDato = esUltimoMes ? (SIN_ARCHIVO[metrica] ?? []) : []
  const items = ASESORES.filter(({ legajo }) => !sinDato.includes(legajo)).map(({ legajo }) => ({
    legajo,
    metrica,
    valor: valorDe(legajo, metrica, periodo, semana),
  }))

  return { periodo, fechaDeCorte: corte, origenImportacion: null, mediciones: items }
}

/* -- los incentivos --------------------------------------------------------- */

/**
 * Lo que cobra cada asesor en el mes.
 *
 * **No se calcula a partir del multimetrico**, y eso es el punto: los importes
 * llegan resueltos desde la empresa y el sistema los adjunta tal cual. Aca se
 * inventan con el mismo pseudoazar sembrado que el resto.
 *
 * Se los hace *correlacionar* con el desempeno -quien bonifica mas cobra algo
 * mas- porque una demo donde el mejor del equipo cobra el minimo se lee como un
 * error de la aplicacion. Es realismo del dato de ejemplo, no una regla: si
 * manana la empresa manda cualquier otro numero, el sistema lo muestra igual.
 */
function importeDe(legajo, periodo) {
  const base = 110000 + sembrado(`incentivo:${legajo}`) * 60000
  const delMes = (sembrado(`incentivo:${legajo}:${periodo}`) - 0.5) * 24000

  // El TMO sembrado sirve de proxy del desempeno: mas bajo, mejor.
  const [min, max] = RANGOS.TMO
  const desempeno = 1 - (Number(valorDe(legajo, 'TMO', periodo, SEMANAS - 1)) - min) / (max - min)

  return Math.round((base + delMes + desempeno * 30000) / 50) * 50
}

/**
 * Quienes no vienen en la planilla de incentivos del ultimo mes.
 *
 * Es el caso que la pantalla existe para mostrar: aparecen en la lista con el
 * importe vacio en vez de desaparecer, y no cuentan como cero en el total.
 */
const SIN_INCENTIVO = ['10495', '10502']

function planillaDeIncentivos(periodo, esUltimoMes) {
  const afuera = esUltimoMes ? SIN_INCENTIVO : []
  const filas = [['ASESOR', 'IMPORTE']]

  for (const { legajo, nombre } of ASESORES) {
    if (afuera.includes(legajo)) continue
    // El archivo trae el NOMBRE y no el legajo, como los de verdad: es lo que
    // obliga a que el sistema resuelva nombres contra el padron.
    filas.push([nombre, importeDe(legajo, periodo)])
  }

  return { archivo: libroDeUnaHoja('Datos', filas), cargados: filas.length - 1 }
}

/* -- el envio --------------------------------------------------------------- */

let fallas = 0

async function postear(ruta, cuerpo, que) {
  let respuesta
  try {
    respuesta = await fetch(`${API}${ruta}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(cuerpo),
    })
  } catch {
    console.error(`  FALLO   ${que}: no se pudo contactar la API`)
    fallas += 1
    return
  }

  // El 409 no es un fallo del script: es que el dato ya estaba. Correrlo dos
  // veces tiene que ser inofensivo, porque la forma normal de usarlo es
  // correrlo cuando no se sabe si la base esta cargada.
  if (respuesta.ok || respuesta.status === 409) return

  console.error(`  FALLO   ${que} (HTTP ${respuesta.status})`)
  console.error(`  ${(await respuesta.text()).slice(0, 300)}`)
  fallas += 1
}

/**
 * Entra como administrador y guarda el token.
 *
 * <p>Falla ruidosamente y corta. Un seed que sigue sin token daria una cascada de
 * 401 que hay que leer entera para entender que el problema era uno solo.
 */
async function entrar() {
  let respuesta
  try {
    respuesta = await fetch(`${API}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ legajo: ADMIN_LEGAJO, password: ADMIN_PASSWORD }),
    })
  } catch {
    console.error('No se pudo contactar la API para entrar.')
    process.exit(1)
  }

  if (!respuesta.ok) {
    console.error(`No se pudo entrar como '${ADMIN_LEGAJO}' (HTTP ${respuesta.status}).`)
    console.error('')
    console.error('Si la base es nueva, el administrador se crea al arrancar la API y')
    console.error('necesita que ADMIN_PASSWORD este definida en su entorno. Si ya existe,')
    console.error('pasa las credenciales al script:')
    console.error('')
    console.error('  ADMIN_LEGAJO=admin ADMIN_PASSWORD=... node scripts/datos-de-ejemplo.mjs')
    process.exit(1)
  }

  const cuerpo = await respuesta.json()
  token = cuerpo.token
  console.log(`Entrando como ${cuerpo.legajo} (${cuerpo.rol})\n`)
}

/**
 * Sube la planilla de incentivos del mes.
 *
 * Va aparte de `postear` porque es multipart y no JSON. Sin Content-Type a mano:
 * el runtime tiene que poner el suyo con el boundary, y fijarlo desde aca deja al
 * servidor sin poder parsear el cuerpo.
 */
async function subirIncentivos(periodo, esUltimoMes) {
  const { archivo, cargados } = planillaDeIncentivos(periodo, esUltimoMes)

  const cuerpo = new FormData()
  cuerpo.append('archivo', new Blob([archivo]), `incentivos-${periodo}.xlsx`)
  cuerpo.append('hoja', 'Datos')
  cuerpo.append('columnaAsesor', 'ASESOR')
  cuerpo.append('columnaImporte', 'IMPORTE')

  let respuesta
  try {
    respuesta = await fetch(`${API}/api/incentivos/${periodo}`, {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: cuerpo,
    })
  } catch {
    console.error(`  FALLO   ${periodo} incentivos: no se pudo contactar la API`)
    fallas += 1
    return 0
  }

  if (!respuesta.ok) {
    console.error(`  FALLO   ${periodo} incentivos (HTTP ${respuesta.status})`)
    console.error(`  ${(await respuesta.text()).slice(0, 300)}`)
    fallas += 1
    return 0
  }

  const informe = await respuesta.json()
  if (informe.rechazadas.length > 0) {
    // No es un fallo del script, pero si lo fuera del generador conviene verlo:
    // una fila rechazada aca significa que un nombre dejo de resolver.
    console.error(`  OJO     ${periodo}: ${informe.rechazadas.length} fila(s) sin cargar`)
    informe.rechazadas.forEach((f) => console.error(`          fila ${f.numeroDeFila}: ${f.motivo}`))
  }
  return cargados
}

async function main() {
  const lista = [...meses(DESDE, HASTA)]
  if (lista.length === 0) {
    console.error(`Rango vacio: ${DESDE} .. ${HASTA}`)
    process.exit(1)
  }

  console.log(`API      ${API}`)
  console.log(`Campania ${CAMPANIA}`)
  console.log(`Meses    ${lista[0]} .. ${lista[lista.length - 1]}  (${lista.length})\n`)

  try {
    await fetch(`${API}/api/pliegos`)
  } catch {
    console.error(`No se pudo contactar la API en ${API}.`)
    console.error('Levantala con:  docker compose up -d  &&  ./mvnw spring-boot:run')
    process.exit(1)
  }

  await entrar()

  for (const periodo of lista) {
    const esUltimoMes = periodo === lista[lista.length - 1]

    await postear('/api/pliegos', { periodo, campania: CAMPANIA, escalas: ESCALAS }, `${periodo} pliego`)
    await postear('/api/padrones', { periodo, asesores: ASESORES }, `${periodo} padron`)

    // Un lote por metrica y por semana, no uno solo con todo adentro.
    //
    // Cada carga semanal es una medicion nueva y no un UPDATE: eso es lo que
    // deja reconstruir por que cambio un numero dentro del mes, que es de las
    // pocas cosas que la planilla no puede contestar. La vigente es la del
    // ultimo corte, asi que la tabla del equipo muestra la semana de cierre.
    const resumen = []
    for (const metrica of METRICAS) {
      const cortes = cortesDe(metrica, periodo)
      for (let semana = 0; semana < SEMANAS; semana++) {
        const lote = mediciones(periodo, metrica, semana, cortes[semana], esUltimoMes)
        await postear('/api/mediciones', lote, `${periodo} ${metrica} s${semana + 1}`)
      }
      resumen.push(`${metrica.slice(0, 3)} al ${cortes[SEMANAS - 1].slice(8)}`)
    }

    const conIncentivo = await subirIncentivos(periodo, esUltimoMes)

    console.log(
      `  ${periodo}  ${ASESORES.length} asesores  ·  ${SEMANAS} cargas  ·  ` +
        `${conIncentivo} incentivos  ·  ${resumen.join('  ')}`,
    )
  }

  if (fallas > 0) {
    console.error(`\nTerminado con ${fallas} falla(s).`)
    process.exit(1)
  }

  console.log(`\nListo. ${lista.length} mes(es) cargados en la campania ${CAMPANIA}.`)
  console.log(`Abri la pantalla con el periodo ${lista[lista.length - 1]}.`)
}

main()
