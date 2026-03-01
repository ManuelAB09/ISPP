# Guía Frontend - Uso de la API OpenAPI

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

Esta guía explica cómo usar el archivo `openapi.yaml` para desarrollar el frontend mientras el backend aún no está implementado.

---

## Swagger UI Desplegado

El proyecto tiene **Swagger UI desplegado** en:

```
http://localhost:8080/swagger-ui.html
```

> **IMPORTANTE**: Swagger UI carga la especificación directamente del propio backend. Al implementar controladores, los nuevos endpoints aparecen automáticamente y el botón **"Try it out"** ejecuta las llamadas reales contra tu servidor local. Además puedes elegir entre la versión dinámica y una copia estática (`/spec/openapi.yaml`) usando el selector "Select a definition" en la esquina superior derecha.
>
> Para desarrollar tu código frontend, usa **Prism** (mock server) como se explica más abajo.

### Selector de Servers (Dropdown)

En la parte superior de Swagger UI hay un **dropdown de "Servers"** que permite seleccionar la URL base:

| Server | URL | Uso |
|--------|-----|-----|
| **Producción** | `https://api.meerkatters.com/v1` | Versión final desplegada |
| **Staging** | `https://staging-api.meerkatters.com/v1` | Pruebas pre-producción |
| **Desarrollo** | `http://localhost:8080/api/v1` | Backend local |

Este selector es útil para entender contra qué URL apuntar. Para frontend en desarrollo, usa **Prism en localhost:4010**.

---

## ¿Qué es openapi.yaml?

Es el **contrato de la API** - define exactamente qué endpoints existirán, qué datos enviar y qué respuestas esperar. Puedes usarlo para:

1. **Mockear la API** - Simular respuestas sin backend real
2. **Consultar la documentación** - Ver qué endpoints hay disponibles
3. **Validar tus requests** - Asegurar que envías los datos correctos

---

## 1. Configuración del Mock Server (Prism)

### Instalación

```bash
npm install -g @stoplight/prism-cli
```

### Ejecutar el mock server

```bash
cd docs/api
prism mock openapi.yaml
```

Esto levanta un servidor en `http://127.0.0.1:4010` que responde a todos los endpoints definidos con datos de ejemplo.

### Ejemplo de uso

Con el mock corriendo:

```bash
# Obtener un usuario
curl http://127.0.0.1:4010/api/v1/users/1

# Respuesta automática con datos de ejemplo:
{
  "id": 1,
  "email": "usuario@example.com",
  "nombre": "string",
  ...
}
```

---

## 2. Cómo leer el openapi.yaml

### Estructura de un endpoint

```yaml
/api/v1/users/{id}:          # URL (el {id} es un parámetro)
  get:                        # Método HTTP
    summary: Obtener usuario  # Descripción corta
    parameters:               # Parámetros que recibe
      - name: id
        in: path              # Va en la URL
        required: true
        schema:
          type: integer
    responses:
      '200':                  # Código de éxito
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UserResponse'  # Estructura de respuesta
      '404':                  # Código de error
        description: Usuario no encontrado
```

### Schemas (estructuras de datos)

Al final del archivo están los schemas - úsalos para saber qué campos esperar:

```yaml
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
    avatarUrl:
      type: string
      nullable: true    # Puede ser null
```

### Mapeo de tipos OpenAPI → JavaScript

| OpenAPI | JavaScript |
|---------|------------|
| `type: integer` | `number` |
| `type: number` | `number` |
| `type: string` | `string` |
| `type: boolean` | `boolean` |
| `type: array, items: X` | `Array` |
| `nullable: true` | Puede ser `null` |
| `type: string, format: date-time` | `string` (ISO 8601) |

---

## 3. Crear el cliente API

### Estructura recomendada

```
src/
├── api/
│   ├── client.js        # Configuración base (fetch)
│   ├── auth.api.js      # Endpoints de autenticación
│   ├── users.api.js     # Endpoints de usuarios
│   ├── communities.api.js
│   └── index.js         # Re-exporta todo
```

### client.js - Configuración base

```javascript
// src/api/client.js
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://127.0.0.1:4010';

class ApiClient {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
    this.token = null;
  }

  setToken(token) {
    this.token = token;
  }

  async request(method, path, body = null) {
    const headers = {
      'Content-Type': 'application/json',
    };

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }

    const options = {
      method,
      headers,
    };

    if (body) {
      options.body = JSON.stringify(body);
    }

    const response = await fetch(`${this.baseUrl}${path}`, options);

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new ApiError(response.status, error.message || 'Error desconocido');
    }

    // Si la respuesta es 204 No Content, no hay body
    if (response.status === 204) {
      return null;
    }

    return response.json();
  }

  get(path) {
    return this.request('GET', path);
  }

  post(path, body) {
    return this.request('POST', path, body);
  }

  put(path, body) {
    return this.request('PUT', path, body);
  }

  patch(path, body) {
    return this.request('PATCH', path, body);
  }

  delete(path) {
    return this.request('DELETE', path);
  }
}

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
    this.name = 'ApiError';
  }
}

export const apiClient = new ApiClient(API_BASE_URL);
```

