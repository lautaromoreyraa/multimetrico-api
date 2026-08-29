# Deploy — Railway (API + MySQL) y Vercel (pantalla)

El orden importa: **primero el backend**, porque el frontend necesita su URL.

## Estado actual

**El backend ya está desplegado y con datos.**

- Proyecto Railway: `multimetrico` (workspace `lautaromoreyraa's Projects`)
- Servicios: `multimetrico-api` y `MySQL`
- URL: <https://multimetrico-api-production.up.railway.app>
- Las tres migraciones de Flyway se aplicaron solas al arrancar
- Cargado con el seed: 8 meses de 2026, campaña `CONVERGENTE`

Falta el frontend en Vercel, que necesita `vercel login` — eso sale de tu lado.

> **Cuidado al correr comandos de Railway en este directorio.** Antes de crear el
> proyecto, la carpeta estaba vinculada a otro proyecto tuyo (`oriente`). Un
> `railway up` en esa situación habría desplegado el multimétrico **encima de
> ese proyecto**. Verificá siempre con `railway status` antes de desplegar.

---

---

## Antes de empezar: qué vas a publicar

**El multimétrico se ve sin entrar.** Es a propósito: la pantalla tiene que poder
mostrarse sin pedirle nada a nadie. Lo que pide sesión es el panel de
administración, o sea todo lo que carga datos.

Los datos son sintéticos —el seed no toca ninguna planilla real—, así que aun con
la URL pública no hay datos de nadie ahí adentro. Si alguna vez entran números
reales, esto hay que volver a cerrarlo; está anotado en `HANDOFF.md`.

---

## 1. Backend en Railway

Esto ya se hizo; queda como referencia para rehacerlo desde cero.

```bash
cd multimetrico-api
railway login                                    # abre el navegador
railway status                                   # ¿a qué proyecto apunta la carpeta?
railway unlink -y                                # si apunta a otro proyecto
railway init --name multimetrico --workspace "tu workspace"
```

### 1.1 La base

```bash
railway add --database mysql
```

### 1.2 Enlazar la base con la app

El servicio de la API se crea con sus variables de una:

```bash
railway add --service multimetrico-api \
  -v "JWT_SECRETO=$(openssl rand -base64 48)" \
  -v "ADMIN_PASSWORD=la-que-elijas" \
  -v 'MYSQLHOST=${{MySQL.MYSQLHOST}}' \
  -v 'MYSQLPORT=${{MySQL.MYSQLPORT}}' \
  -v 'MYSQLDATABASE=${{MySQL.MYSQLDATABASE}}' \
  -v 'MYSQLUSER=${{MySQL.MYSQLUSER}}' \
  -v 'MYSQLPASSWORD=${{MySQL.MYSQLPASSWORD}}'
```

Las cinco del MySQL van **como referencias** —con `${{MySQL.…}}`— y en comillas
simples, para que las expanda Railway y no el shell. Si el servicio de base se
llama distinto, cambiá ese prefijo. En el panel se agregan igual, en
**Variables** del servicio de la API:

| Variable | Valor |
|---|---|
| `MYSQLHOST` | `${{MySQL.MYSQLHOST}}` |
| `MYSQLPORT` | `${{MySQL.MYSQLPORT}}` |
| `MYSQLDATABASE` | `${{MySQL.MYSQLDATABASE}}` |
| `MYSQLUSER` | `${{MySQL.MYSQLUSER}}` |
| `MYSQLPASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |

Y estas dos, que **no son referencias sino valores tuyos**:

| Variable | Qué poner |
|---|---|
| `JWT_SECRETO` | Una cadena de 32 caracteres o más. Generala: `openssl rand -base64 48` |
| `ADMIN_PASSWORD` | La contraseña del primer administrador |

**Sin `JWT_SECRETO` la aplicación no arranca**, a propósito: un secreto con valor
por defecto termina siendo el mismo en todas las instalaciones y está publicado en
el repositorio. El error del arranque dice exactamente esto.

`ADMIN_PASSWORD` sólo se usa la primera vez, cuando la tabla de usuarios está
vacía. Sin ella la aplicación arranca igual pero **nadie puede entrar**, y lo avisa
en el log con un recuadro difícil de no ver.

`application.properties` las lee con esos nombres exactos, así que no hay que
traducir nada. Si tu servicio de base se llama distinto de `MySQL`, cambiá ese
prefijo.

> Si preferís una sola variable, `MULTIMETRICO_DB_URL` pisa a las cinco. Tiene
> que ser una URL **JDBC** (`jdbc:mysql://host:puerto/base`), no la `mysql://`
> que Railway expone en `MYSQL_URL`.

### 1.3 Desplegar

```bash
railway service multimetrico-api   # que el CLI apunte al servicio y no a la base
railway up --detach
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

Comprobá que responde abriendo `/api/pliegos` en el navegador: con la base recién
creada devuelve `[]`, y **un 200 con lista vacía es lo correcto acá** — quiere
decir que la API está viva y la base todavía no tiene nada.

Los logs se miran con `railway logs` (el runtime) y `railway logs --build` (la
compilación). Tiene que aparecer *"Successfully applied 3 migrations"* y después
*"Started MultimetricoApplication"*.

### 1.5 Cargar los datos

La base arranca vacía y **sin datos la primera pantalla responde 404**:

```bash
MULTIMETRICO_API=https://TU-URL.up.railway.app \
ADMIN_PASSWORD=la-que-pusiste-en-railway \
node scripts/datos-de-ejemplo.mjs
```

Esto sí lo podés correr desde tu máquina: el script habla con la API por HTTP.
Entra como administrador antes de cargar nada, porque cargar el pliego, el padrón
y las mediciones es de `ADMIN`.

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
el mismo origen. Eso evita configurar CORS —superficie que no hace falta abrir si
la arquitectura ya la evita— y además la URL del backend no queda escrita en el
bundle. El costo es esta línea a mano.

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

1. Abrí la URL de Vercel. Tiene que salir **Multimétrico** con quince asesores,
   sin pedir nada. En **Administración** tiene que aparecer el login: entrá con
   el legajo `admin` y la `ADMIN_PASSWORD` que pusiste en Railway.
2. Si ves el cartel de *"No se pudo contactar a la API"*, el rewrite apunta mal:
   revisá `vercel.json` y volvé a desplegar.
3. Si ves un **404 de pliego**, la API responde pero la base está vacía: falta el
   paso 1.5.
4. Si la pantalla abre en un mes o un equipo raro, es el `localStorage` de tu
   navegador (`multimetrico.contexto`), no el deploy.

---

## Lo que este deploy no resuelve

- **Los usuarios se crean por API**, con `POST /api/usuarios` desde una cuenta de
  administrador. No hay pantalla para eso.
- **El plan gratuito de Railway duerme o corta el servicio** según el uso. La
  primera carga después de un rato puede tardar; no es un error de la aplicación.
- **La base de Railway es otra base.** Lo que cargues en local no viaja, y al
  revés. El seed hay que correrlo una vez contra cada una.
- **No hay pipeline.** Cada deploy es un `railway up` y un `vercel --prod` a mano.
