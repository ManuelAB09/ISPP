# Documentación API REST

## MeerKatters

### Grupo D – Turno de tarde

![Logo App](../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Documentación técnica  
**Sprint:** Sprint 1  
**Semana:** Semana 1  
**Estado:** En revisión  
**Fecha:** 19/02/2026  
**Autor(es):** Equipo de arquitectura

---

## Índice
1. [Introducción](#1-introducción)
2. [Autenticación](#2-autenticación)
3. [Modelo de Autorización por Contexto](#3-modelo-de-autorización-por-contexto)
4. [Resumen de Endpoints](#4-resumen-de-endpoints)
5. [Códigos de Estado HTTP](#5-códigos-de-estado-http)
6. [Estructura de Respuestas](#6-estructura-de-respuestas)
7. [Paginación](#7-paginación)
8. [Guía de Uso para Frontend (Mocks)](#8-guía-de-uso-para-frontend-mocks)
9. [Guía de Implementación para Backend](#9-guía-de-implementación-para-backend)

---

## 1. Introducción

Este documento describe la API REST del proyecto **MeerKatters**, una plataforma para comunidades de estudio colaborativo. La especificación completa está disponible en formato OpenAPI 3.0 en:

📁 `backend/src/main/resources/static/openapi.yaml`

### Servidores disponibles
| Entorno | URL |
|---------|-----|
| Producción | `https://api.meerkatters.com/v1` |
| Staging | `https://staging-api.meerkatters.com/v1` |
| Desarrollo | `http://localhost:8080/api/v1` |

---

## 2. Autenticación

La API utiliza **JWT (JSON Web Tokens)** para autenticación.

### Flujo de autenticación
1. El usuario se registra (`POST /auth/register`) o inicia sesión (`POST /auth/login`)
2. El servidor devuelve un `accessToken` y un `refreshToken`
3. Incluir el token en todas las peticiones autenticadas:
   ```
   Authorization: Bearer <accessToken>
   ```
4. Cuando el `accessToken` expire, usar `POST /auth/refresh` con el `refreshToken` para obtener uno nuevo

### Tokens
| Token | Duración | Uso |
|-------|----------|-----|
| `accessToken` | 1 hora | Autenticación de peticiones |
| `refreshToken` | 7 días | Renovar el accessToken |

### OAuth con Google
Para autenticación con Google (y acceso opcional a Classroom):
```json
POST /auth/google
{
  "idToken": "<token de Google>",
  "requestClassroomAccess": true
}
```

---

## 3. Modelo de Autorización por Contexto

> ⚠️ **IMPORTANTE**: Un mismo usuario puede ser tanto **estudiante** como **Tutor** de distintas comunidades. Los permisos se evalúan por **contexto específico del recurso**, NO dependen de un rol global estático.

### Roles por Contexto

| Rol | Contexto | Descripción |
|-----|----------|-------------|
| **Admin de Comunidad** | `communityId` | Usuario que creó la comunidad o recibió transferencia |
| **Miembro** | `communityId` | Usuario que pertenece a una comunidad específica |
| **Tutor contratado** | `communityId` | Tutor asociado a un grupo específico |
| **Creador de Evento** | `eventId` | Usuario que creó un evento específico |

### Ejemplos de evaluación de permisos

```
Usuario A es:
- ADMIN en Comunidad 1
- MIEMBRO en Comunidad 2
- Tutor en Comunidad 3

GET /communities/1/members    → ✅ Permitido (es admin)
PUT /communities/2            → ❌ Prohibido (es miembro, no admin)
DELETE /events/5              → Depende de si creó el evento 5 o es admin de su comunidad
```

### Endpoint para verificar el rol del usuario

```
GET /communities/{communityId}/members/me

Response:
{
  "id": 123,
  "usuario": {...},
  "rol": "ADMIN",        // o "MIEMBRO"
  "fechaIngreso": "2026-02-01T10:00:00Z"
}
```

---

## 4. Resumen de Endpoints

### 4.1 Autenticación `/auth`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/auth/register` | Registrar nuevo usuario |
| POST | `/auth/login` | Iniciar sesión |
| POST | `/auth/logout` | Cerrar sesión |
| POST | `/auth/refresh` | Refrescar token |
| POST | `/auth/password/forgot` | Solicitar recuperación de contraseña |
| POST | `/auth/password/reset` | Restablecer contraseña |
| POST | `/auth/google` | Login con Google OAuth |

### 4.2 Usuarios `/users`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/users/me` | Obtener perfil propio |
| PUT | `/users/me` | Actualizar perfil propio |
| DELETE | `/users/me` | Eliminar cuenta |
| PUT | `/users/me/password` | Cambiar contraseña |
| PUT | `/users/me/visibility` | Configurar visibilidad en listados |
| GET | `/users/{userId}` | Obtener perfil de usuario |
| GET | `/users/{userId}/communities` | Listar comunidades de un usuario |

### 4.3 Tutores `/tutors`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/tutors` | Listar tutores (filtrable) |
| POST | `/tutors` | Crear perfil de tutor |
| GET | `/tutors/me` | Obtener mi perfil de tutor |
| PUT | `/tutors/me` | Actualizar mi perfil de tutor |
| GET | `/tutors/{tutorId}` | Obtener perfil de tutor |
| POST | `/tutors/me/verification` | Solicitar verificación (pago) |
| POST | `/tutors/me/classroom` | Conectar Google Classroom |
| DELETE | `/tutors/me/classroom` | Desconectar Classroom |

### 4.4 Comunidades `/communities`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/communities` | Explorar comunidades públicas |
| POST | `/communities` | Crear comunidad |
| GET | `/communities/{id}` | Obtener detalle |
| PUT | `/communities/{id}` | Actualizar comunidad (admin) |
| DELETE | `/communities/{id}` | Eliminar comunidad (admin) |
| PUT | `/communities/{id}/privacy` | Configurar privacidad (admin) |
| POST | `/communities/{id}/upgrade` | Mejorar a Premium |
| POST | `/communities/{id}/tutor` | Contratar tutor (admin) |
| DELETE | `/communities/{id}/tutor` | Desvincular tutor (admin) |

### 4.5 Miembros `/communities/{id}/members`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/communities/{id}/members` | Listar miembros |
| POST | `/communities/{id}/members` | Unirse a comunidad pública |
| GET | `/communities/{id}/members/me` | Obtener mi membresía/rol |
| DELETE | `/communities/{id}/members/me` | Abandonar comunidad |
| DELETE | `/communities/{id}/members/{userId}` | Expulsar miembro (admin) |
| POST | `/communities/{id}/admin/transfer` | Transferir administración |

### 4.6 Solicitudes `/communities/{id}/requests`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/communities/{id}/requests` | Listar solicitudes (admin) |
| POST | `/communities/{id}/requests` | Solicitar acceso a comunidad privada |
| PUT | `/communities/{id}/requests/{requestId}` | Responder solicitud (admin) |

### 4.7 Categorías `/communities/{id}/categories`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/communities/{id}/categories` | Listar categorías |
| POST | `/communities/{id}/categories` | Crear categoría (admin) |
| PUT | `/communities/{id}/categories/{catId}` | Actualizar categoría (admin) |
| DELETE | `/communities/{id}/categories/{catId}` | Eliminar categoría (admin) |
| PUT | `/communities/{id}/categories/reorder` | Reordenar categorías (admin) |

### 4.8 Eventos `/events`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/communities/{id}/events` | Listar eventos de comunidad |
| POST | `/communities/{id}/events` | Crear evento |
| GET | `/events` | Explorar eventos públicos |
| GET | `/events/map` | Obtener eventos para mapa |
| GET | `/events/recommended-locations` | Ubicaciones recomendadas |
| GET | `/events/{id}` | Obtener detalle de evento |
| PUT | `/events/{id}` | Actualizar evento (creador/admin) |
| POST | `/events/{id}/cancel` | Cancelar evento (creador/admin) |

### 4.9 Asistencia `/events/{id}/attendance`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/events/{id}/attendance` | Listar asistentes |
| POST | `/events/{id}/attendance` | Confirmar asistencia |
| GET | `/events/{id}/attendance/me` | Obtener mi asistencia |
| DELETE | `/events/{id}/attendance/me` | Cancelar asistencia |

### 4.10 Invitaciones a Eventos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/events/{id}/invitations` | Listar invitaciones (creador/admin) |
| POST | `/events/{id}/invitations` | Enviar invitación (creador/admin) |
| PUT | `/events/{id}/invitations/{invId}` | Responder invitación |
| GET | `/users/me/invitations` | Listar mis invitaciones |

### 4.11 Archivos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/communities/{id}/files` | Listar archivos de comunidad |
| POST | `/communities/{id}/files` | Subir archivo |
| GET | `/events/{id}/files` | Listar archivos de evento |
| POST | `/events/{id}/files` | Subir archivo a evento |
| GET | `/files/{id}` | Obtener info del archivo |
| DELETE | `/files/{id}` | Eliminar archivo |
| GET | `/files/{id}/download` | Descargar archivo |

### 4.12 Suscripciones `/subscriptions`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/subscriptions/plans` | Ver planes disponibles |
| GET | `/subscriptions/me` | Obtener mi suscripción |
| POST | `/subscriptions/me` | Suscribirse a Premium |
| DELETE | `/subscriptions/me` | Cancelar suscripción |

### 4.13 Pagos `/payments`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/payments/webhook` | Webhook de Stripe |
| GET | `/payments/history` | Historial de pagos |
| GET | `/payments/{id}` | Detalle de transacción |

### 4.14 Instituciones `/institutions`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/institutions` | Registrar institución |
| GET | `/institutions/{id}` | Obtener detalle |
| PUT | `/institutions/{id}` | Actualizar institución |
| POST | `/institutions/{id}/plan` | Contratar plan corporativo |
| GET | `/institutions/{id}/invitations` | Listar invitaciones corporativas |
| POST | `/institutions/{id}/invitations` | Invitar usuarios |

### 4.15 Google Classroom `/communities/{id}/classroom`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/communities/{id}/classroom` | Info del Classroom asociado |
| DELETE | `/communities/{id}/classroom` | Desvincular Classroom |
| POST | `/communities/{id}/classroom/sync` | Sincronizar permisos |

---

## 5. Códigos de Estado HTTP

| Código | Significado | Cuándo se usa |
|--------|-------------|---------------|
| **200** | OK | Operación exitosa |
| **201** | Created | Recurso creado exitosamente |
| **204** | No Content | Operación exitosa sin contenido (DELETE) |
| **400** | Bad Request | Datos de entrada inválidos |
| **401** | Unauthorized | Token no proporcionado o expirado |
| **403** | Forbidden | Sin permisos para la acción |
| **404** | Not Found | Recurso no encontrado |
| **409** | Conflict | Conflicto (recurso ya existe) |
| **500** | Internal Error | Error del servidor |

---

## 6. Estructura de Respuestas

### Respuesta exitosa (objeto)
```json
{
  "id": 1,
  "nombre": "Grupo de Estudio",
  "descripcion": "...",
  // ... más campos
}
```

### Respuesta exitosa (lista paginada)
```json
{
  "content": [
    { /* objeto 1 */ },
    { /* objeto 2 */ }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  }
}
```

### Respuesta de error
```json
{
  "error": "VALIDATION_ERROR",
  "message": "El email ya está registrado",
  "details": [
    "email: debe ser único"
  ],
  "timestamp": "2026-02-19T10:30:00Z",
  "path": "/api/v1/auth/register"
}
```

---

## 7. Paginación

Todos los endpoints que devuelven listas soportan paginación mediante query parameters:

| Parámetro | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `page` | int | 0 | Número de página (0-indexed) |
| `size` | int | 20 | Elementos por página (máx 100) |
| `sort` | string | - | Ordenamiento: `campo,direccion` |

Ejemplo:
```
GET /communities?page=0&size=10&sort=createdAt,desc
```

---

## 8. Guía de Uso para Frontend (Mocks)

### Generar mocks desde OpenAPI

Usando herramientas como [Prism](https://stoplight.io/open-source/prism):
```bash
npx @stoplight/prism-cli mock openapi.yaml
```

O con [MSW (Mock Service Worker)](https://mswjs.io/):
```javascript
// handlers.js
import { rest } from 'msw';

export const handlers = [
  rest.get('/api/v1/communities', (req, res, ctx) => {
    return res(ctx.json({
      content: [
        { id: 1, nombre: 'Grupo de Estudio Matemáticas', tipoGrupo: 'COMUNIDAD_PUBLICA' },
        { id: 2, nombre: 'Física Avanzada', tipoGrupo: 'GRUPO_PRIVADO' }
      ],
      page: { number: 0, size: 20, totalElements: 2, totalPages: 1, first: true, last: true }
    }));
  }),
  // ... más handlers
];
```

### Verificar rol del usuario en contexto
```javascript
// Antes de mostrar botones de admin, verificar el rol
async function checkMemberRole(communityId) {
  const response = await fetch(`/api/v1/communities/${communityId}/members/me`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  
  if (response.ok) {
    const membership = await response.json();
    return membership.rol; // 'ADMIN' | 'MIEMBRO'
  }
  return null; // No es miembro
}
```

---

## 9. Guía de Implementación para Backend

### Estructura de DTOs recomendada

```java
// Request DTO
public record CreateCommunityRequest(
    @NotBlank @Size(min=3, max=100) String nombre,
    @Size(max=1000) String descripcion,
    @NotNull TipoGrupo tipoGrupo,
    String imagen
) {}

// Response DTO
public record CommunityDetailResponse(
    Long id,
    String nombre,
    String descripcion,
    TipoGrupo tipoGrupo,
    String imagen,
    Integer maxMiembros,
    Integer miembrosActuales,
    TipoPlanComunidad tipoPlan,
    EstadoComunidad estado,
    UserPublicResponse creador,
    TutorResponse TutorContratado,
    Boolean tieneClassroom,
    RolComunidad miRol,  // Rol del usuario autenticado en ESTA comunidad
    Boolean esMiembro,
    LocalDateTime createdAt
) {}
```

### Evaluación de permisos por contexto

```java
@Service
public class AuthorizationService {
    
    public RolComunidad getUserRoleInCommunity(Long userId, Long communityId) {
        return miembroComunidadRepository
            .findByUsuarioIdAndComunidadId(userId, communityId)
            .map(MiembroComunidad::getRol)
            .orElse(null);
    }
    
    public boolean isAdminOfCommunity(Long userId, Long communityId) {
        return RolComunidad.ADMIN.equals(getUserRoleInCommunity(userId, communityId));
    }
    
    public boolean canEditEvent(Long userId, Long eventId) {
        Evento evento = eventoRepository.findById(eventId).orElseThrow();
        // Es creador del evento O admin de la comunidad
        return evento.getCreador().getId().equals(userId) 
            || isAdminOfCommunity(userId, evento.getComunidad().getId());
    }
}
```

### Ejemplo de Controller con verificación de contexto

```java
@RestController
@RequestMapping("/api/v1/communities/{communityId}")
public class CommunityController {
    
    @PutMapping
    @PreAuthorize("@authService.isAdminOfCommunity(authentication.principal.id, #communityId)")
    public ResponseEntity<CommunityDetailResponse> updateCommunity(
            @PathVariable Long communityId,
            @Valid @RequestBody UpdateCommunityRequest request) {
        // Solo llega aquí si es admin de esta comunidad específica
        return ResponseEntity.ok(communityService.update(communityId, request));
    }
}
```

---

## Historial de Versiones

| Versión | Fecha | Descripción |
|---------|-------|-------------|
| 1.0.0 | 2026-02-19 | Versión inicial del contrato API |

---

## Referencias

- [Especificación OpenAPI](./openapi.yaml)
- [Diagrama de Clases MVP](../diagramas/diagrama_clases_mvp.puml)
- [Requisitos Funcionales](../Sprint%20DP/Semana%201/Elicitación%20de%20Requisitos.md)
