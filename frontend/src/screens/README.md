# Screens (Pantallas)

## Descripción

Esta carpeta contiene los **archivos de pantallas** (o vistas) de la aplicación. Cada screen representa una página completa o vista principal que el usuario puede navegar. A diferencia de los componentes, las screens están asociadas directamente a rutas de navegación.

## Estructura Organizativa

Las screens se organizan en **subcarpetas** según su funcionalidad o módulo de la aplicación. Cada subcarpeta agrupa las pantallas relacionadas con una característica específica.

### Ejemplo de Estructura

```
screens/
  ├── auth/                    # Pantallas de autenticación
  │   ├── Login.js
  │   ├── Register.js
  │   └── ForgotPassword.js
  ├── home/                    # Pantallas del inicio
  │   ├── Home.js
  │   └── Dashboard.js
  ├── profile/                 # Pantallas de perfil
  │   ├── Profile.js
  │   └── EditProfile.js
  ├── community/               # Pantallas de comunidades
  │   ├── CommunityList.js
  │   └── CommunityDetail.js
  └── README.md
```

## Convenciones de Nomenclatura

- **Carpetas**: Nombres en minúscula que representen el módulo o funcionalidad (ej: `auth`, `home`, `profile`)
- **Archivos**: Nombres en **mayúscula** (PascalCase) con extensión **`.js`** (ej: `Login.js`, `Dashboard.js`)
- Esta convención diferencia las screens (`.js`) de los componentes (`.jsx`)

## Diferencia entre Screens y Components

| Aspecto | Screens | Components |
|---------|---------|------------|
| **Propósito** | Páginas completas asociadas a rutas | Piezas reutilizables de UI |
| **Extensión** | `.js` | `.jsx` |
| **Nomenclatura** | PascalCase (mayúscula) | PascalCase (mayúscula) |
| **Ubicación** | `src/screens/` | `src/components/` |
| **Organización** | Subcarpetas por módulo | Por componente individual |

## Características de las Screens

- **Asociadas a rutas**: Cada screen típicamente corresponde a una ruta de React Router
- **Composición**: Las screens utilizan componentes reutilizables de la carpeta `components`
- **Lógica de negocio**: Pueden contener lógica específica de la página y llamadas a endpoints
- **Contextos**: Consumen contextos para acceder a estado global
- **Layout completo**: Representan vistas completas, no fragmentos

## Organización por Módulos

Las pantallas están agrupadas en subcarpetas según el módulo funcional al que pertenecen:

- **`auth/`** - Pantallas relacionadas con autenticación (login, registro, recuperación de contraseña)
- **`home/`** - Pantallas principales y dashboard
- **`profile/`** - Pantallas de gestión de perfil de usuario
- **`community/`** - Pantallas de gestión de comunidades
- **`events/`** - Pantallas de eventos
- etc.

Esta organización facilita la navegación del código y mantiene relacionadas las pantallas que trabajan con funcionalidades similares.

## Relación con Otras Carpetas

- **Components**: Las screens importan y utilizan componentes reutilizables
- **Contexts**: Las screens consumen contextos para acceder a estado global
- **API**: Las screens utilizan los endpoints para obtener o enviar datos al backend
- **Utils**: Pueden usar funciones utilitarias para procesamiento de datos