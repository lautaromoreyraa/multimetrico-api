# Deploy — Railway (API + MySQL) y Vercel (pantalla)

El orden importa: **primero el backend**, porque el frontend necesita su URL.

Todo lo que sigue lo tenés que correr vos: `railway login` y `vercel login` abren
el navegador y la sesión es tuya.

---

## Antes de empezar: qué vas a publicar

**La aplicación no tiene autenticación.** Está anotado en `HANDOFF.md` como lo
único que falta del alcance. Desplegarla con una URL pública significa que
cualquiera que tenga el link ve todas las filas, cambia de equipo y carga
pliegos.

Los datos son sintéticos —el seed no toca ninguna planilla real— así que no hay
un problema de privacidad. Pero es una demo abierta, no un sistema en producción,
y conviene tenerlo claro antes de compartir el link.

---

## 1. Backend en Railway

```bash
cd multimetrico-api
railway login          # abre el navegador
railway init           # crea el proyecto; elegí un nombre
```

### 1.1 La base

Agregá el MySQL desde el panel de Railway (**New → Database → MySQL**) o:

```bash
railway add --database mysql
```

### 1.2 Enlazar la base con la app

El servicio de la API necesita ver las variables del MySQL. En el panel, en
**Variables** del servicio de la API, agregá estas cinco **como referencias**:

| Variable | Valor |
|---|---|
| `MYSQLHOST` | `${{MySQL.MYSQLHOST}}` |
| `MYSQLPORT` | `${{MySQL.MYSQLPORT}}` |
| `MYSQLDATABASE` | `${{MySQL.MYSQLDATABASE}}` |
| `MYSQLUSER` | `${{MySQL.MYSQLUSER}}` |
| `MYSQLPASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |

`application.yml` los lee con esos nombres exactos, así que no hay que traducir
nada. Si tu servicio de base se llama distinto de `MySQL`, cambiá ese prefijo.

> Si preferís una sola variable, `MULTIMETRICO_DB_URL` pisa a las cinco. Tiene
> que ser una URL **JDBC** (`jdbc:mysql://host:puerto/base`), no la `mysql://`
> que Railway expone en `MYSQL_URL`.

### 1.3 Desplegar

```bash
railway up
```

Usa el `Dockerfile` del repo. Es a propósito: la autodetección elige la versión
de Java por su cuenta y este proyecto necesita **21**.

El primer arranque corre las migraciones de Flyway y crea el esquema. Mirá los
logs con `railway logs`; tiene que aparecer *"Successfully applied 1 migration"* y
después *"Started MultimetricoApplication"*.

### 1.4 La URL pública

```bash
railway domain          # genera y muestra el dominio
```

Guardala: es lo que va en el paso 3. Se ve como
`https://algo-production.up.railway.app`.

Comprobá que responde:

```bash
curl https://TU-URL.up.railway.app/api/pliegos
# [] con la base vacía. Un 200 con lista vacía es lo correcto acá.
```

### 1.5 Cargar los datos

La base arranca vacía y **sin datos la primera pantalla responde 404**:

```bash
MULTIMETRICO_API=https://TU-URL.up.railway.app node scripts/datos-de-ejemplo.mjs
```

Esto sí lo podés correr desde tu máquina: el script habla con la API por HTTP.

---

## 2. Frontend en Vercel

### 2.1 Poner la URL del backend

Editá `vercel.json` en `multimetrico-frontend` y reemplazá el marcador por la URL
del paso 1.4:

```json
"destination": "https://TU-URL.up.railway.app/api/:ruta*"
```

**Por qué un rewrite y no una variable de entorno.** Con el rewrite, el navegador
le pide todo a Vercel y Vercel reenvía a Railway: para la pantalla sigue siendo
el mismo origen. Eso evita configurar CORS —superficie que esta aplicación, que
no tiene autenticación, no necesita abrir— y además la URL del backend no queda
escrita en el bundle. El costo es esta línea a mano.

### 2.2 Desplegar

```bash
cd multimetrico-frontend
npm i -g vercel        # no está instalado
vercel login
vercel --prod
```

Vercel detecta Vite solo, pero `vercel.json` fija el build y el directorio de
salida igual, para que no dependa de la detección.

**No hace falta un catch-all de SPA.** La aplicación rutea por hash (`#/equipo`),
así que el servidor sólo sirve `index.html` y nunca ve las rutas. Está explicado
en `src/lib/router.ts`, y es justamente el caso en que esa decisión paga.

---

## 3. Comprobar que quedó andando

1. Abrí la URL de Vercel. Tiene que salir **Multimétrico** con quince asesores.
2. Si ves el cartel de *"No se pudo contactar a la API"*, el rewrite apunta mal:
   revisá `vercel.json` y volvé a desplegar.
3. Si ves un **404 de pliego**, la API responde pero la base está vacía: falta el
   paso 1.5.
4. Si la pantalla abre en un mes o un equipo raro, es el `localStorage` de tu
   navegador (`multimetrico.contexto`), no el deploy.

---

## Lo que este deploy no resuelve

- **Sin autenticación.** Ver arriba.
- **El plan gratuito de Railway duerme o corta el servicio** según el uso. La
  primera carga después de un rato puede tardar; no es un error de la aplicación.
- **La base de Railway es otra base.** Lo que cargues en local no viaja, y al
  revés. El seed hay que correrlo una vez contra cada una.
- **No hay pipeline.** Cada deploy es un `railway up` y un `vercel --prod` a mano.