### auth.api.js - Ejemplo de módulo

```javascript
// src/api/auth.api.js
import { apiClient } from './client';

export const authApi = {
  /**
   * POST /api/v1/auth/login
   * Iniciar sesión con email y contraseña
   * @param {Object} data - { email: string, password: string }
   * @returns {Promise<Object>} - { accessToken, refreshToken, expiresIn, user }
   */
  login(data) {
    return apiClient.post('/api/v1/auth/login', data);
  },

  /**
   * POST /api/v1/auth/register
   * Registrar nuevo usuario
   * @param {Object} data - { email, password, nombre }
   * @returns {Promise<Object>} - { accessToken, refreshToken, expiresIn, user }
   */
  register(data) {
    return apiClient.post('/api/v1/auth/register', data);
  },

  /**
   * POST /api/v1/auth/refresh
   * Renovar token de acceso
   * @param {string} refreshToken
   * @returns {Promise<Object>} - { accessToken, refreshToken, expiresIn }
   */
  refresh(refreshToken) {
    return apiClient.post('/api/v1/auth/refresh', { refreshToken });
  },

  /**
   * GET /api/v1/users/me
   * Obtener perfil del usuario autenticado
   * @returns {Promise<Object>} - UserResponse
   */
  getMe() {
    return apiClient.get('/api/v1/users/me');
  },
};
```

### users.api.js - Otro ejemplo

```javascript
// src/api/users.api.js
import { apiClient } from './client';

export const usersApi = {
  /**
   * GET /api/v1/users/{id}
   * @param {number} id - ID del usuario
   * @returns {Promise<Object>} - UserResponse
   */
  getById(id) {
    return apiClient.get(`/api/v1/users/${id}`);
  },

  /**
   * GET /api/v1/users?search=...&page=0&size=20
   * @param {Object} params - { search?, page?, size? }
   * @returns {Promise<Object>} - UserListResponse
   */
  search(params = {}) {
    const query = new URLSearchParams();
    if (params.search) query.set('search', params.search);
    if (params.page !== undefined) query.set('page', String(params.page));
    if (params.size !== undefined) query.set('size', String(params.size));
    
    const queryString = query.toString();
    return apiClient.get(`/api/v1/users${queryString ? '?' + queryString : ''}`);
  },

  /**
   * PUT /api/v1/users/me
   * @param {Object} data - { nombre?, avatarUrl?, bio? }
   * @returns {Promise<Object>} - UserResponse
   */
  updateMe(data) {
    return apiClient.put('/api/v1/users/me', data);
  },
};
```

### communities.api.js - Ejemplo con comunidades

```javascript
// src/api/communities.api.js
import { apiClient } from './client';

export const communitiesApi = {
  /**
   * GET /api/v1/communities
   * Explorar comunidades públicas
   * @param {Object} params - { search?, page?, size? }
   */
  list(params = {}) {
    const query = new URLSearchParams();
    if (params.search) query.set('search', params.search);
    if (params.page !== undefined) query.set('page', String(params.page));
    if (params.size !== undefined) query.set('size', String(params.size));
    
    const queryString = query.toString();
    return apiClient.get(`/api/v1/communities${queryString ? '?' + queryString : ''}`);
  },

  /**
   * POST /api/v1/communities
   * Crear nueva comunidad
   * @param {Object} data - { nombre, descripcion?, tipoGrupo, imagen? }
   */
  create(data) {
    return apiClient.post('/api/v1/communities', data);
  },

  /**
   * GET /api/v1/communities/{id}
   * Obtener detalle de comunidad
   * @param {number} id
   */
  getById(id) {
    return apiClient.get(`/api/v1/communities/${id}`);
  },

  /**
   * POST /api/v1/communities/{id}/members
   * Unirse a comunidad pública
   * @param {number} communityId
   */
  join(communityId) {
    return apiClient.post(`/api/v1/communities/${communityId}/members`, {});
  },

  /**
   * GET /api/v1/communities/{id}/members
   * Listar miembros de una comunidad
   * @param {number} communityId
   * @param {Object} params - { page?, size? }
   */
  getMembers(communityId, params = {}) {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.set('page', String(params.page));
    if (params.size !== undefined) query.set('size', String(params.size));
    
    const queryString = query.toString();
    return apiClient.get(`/api/v1/communities/${communityId}/members${queryString ? '?' + queryString : ''}`);
  },
};
```

