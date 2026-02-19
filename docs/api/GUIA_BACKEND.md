# Guía Backend - Uso de la API OpenAPI

## MeerKatters

### Grupo D – Turno de tarde

![Logo App](../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Guía técnica  
**Sprint:** Sprint 1  
**Semana:** Semana 1  
**Estado:** En revisión  
**Fecha:** 19/02/2026  
**Autor(es):** Equipo de arquitectura

---

Esta guía explica cómo usar el archivo `openapi.yaml` para implementar los endpoints en Spring Boot.

---

## Swagger UI Desplegado

El proyecto tiene **Swagger UI desplegado** en:

```
http://localhost:8080/swagger-ui.html
```

> **IMPORTANTE**: Swagger UI muestra la documentación completa de la API (todos los endpoints, parámetros y respuestas esperadas), pero **"Try it out" NO funciona** porque los endpoints aún no están implementados. Es solo para **visualización y referencia** mientras desarrollas.

### Selector de Servers (Dropdown)

En la parte superior de Swagger UI hay un **dropdown de "Servers"** que permite seleccionar la URL base:

| Server | URL | Uso |
|--------|-----|-----|
| **Producción** | `https://api.meerkatters.com/v1` | Versión final desplegada |
| **Staging** | `https://staging-api.meerkatters.com/v1` | Pruebas pre-producción |
| **Desarrollo** | `http://localhost:8080/api/v1` | Tu máquina local |

Esto permite probar contra diferentes entornos sin cambiar la configuración. Actualmente solo **Desarrollo** funcionará (cuando implementes los endpoints).

---

## ¿Qué es openapi.yaml?

Es el **contrato de la API** - define exactamente qué endpoints existen, qué parámetros reciben y qué respuestas devuelven. Tu trabajo es implementar ese contrato en Java.

---

## 1. Configuración inicial (ya hecha)

El proyecto ya tiene SpringDoc configurado. Cuando ejecutes la aplicación:

```powershell
cd backend
mvn spring-boot:run
```

Podrás acceder a:
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/api-docs

---

## 2. Cómo leer el openapi.yaml

### Estructura básica de un endpoint

```yaml
/api/v1/users/{id}:          # URL del endpoint
  get:                        # Método HTTP
    tags:
      - Usuarios              # Categoría (aparece en Swagger)
    summary: Obtener usuario  # Título corto
    security:
      - bearerAuth: []        # Requiere token JWT
    parameters:
      - name: id              # Parámetro de la URL
        in: path
        required: true
        schema:
          type: integer
    responses:
      '200':                  # Código de respuesta
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UserResponse'  # DTO de respuesta
```

### Schemas (DTOs)

Al final del archivo están los schemas - son los DTOs que debes crear:

```yaml
components:
  schemas:
    UserResponse:
      type: object
      properties:
        id:
          type: integer
          format: int64
        email:
          type: string
          format: email
        nombre:
          type: string
```

---

## 3. Implementar un endpoint paso a paso

### Ejemplo: GET /api/v1/users/{id}

**Paso 1: Crear el DTO de respuesta**

Mira el schema `UserResponse` en openapi.yaml y créalo en Java:

```java
// src/main/java/es/us/meerkatters/dto/UserResponse.java
package es.us.meerkatters.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos públicos de un usuario")
public record UserResponse(
    @Schema(description = "ID único del usuario", example = "1")
    Long id,
    
    @Schema(description = "Email del usuario", example = "juan@example.com")
    String email,
    
    @Schema(description = "Nombre completo", example = "Juan García")
    String nombre,
    
    @Schema(description = "URL del avatar")
    String avatarUrl
) {}
```

**Paso 2: Crear el controlador**

```java
// src/main/java/es/us/meerkatters/controller/UserController.java
package es.us.meerkatters.controller;

import es.us.meerkatters.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuarios", description = "Gestión de usuarios")
public class UserController {

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID", 
               description = "Devuelve los datos públicos de un usuario")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuario no existe")
    })
    public ResponseEntity<UserResponse> getUser(
            @Parameter(description = "ID del usuario") 
            @PathVariable Long id) {
        
        // TODO: Implementar lógica real con servicio
        UserResponse user = new UserResponse(
            id, 
            "ejemplo@test.com", 
            "Usuario Ejemplo",
            null
        );
        return ResponseEntity.ok(user);
    }
}
```

**Paso 3: Verificar en Swagger UI**

1. Ejecuta: `mvn spring-boot:run`
2. Abre: http://localhost:8080/swagger-ui.html
3. Busca la categoría "Usuarios"
4. Prueba el endpoint directamente desde el navegador

---

## 4. Anotaciones de Swagger más usadas

### En el Controlador (clase)

```java
@Tag(name = "Usuarios", description = "Gestión de usuarios")
```

### En el Método

```java
@Operation(
    summary = "Título corto",
    description = "Descripción larga del endpoint"
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Éxito"),
    @ApiResponse(responseCode = "400", description = "Datos inválidos"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "No encontrado")
})
```

### En Parámetros

```java
// Path parameter
@Parameter(description = "ID del usuario", required = true)
@PathVariable Long id

// Query parameter
@Parameter(description = "Página (empieza en 0)")
@RequestParam(defaultValue = "0") int page

// Request body
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    description = "Datos del nuevo usuario"
)
@RequestBody CreateUserRequest request
```

### En DTOs

```java
@Schema(description = "Datos para crear usuario")
public record CreateUserRequest(
    
    @Schema(description = "Email", example = "juan@test.com", required = true)
    @NotBlank
    String email,
    
    @Schema(description = "Contraseña", minLength = 8, required = true)
    @Size(min = 8)
    String password
) {}
```

---

## 5. Implementar Request Bodies

Cuando el endpoint recibe datos en el body (POST, PUT, PATCH):

**openapi.yaml:**
```yaml
/api/v1/users:
  post:
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/CreateUserRequest'
```

**Java:**
```java
// DTO de entrada
public record CreateUserRequest(
    @NotBlank String email,
    @Size(min = 8) String password,
    @NotBlank String nombre
) {}

// Controlador
@PostMapping
@Operation(summary = "Crear usuario")
public ResponseEntity<UserResponse> createUser(
        @Valid @RequestBody CreateUserRequest request) {
    // Implementar...
    return ResponseEntity.status(201).body(nuevoUsuario);
}
```

---

## 6. Implementar Paginación

Muchos endpoints devuelven listas paginadas.

**openapi.yaml:**
```yaml
parameters:
  - name: page
    in: query
    schema:
      type: integer
      default: 0
  - name: size
    in: query
    schema:
      type: integer
      default: 20
```

**Java:**
```java
@GetMapping
@Operation(summary = "Listar usuarios")
public ResponseEntity<UserListResponse> listUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    
    Pageable pageable = PageRequest.of(page, size);
    Page<User> usuarios = userRepository.findAll(pageable);
    
    // Convertir a DTO y devolver
    return ResponseEntity.ok(convertToListResponse(usuarios));
}
```

---

## 7. Checklist para cada endpoint

Antes de dar por terminado un endpoint, verifica:

- [ ] El método HTTP es correcto (GET, POST, PUT, DELETE, PATCH)
- [ ] La URL coincide exactamente con openapi.yaml
- [ ] Los parámetros de path/query tienen los nombres correctos
- [ ] El DTO de request tiene todos los campos del schema
- [ ] El DTO de response tiene todos los campos del schema
- [ ] Los códigos de respuesta están documentados con `@ApiResponse`
- [ ] Aparece correctamente en Swagger UI
- [ ] Funciona al probarlo desde Swagger UI

---

## 8. Mapeo openapi.yaml → Spring Boot

| OpenAPI | Spring Boot |
|---------|-------------|
| `get:` | `@GetMapping` |
| `post:` | `@PostMapping` |
| `put:` | `@PutMapping` |
| `patch:` | `@PatchMapping` |
| `delete:` | `@DeleteMapping` |
| `parameters: in: path` | `@PathVariable` |
| `parameters: in: query` | `@RequestParam` |
| `requestBody:` | `@RequestBody` |
| `type: integer, format: int64` | `Long` |
| `type: integer` | `Integer` |
| `type: string` | `String` |
| `type: string, format: date-time` | `LocalDateTime` |
| `type: string, format: date` | `LocalDate` |
| `type: boolean` | `Boolean` |
| `type: array` | `List<T>` |
| `nullable: true` | Campo puede ser `null` |

---

## 9. Endpoints prioritarios (MVP)

Empieza implementando estos endpoints en orden:

### Sprint 1 - Autenticación
1. `POST /api/v1/auth/register`
2. `POST /api/v1/auth/login`
3. `POST /api/v1/auth/refresh`
4. `GET /api/v1/users/me`

### Sprint 2 - Usuarios y Comunidades
1. `GET /api/v1/users/{id}`
2. `PUT /api/v1/users/me`
3. `POST /api/v1/communities`
4. `GET /api/v1/communities`
5. `GET /api/v1/communities/{id}`

### Sprint 3 - Membresías y Eventos
1. `POST /api/v1/communities/{id}/join`
2. `GET /api/v1/communities/{id}/members`
3. `POST /api/v1/communities/{id}/events`
4. `GET /api/v1/communities/{id}/events`

---

## 10. Recursos útiles

- [SpringDoc documentación oficial](https://springdoc.org/)
- [OpenAPI 3.0 Specification](https://spec.openapis.org/oas/v3.0.3)
- [Swagger Annotations](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)

---

## Preguntas frecuentes

**¿Tengo que documentar todo manualmente?**
No. SpringDoc genera documentación automática. Las anotaciones son para enriquecer con descripciones, ejemplos, etc.

**¿Qué pasa si el frontend necesita algo diferente?**
Primero actualiza el `openapi.yaml` (el contrato), luego implementa el cambio.

**¿Cómo pruebo endpoints que requieren autenticación?**
En Swagger UI hay un botón "Authorize" - pega ahí el token JWT que obtienes de `/auth/login`.
