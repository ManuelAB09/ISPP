# Contexts (Contextos)

## Descripción

Esta carpeta contiene los **archivos de contexto** de la aplicación. Los contextos en React son una característica que permite compartir datos globales entre componentes sin necesidad de pasar props manualmente a través de cada nivel del árbol de componentes.

## ¿Qué es el Context API?

El Context API es una funcionalidad nativa de React que permite:

- **Gestionar estado global**: Compartir estado entre múltiples componentes sin prop drilling.
- **Proveer datos**: Hacer accesible información en cualquier parte del árbol de componentes.
- **Evitar redundancia**: Eliminar la necesidad de pasar props a través de componentes intermedios.

## Casos de Uso Comunes

Los contextos son ideales para manejar:

- **Autenticación**: Información del usuario logueado, token de sesión, permisos.
- **Tema**: Modo claro/oscuro, preferencias de diseño.
- **Idioma**: Internacionalización y traducciones.
- **Configuración global**: Ajustes de la aplicación compartidos.
- **Estado de la aplicación**: Datos que necesitan ser accesibles desde múltiples lugares.
- **Notificaciones**: Sistema de alertas o mensajes globales.

## Estructura de la Carpeta

Cada contexto debe estar en su propio archivo dentro de esta carpeta:

```
contexts/
  ├── AuthContext.js          # Contexto de autenticación
  ├── ThemeContext.js         # Contexto de tema
  ├── LanguageContext.js      # Contexto de idioma
  ├── NotificationContext.js  # Contexto de notificaciones
  └── README.md               # Este archivo
```

## Componentes Principales de un Context

Un archivo de contexto típicamente incluye:

1. **Context**: El objeto de contexto creado con `React.createContext()`.
2. **Provider**: Componente que envuelve la aplicación o parte de ella y provee los valores del contexto.
3. **Hook personalizado**: Función que facilita el consumo del contexto (opcional pero recomendado).

## Uso de Contextos en la Aplicación

### Estructura General

Cada archivo de contexto generalmente sigue este patrón:

- Crear el contexto
- Crear el Provider con la lógica de estado
- Exportar un hook personalizado para consumir el contexto
- Envolver los componentes que necesitan acceder al contexto

### Envolver la Aplicación

Los Providers se suelen ubicar en niveles altos del árbol de componentes (`index.js`) para que estén disponibles en toda la aplicación.

### Consumir Contextos

Los componentes que necesiten acceder a los datos del contexto pueden hacerlo usando el hook personalizado exportado, lo que simplifica el código y mejora la legibilidad.