### index.js - Re-exportar todo

```javascript
// src/api/index.js
export { apiClient, ApiError } from './client';
export { authApi } from './auth.api';
export { usersApi } from './users.api';
export { communitiesApi } from './communities.api';
```

---

## 4. Uso en componentes React

### Hook personalizado para datos

```javascript
// src/hooks/useUser.js
import { useState, useEffect } from 'react';
import { usersApi } from '../api';

export function useUser(userId) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!userId) return;
    
    setLoading(true);
    setError(null);
    
    usersApi.getById(userId)
      .then(data => setUser(data))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [userId]);

  return { user, loading, error };
}
```

### Componente que usa el hook

```jsx
// src/components/UserProfile.js
import { useUser } from '../hooks/useUser';

function UserProfile({ userId }) {
  const { user, loading, error } = useUser(userId);

  if (loading) return <div>Cargando...</div>;
  if (error) return <div>Error: {error}</div>;
  if (!user) return <div>Usuario no encontrado</div>;

  return (
    <div>
      <h1>{user.nombre}</h1>
      <p>{user.email}</p>
      {user.avatarUrl && <img src={user.avatarUrl} alt="Avatar" />}
    </div>
  );
}

export default UserProfile;
```

### Componente con formulario (login)

```jsx
// src/components/LoginForm.js
import { useState } from 'react';
import { authApi, apiClient, ApiError } from '../api';

function LoginForm({ onSuccess }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await authApi.login({ email, password });
      
      // Guardar tokens
      localStorage.setItem('accessToken', response.accessToken);
      localStorage.setItem('refreshToken', response.refreshToken);
      
      // Configurar cliente para futuras peticiones
      apiClient.setToken(response.accessToken);
      
      // Notificar éxito
      onSuccess(response.user);
    } catch (err) {
      if (err instanceof ApiError) {
        switch (err.status) {
          case 400:
            setError('Email o contraseña inválidos');
            break;
          case 401:
            setError('Credenciales incorrectas');
            break;
          default:
            setError('Error del servidor');
        }
      } else {
        setError('Error de conexión');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {error && <div className="error">{error}</div>}
      
      <input
        type="email"
        placeholder="Email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        required
      />
      
      <input
        type="password"
        placeholder="Contraseña"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        required
      />
      
      <button type="submit" disabled={loading}>
        {loading ? 'Cargando...' : 'Iniciar sesión'}
      </button>
    </form>
  );
}

export default LoginForm;
```

### Componente para listar comunidades

```jsx
// src/components/CommunityList.js
import { useState, useEffect } from 'react';
import { communitiesApi } from '../api';

function CommunityList() {
  const [communities, setCommunities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    setLoading(true);
    communitiesApi.list({ page, size: 10 })
      .then(response => {
        setCommunities(response.content);
        setTotalPages(response.page.totalPages);
      })
      .catch(err => console.error(err))
      .finally(() => setLoading(false));
  }, [page]);

  if (loading) return <div>Cargando comunidades...</div>;

  return (
    <div>
      <h2>Comunidades</h2>
      <ul>
        {communities.map(community => (
          <li key={community.id}>
            <h3>{community.nombre}</h3>
            <p>{community.descripcion}</p>
            <span>{community.miembrosActuales} miembros</span>
          </li>
        ))}
      </ul>
      
      {/* Paginación */}
      <div>
        <button 
          onClick={() => setPage(p => p - 1)} 
          disabled={page === 0}
        >
          Anterior
        </button>
        <span>Página {page + 1} de {totalPages}</span>
        <button 
          onClick={() => setPage(p => p + 1)} 
          disabled={page >= totalPages - 1}
        >
          Siguiente
        </button>
      </div>
    </div>
  );
}

export default CommunityList;
```

---

## 5. Configurar variables de entorno

### .env.development (mock)

```env
REACT_APP_API_URL=http://127.0.0.1:4010
```

### .env.production (producción)

```env
REACT_APP_API_URL=https://meerkatters.azurewebsites.net

> ⚠️ Si la variable se deja en blanco o se establece en un valor inválido
> (por ejemplo solo ":8080"), el cliente la ignorará y usará
> `http://localhost:8080` por defecto gracias a una comprobación añadida
> en `src/api/client.js`. Esto evita los errores `net::ERR_NAME_NOT_RESOLVED`.
```

### .env.local (backend local)

```env
REACT_APP_API_URL=http://localhost:8080   # suele ser la opción más segura, evita asignar solo ":8080" por error
```

> **Nota:** Usa `REACT_APP_` como prefijo (requerido por Create React App).

---

## 6. Flujo de desarrollo paso a paso

### Paso 1: Levantar el mock

```bash
cd docs/api
prism mock openapi.yaml
```

### Paso 2: Identificar el endpoint que necesitas

Abre `openapi.yaml` y busca el endpoint. Por ejemplo, para login:

```yaml
/api/v1/auth/login:
  post:
    requestBody:
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/LoginRequest'
    responses:
      '200':
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LoginResponse'
```

### Paso 3: Ver la estructura de datos

Busca el schema referenciado:

```yaml
LoginRequest:
  type: object
  required:
    - email
    - password
  properties:
    email:
      type: string
      format: email
    password:
      type: string
      minLength: 8

