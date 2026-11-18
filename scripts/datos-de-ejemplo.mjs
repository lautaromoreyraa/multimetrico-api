
import { libroDeUnaHoja } from './xlsx.mjs'

const API = process.env.MULTIMETRICO_API ?? 'http://localhost:8082'
const CAMPANIA = process.env.MULTIMETRICO_CAMPANIA ?? 'CONVERGENTE'

const ADMIN_LEGAJO = process.env.ADMIN_LEGAJO ?? 'admin'
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD ?? 'admin'

let token = null


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

const ASESORES = NOMBRES.map(([legajo, nombre], i) => ({
  legajo,
  nombre,
  lider: i < 8 ? LIDERES[0] : LIDERES[1],
}))


function sembrado(semilla) {
  let h = 2166136261
  for (const c of semilla) {
    h ^= c.charCodeAt(0)
    h = Math.imul(h, 16777619)
  }
  return ((h >>> 0) % 10000) / 10000
}

const RANGOS = {
  TMO: [598, 712, 0],
  NPS: [41, 68, 1],
  FCR: [74, 84, 1],
  ACTIVIDAD_COMERCIAL: [55, 96, 1],
  BAJA_CONVERGENTE: [0.4, 3.4, 1],
}

const SEMANAS = 4

function valorDeCierre(legajo, metrica, periodo) {
  const [min, max] = RANGOS[metrica]
  const base = sembrado(`${legajo}:${metrica}`)
  const delMes = sembrado(`${legajo}:${metrica}:${periodo}`)
  return min + ((base * 2 + delMes) / 3) * (max - min)
}

function valorDe(legajo, metrica, periodo, semana) {
  const [min, max, decimales] = RANGOS[metrica]
  const cierre = valorDeCierre(legajo, metrica, periodo)
  if (semana === SEMANAS - 1) return cierre.toFixed(decimales)

  const desvio = sembrado(`${legajo}:${metrica}:${periodo}:s${semana}`) - 0.5
  const cuanto = (SEMANAS - 1 - semana) / (SEMANAS - 1)
  const crudo = cierre + desvio * cuanto * (max - min) * 0.22
  return Math.min(Math.max(crudo, min), max).toFixed(decimales)
}

function cortesDe(metrica, periodo) {
  const primero = 1 + Math.floor(sembrado(`corte:${metrica}:${periodo}`) * 7)
  return Array.from({ length: SEMANAS }, (_, semana) => {
    const dia = primero + semana * 7
    return `${periodo}-${String(dia).padStart(2, '0')}`
  })
}

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


function importeDe(legajo, periodo) {
  const base = 110000 + sembrado(`incentivo:${legajo}`) * 60000
  const delMes = (sembrado(`incentivo:${legajo}:${periodo}`) - 0.5) * 24000

  const [min, max] = RANGOS.TMO
  const desempeno = 1 - (Number(valorDe(legajo, 'TMO', periodo, SEMANAS - 1)) - min) / (max - min)

  return Math.round((base + delMes + desempeno * 30000) / 50) * 50
}

const SIN_INCENTIVO = ['10495', '10502']

function planillaDeIncentivos(periodo, esUltimoMes) {
  const afuera = esUltimoMes ? SIN_INCENTIVO : []
  const filas = [['ASESOR', 'IMPORTE']]

  for (const { legajo, nombre } of ASESORES) {
    if (afuera.includes(legajo)) continue
    filas.push([nombre, importeDe(legajo, periodo)])
  }

  return { archivo: libroDeUnaHoja('Datos', filas), cargados: filas.length - 1 }
}


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

  if (respuesta.ok || respuesta.status === 409) return

  console.error(`  FALLO   ${que} (HTTP ${respuesta.status})`)
  console.error(`  ${(await respuesta.text()).slice(0, 300)}`)
  fallas += 1
}

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
