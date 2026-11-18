
import { deflateRawSync } from 'node:zlib'


const TABLA_CRC = (() => {
  const tabla = new Int32Array(256)
  for (let i = 0; i < 256; i++) {
    let c = i
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1
    tabla[i] = c
  }
  return tabla
})()

function crc32(buffer) {
  let c = -1
  for (let i = 0; i < buffer.length; i++) c = TABLA_CRC[(c ^ buffer[i]) & 0xff] ^ (c >>> 8)
  return (c ^ -1) >>> 0
}


function zip(entradas) {
  const locales = []
  const centrales = []
  let offset = 0

  for (const { nombre, contenido } of entradas) {
    const datos = Buffer.from(contenido, 'utf8')
    const comprimido = deflateRawSync(datos)
    const nombreBytes = Buffer.from(nombre, 'utf8')
    const crc = crc32(datos)

    const local = Buffer.alloc(30)
    local.writeUInt32LE(0x04034b50, 0)
    local.writeUInt16LE(20, 4)
    local.writeUInt16LE(0, 6)
    local.writeUInt16LE(8, 8)
    local.writeUInt16LE(0, 10)
    local.writeUInt16LE(0, 12)
    local.writeUInt32LE(crc, 14)
    local.writeUInt32LE(comprimido.length, 18)
    local.writeUInt32LE(datos.length, 22)
    local.writeUInt16LE(nombreBytes.length, 26)
    local.writeUInt16LE(0, 28)

    locales.push(local, nombreBytes, comprimido)

    const central = Buffer.alloc(46)
    central.writeUInt32LE(0x02014b50, 0)
    central.writeUInt16LE(20, 4)
    central.writeUInt16LE(20, 6)
    central.writeUInt16LE(0, 8)
    central.writeUInt16LE(8, 10)
    central.writeUInt16LE(0, 12)
    central.writeUInt16LE(0, 14)
    central.writeUInt32LE(crc, 16)
    central.writeUInt32LE(comprimido.length, 20)
    central.writeUInt32LE(datos.length, 24)
    central.writeUInt16LE(nombreBytes.length, 28)
    central.writeUInt16LE(0, 30)
    central.writeUInt16LE(0, 32)
    central.writeUInt16LE(0, 34)
    central.writeUInt16LE(0, 36)
    central.writeUInt32LE(0, 38)
    central.writeUInt32LE(offset, 42)

    centrales.push(central, nombreBytes)
    offset += local.length + nombreBytes.length + comprimido.length
  }

  const cuerpo = Buffer.concat(locales)
  const directorio = Buffer.concat(centrales)

  const fin = Buffer.alloc(22)
  fin.writeUInt32LE(0x06054b50, 0)
  fin.writeUInt16LE(0, 4)
  fin.writeUInt16LE(0, 6)
  fin.writeUInt16LE(entradas.length, 8)
  fin.writeUInt16LE(entradas.length, 10)
  fin.writeUInt32LE(directorio.length, 12)
  fin.writeUInt32LE(cuerpo.length, 16)
  fin.writeUInt16LE(0, 20)

  return Buffer.concat([cuerpo, directorio, fin])
}


function columna(indice) {
  let nombre = ''
  let n = indice
  do {
    nombre = String.fromCharCode(65 + (n % 26)) + nombre
    n = Math.floor(n / 26) - 1
  } while (n >= 0)
  return nombre
}

function escapar(texto) {
  return String(texto)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function celda(referencia, valor) {
  if (typeof valor === 'number') {
    return `<c r="${referencia}"><v>${valor}</v></c>`
  }
  return `<c r="${referencia}" t="inlineStr"><is><t>${escapar(valor)}</t></is></c>`
}

export function libroDeUnaHoja(hoja, filas) {
  const xmlFilas = filas
    .map((fila, i) => {
      const celdas = fila
        .map((valor, j) => (valor === null || valor === undefined ? '' : celda(`${columna(j)}${i + 1}`, valor)))
        .join('')
      return `<row r="${i + 1}">${celdas}</row>`
    })
    .join('')

  return zip([
    {
      nombre: '[Content_Types].xml',
      contenido:
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">' +
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>' +
        '<Default Extension="xml" ContentType="application/xml"/>' +
        '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>' +
        '<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>' +
        '</Types>',
    },
    {
      nombre: '_rels/.rels',
      contenido:
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
        '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>' +
        '</Relationships>',
    },
    {
      nombre: 'xl/workbook.xml',
      contenido:
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
        '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" ' +
        'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">' +
        `<sheets><sheet name="${escapar(hoja)}" sheetId="1" r:id="rId1"/></sheets>` +
        '</workbook>',
    },
    {
      nombre: 'xl/_rels/workbook.xml.rels',
      contenido:
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
        '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>' +
        '</Relationships>',
    },
    {
      nombre: 'xl/worksheets/sheet1.xml',
      contenido:
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
        '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">' +
        `<sheetData>${xmlFilas}</sheetData>` +
        '</worksheet>',
    },
  ])
}
