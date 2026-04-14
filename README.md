# MeerKatters

Plataforma web para crear, gestionar y dinamizar comunidades de estudio universitarias. El proyecto combina una SPA en React con una API REST en Spring Boot para cubrir colaboración académica, organización de eventos, contratación de tutores, pagos e integraciones con servicios externos.

## Descripción y propósito

MeerKatters está pensado para estudiantes que necesitan coordinarse alrededor de asignaturas, grupos de estudio y recursos compartidos, y para tutores que ofrecen apoyo académico dentro de la misma plataforma. Además del núcleo social y colaborativo, la aplicación incorpora pagos, notificaciones, autenticación con Google, videollamadas y gestión de ubicaciones.

## Caso de uso principal

Un estudiante se registra, explora comunidades públicas o crea una nueva, se une a otros usuarios de su universidad, organiza eventos de estudio presenciales o virtuales, comparte recursos y conversa en tiempo real. Si necesita refuerzo adicional, puede consultar tutores verificados, contratarlos y gestionar pagos desde la propia aplicación.

## Características principales

- Registro, login, verificación por email y recuperación de contraseña.
- Inicio de sesión con Google y soporte de autenticación en dos factores (2FA/TOTP).
- Creación y gestión de comunidades públicas o privadas.
- Gestión de miembros, solicitudes de acceso, roles y transferencia de administración.
- Publicación de anuncios, comentarios y organización de contenido por categorías.
- Creación de eventos de estudio, asistencia, borradores, mapa de eventos y ubicaciones recomendadas.
- Chat privado y de comunidad en tiempo real.
- Sistema de notificaciones y recordatorios programados.
- Directorio de tutores, verificación de perfiles, disponibilidad y contratación.
- Suscripciones y pagos con Stripe para planes, verificación de tutor y contrataciones.
- Integración con Google Calendar y Google Classroom.
- Integración con Zoom para reuniones, participantes y grabaciones.
- Módulos de cuestionarios, recomendaciones y valoraciones.

## Tecnologías utilizadas

### Backend

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Security con JWT
- Spring Data JPA
- PostgreSQL
- H2 para ciertos escenarios locales o de prueba
- WebSocket
- Maven
- SpringDoc / Swagger UI
- Stripe Java SDK
- Google API Client
- SendGrid / SMTP
- Jsoup

### Frontend

- React 19
- React Router 6
- Axios
- STOMP / SockJS / Socket.IO Client
- Stripe Elements
- React Leaflet / Leaflet
- Recharts
- Create React App
- Jest y Testing Library

### Infraestructura y soporte

- Docker Compose
- Git hooks
- Checkstyle
- Spotless
- JaCoCo

## Requisitos previos

- Java 21
- Node.js 18 o superior
- npm
- Maven Wrapper incluido en `backend`
- PostgreSQL 16 o compatible
- Variables de entorno configuradas a partir de `.env.example` y `frontend/.env.example`

## Instalación

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd ISPP
```

### 2. Activar los git hooks

```bash
git config core.hooksPath .githooks
```

### 3. Configurar variables de entorno

```bash
cp .env.example .env
cp frontend/.env.example frontend/.env
```

Después ajusta los valores según tu entorno local. En especial:

- Base de datos PostgreSQL
- JWT
- SMTP o SendGrid
- Stripe
- Google Classroom / Calendar
- Zoom
- URL del frontend

### 4. Levantar la base de datos

Opción recomendada con Docker:

```bash
docker compose up -d postgres
```

### 5. Instalar dependencias del frontend

```bash
cd frontend
npm install
cd ..
```

## Cómo ejecutar el proyecto

### Opción A: ejecución local por separado

Backend:

```bash
cd backend
./mvnw spring-boot:run
```

En Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Frontend:

```bash
cd frontend
npm start
```

URLs por defecto:

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Opción B: backend y base de datos con Docker Compose

```bash
docker compose up --build
```

Esta configuración levanta:

- PostgreSQL en `localhost:5432`
- Backend en `localhost:8080`

El frontend se ejecuta aparte desde `frontend/`.

## Ejemplos de uso

### Explorar comunidades públicas

1. Accede a `http://localhost:3000/comunidades`
2. Filtra o busca por nombre
3. Entra al detalle de una comunidad

### Crear una comunidad

1. Regístrate e inicia sesión
2. Ve a `Crear comunidad`
3. Define nombre, descripción, tipo de grupo y límites
4. El creador queda como administrador automáticamente

### Contratar un tutor

1. Navega al listado de profesores verificados
2. Abre un perfil de tutor
3. Revisa disponibilidad
4. Inicia el flujo de pago con Stripe

### Crear un evento de estudio

1. Accede a una comunidad en la que tengas permisos
2. Crea un evento presencial o virtual
3. Decide si aparece en el mapa
4. Gestiona asistentes y recordatorios

## Estructura del proyecto