LoginResponse:
  type: object
  properties:
    accessToken:
      type: string
    refreshToken:
      type: string
    expiresIn:
      type: integer
    user:
      $ref: '#/components/schemas/UserResponse'
```

### Paso 4: Crear la función API

```javascript
// Ya está hecha en auth.api.js
export const authApi = {
  login(data) {
    return apiClient.post('/api/v1/auth/login', data);
  },
};
```

### Paso 5: Usar en el componente

```javascript
const handleLogin = async (email, password) => {
  try {
    const response = await authApi.login({ email, password });
    apiClient.setToken(response.accessToken);
    // Guardar token, redirigir, etc.
  } catch (error) {
    // Manejar error
  }
};
```

### Paso 6: Probar

El mock de Prism responderá con datos de ejemplo. Cuando el backend esté listo, solo cambias `REACT_APP_API_URL`.

---

## 7. Manejo de errores

El openapi.yaml define los posibles errores:

```yaml
responses:
  '400':
    description: Datos inválidos
  '401':
    description: No autenticado
  '403':
    description: Sin permisos
  '404':
    description: No encontrado
  '500':
    description: Error del servidor
```

Manéjalos en tu código:

```javascript
import { ApiError } from './client';

try {
  await authApi.login(data);
} catch (error) {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 400:
        mostrarError('Datos inválidos');
        break;
      case 401:
        mostrarError('Credenciales incorrectas');
        break;
      case 403:
        mostrarError('No tienes permisos');
        break;
      case 404:
        mostrarError('No encontrado');
        break;
      default:
        mostrarError('Error del servidor');
    }
  } else {
    mostrarError('Error de conexión');
  }
}
```

---

## 8. Autenticación JWT

### Flujo completo de login

```javascript
// 1. Usuario hace login
const response = await authApi.login({ email, password });

// 2. Guardar tokens en localStorage
localStorage.setItem('accessToken', response.accessToken);
localStorage.setItem('refreshToken', response.refreshToken);

// 3. Configurar cliente para futuras peticiones
apiClient.setToken(response.accessToken);

// 4. Ya puedes llamar endpoints protegidos
const profile = await authApi.getMe();
```

### Recuperar sesión al cargar la app

```javascript
// src/App.js
import { useEffect, useState } from 'react';
import { apiClient, authApi } from './api';

function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    
    if (token) {
      apiClient.setToken(token);
      authApi.getMe()
        .then(userData => setUser(userData))
        .catch(() => {
          // Token inválido, limpiar
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  if (loading) return <div>Cargando...</div>;

  return user ? <Dashboard user={user} /> : <LoginPage />;
}
```

### Logout

```javascript
const handleLogout = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  apiClient.setToken(null);
  // Redirigir a login
};
```

---

## 9. Tips y trucos

### Ver Swagger UI (cuando backend esté listo)

http://localhost:8080/swagger-ui.html

Puedes probar endpoints directamente desde el navegador.

### Prism con datos dinámicos

```bash
# Modo dinámico - genera datos aleatorios más realistas
prism mock openapi.yaml --dynamic
```

### Validar tus requests

Prism valida que tus requests cumplan el schema:

```bash
# Si envías datos inválidos, Prism te lo dice
curl -X POST http://127.0.0.1:4010/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "no-es-email"}'
  
# Error: email must be a valid email
```

### Ver el openapi.yaml visualmente

Abre https://editor.swagger.io/ y pega el contenido de `openapi.yaml`.

---

## 10. Checklist para cada endpoint

- [ ] Identificar endpoint en openapi.yaml
- [ ] Revisar estructura de request y response
- [ ] Crear función en el módulo API correspondiente
- [ ] Probar con mock (Prism)
- [ ] Manejar casos de error
- [ ] Implementar en componente
- [ ] Probar con backend real cuando esté disponible

---

## Recursos útiles

- [Prism documentación](https://stoplight.io/open-source/prism)
- [Create React App - Variables de entorno](https://create-react-app.dev/docs/adding-custom-environment-variables/)
- [Swagger Editor online](https://editor.swagger.io/) - Visualizar el openapi.yaml