```text
ISPP/
├── backend/                  # API REST, seguridad, lógica de negocio y acceso a datos
│   ├── src/main/java/es/us/meerkat/backend/
│   │   ├── config/           # Configuración de seguridad, OpenAPI, websockets, seeds
│   │   ├── controller/       # Endpoints REST por dominio
│   │   ├── dto/              # DTOs de entrada y salida
│   │   ├── entity/           # Entidades JPA por módulo
│   │   ├── repository/       # Repositorios Spring Data
│   │   ├── scheduler/        # Tareas programadas y recordatorios
│   │   ├── security/         # JWT y filtros de autenticación
│   │   └── service/          # Casos de uso y reglas de negocio
│   └── src/main/resources/   # application*.yml, SQL y recursos
├── frontend/                 # SPA React
│   ├── src/api/              # Cliente HTTP y wrappers por dominio
│   ├── src/components/       # Componentes reutilizables
│   ├── src/contexts/         # Estado global de auth, sockets y notificaciones
│   ├── src/hooks/            # Hooks personalizados
│   ├── src/screens/          # Pantallas por funcionalidad
│   └── src/utils/            # Utilidades compartidas
├── docs/                     # Documentación funcional y técnica
├── config/                   # Configuración compartida, por ejemplo Checkstyle
├── docker-compose.yaml       # PostgreSQL + backend
└── *.ps1                     # Scripts auxiliares de desarrollo y pruebas
```

## Variables de entorno

### Backend

Variables relevantes detectadas en `application.yaml`, `application.properties` y `.env.example`:

```env
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=

LOCAL_DATASOURCE_URL=
LOCAL_DATASOURCE_USERNAME=
LOCAL_DATASOURCE_PASSWORD=

JWT_SECRET=
JWT_EXPIRATION=

FRONTEND_URL=

MAIL_HOST=
MAIL_PORT=
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=
SENDGRID_API_KEY=

STRIPE_API_KEY=
STRIPE_WEBHOOK_SECRET=
STRIPE_SUCCESS_URL=
STRIPE_CANCEL_URL=
STRIPE_PRICE_PREMIUM=

GOOGLE_CLASSROOM_CLIENT_ID=
GOOGLE_CLASSROOM_CLIENT_SECRET=
GOOGLE_CLASSROOM_REDIRECT_URI=
GOOGLE_CALENDAR_CLIENT_ID=
GOOGLE_CALENDAR_CLIENT_SECRET=
GOOGLE_CALENDAR_REDIRECT_URI=

ZOOM_CLIENT_ID=
ZOOM_CLIENT_SECRET=
ZOOM_ACCOUNT_ID=
ZOOM_WEBHOOK_SECRET_TOKEN=
ZOOM_API_BASE_URL=
ZOOM_RECORDINGS_STORAGE_PATH=
ZOOM_RECORDINGS_SUPABASE_URL=
ZOOM_RECORDINGS_SUPABASE_SERVICE_ROLE_KEY=
ZOOM_RECORDINGS_SUPABASE_BUCKET=
```

### Frontend

```env
REACT_APP_API_URL=http://localhost:8080
REACT_APP_STRIPE_PUBLIC_KEY=
```

## Testing y calidad

### Backend

```bash
cd backend
./mvnw test
./mvnw verify
./mvnw checkstyle:check
./mvnw spotless:check
```

### Frontend

```bash
cd frontend
npm test
npm run lint
```

El repositorio contiene una batería amplia de tests unitarios e integrados tanto en frontend como en backend, además de configuración de cobertura con JaCoCo.

## Buenas prácticas y notas importantes

- Usa `.env.example` como plantilla; no reutilices secretos reales ni credenciales de prueba comprometidas.
- El backend funciona con JWT y política stateless; la sesión se mantiene en el cliente.
- Muchas acciones dependen del contexto del recurso, no de un rol global único. Por ejemplo, un usuario puede ser admin de una comunidad y miembro de otra.
- Algunas integraciones externas son opcionales en local, pero ciertas funcionalidades quedan degradadas sin ellas: Stripe, Google, Zoom, correo y almacenamiento de grabaciones.
- La app expone Swagger UI y una especificación OpenAPI para revisar contratos y probar endpoints.
- El proyecto incluye scripts PowerShell para tareas concretas de soporte y verificación.
- Existe documentación histórica en `docs/` que conviene revisar si se necesita contexto funcional adicional del curso o de sprints anteriores.

## Suposiciones explícitas

- Se asume que el caso de uso principal es universitario, porque el código y la documentación hacen referencia constante a universidades, asignaturas y la Universidad de Sevilla.
- Se asume que la interfaz principal es web, aunque la estructura original del README mencionaba “móvil y web”; en el repositorio actual el cliente visible es una SPA React.
- Se asume que el plan individual principal es `PREMIUM` y que existen variantes para tutores e instituciones, ya que el código contiene flujos separados para suscripciones, verificación docente y planes institucionales.

## Documentación adicional

- Documentación general: [`docs/overview.md`](docs/overview.md)
- API y OpenAPI: `docs/api/`
- Diagramas y artefactos del proyecto: `docs/diagramas/`
