# Elicitación de Requisitos - ISPP

> **Fecha:** Febrero 2025  
> **Versión:** 1.0  
> **Equipo:** Arquitectura/Integración
> **Fuentes:** Informe de Producto, Reunión con Cliente (Manuel Artero)

---

## 1. Introducción

Este documento recoge los requisitos elicitados para la plataforma web, una comunidad de estudiantes que facilita la organización de actividades de estudio colaborativo. Los requisitos se han obtenido mediante:

- Análisis del Informe de Producto
- Reunión con el cliente (Manuel Artero - Dirección)
- Sesión de trabajo del equipo de Arquitectura

---

## 2. Requisitos Funcionales

### 2.1 Gestión de Usuarios

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-01** | Registro de usuarios | El sistema debe permitir a los usuarios registrarse con email y contraseña | MVP |
| **RF-02** | Inicio de sesión | El sistema debe permitir a los usuarios autenticarse mediante email/contraseña | MVP |
| **RF-03** | Cierre de sesión | El sistema debe permitir a los usuarios cerrar su sesión | MVP |
| **RF-04** | Personalización de perfil | Los usuarios pueden modificar su foto, nombre e intereses | MVP |
| **RF-05** | Visualización de perfil | Los usuarios pueden ver su perfil y el de otros usuarios | MVP |
| **RF-06** | Inicio de sesión con Google | El sistema debe permitir OAuth con Google (OAuth 2.0) | Extra |
| **RF-07** | Autenticación de doble factor | El sistema permite activar 2FA opcional | Extra |
| **RF-08** | Cambiar contraseña | Los usuarios pueden cambiar su contraseña | MVP |
| **RF-09** | Eliminar cuenta | Los usuarios pueden eliminar su cuenta | MVP |

### 2.2 Gestión de Comunidades

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-10** | Crear comunidad | Cualquier usuario puede crear una comunidad de estudio | MVP |
| **RF-11** | Configurar privacidad | El creador decide si la comunidad es pública o privada | MVP |
| **RF-12** | Límite de comunidades (Free) | Usuarios gratuitos pueden crear máximo 3 comunidades | MVP |
| **RF-13** | Límite de miembros (Free) | Comunidades gratuitas tienen máximo 10-20 miembros | MVP |
| **RF-14** | Unirse a comunidad pública | Los usuarios pueden unirse a comunidades públicas | MVP |
| **RF-15** | Solicitar acceso a comunidad privada | Los usuarios pueden solicitar acceso a comunidades privadas | MVP |
| **RF-16** | Buscar comunidades | El sistema permite buscar comunidades públicas por nombre/temática/localización | MVP |
| **RF-17** | Explorar comunidades | Existe una sección para explorar comunidades públicas | MVP |
| **RF-18** | Rol de administrador | El creador de la comunidad es automáticamente administrador | MVP |
| **RF-19** | Aceptar/rechazar solicitudes | El admin puede aceptar o rechazar solicitudes de acceso | MVP |
| **RF-20** | Expulsar miembros | El admin puede expulsar miembros de la comunidad | MVP |
| **RF-21** | Abandonar comunidad | Los usuarios pueden salir de una comunidad voluntariamente | MVP |
| **RF-22** | Transferir administración | Si el admin abandona, debe elegir un sucesor | MVP |
| **RF-23** | Eliminar comunidad | El admin puede eliminar la comunidad completa | MVP |
| **RF-24** | Definir categorías | El admin puede definir categorías para organizar contenido | MVP |
| **RF-25** | Múltiples administradores | Permitir varios admins por comunidad | Extra |
| **RF-26** | Comunidades premium (más miembros) | Usuarios premium pueden crear comunidades con más miembros | Extra |
| **RF-27** | Comunidades corporativas ilimitadas | Instituciones pueden crear comunidades sin límite de miembros | Extra |
| **RF-28** | Chat de comunidad | Chat en tiempo real dentro de cada comunidad | Extra |

### 2.3 Gestión de Eventos/Quedadas

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-29** | Crear evento | Los miembros de una comunidad pueden crear eventos de estudio | MVP |
| **RF-30** | Configurar privacidad de evento | Los eventos pueden ser públicos o privados (independiente de la comunidad) | MVP |
| **RF-31** | Información de evento | Eventos incluyen: título, descripción, fecha/hora, ubicación, aforo | MVP |
| **RF-32** | Qué llevar | Se puede especificar qué materiales llevar al evento | MVP |
| **RF-33** | Integración Google Maps | Selección de ubicación mediante mapa interactivo | MVP |
| **RF-34** | Ubicaciones recomendadas | Mostrar lugares públicos recomendados (bibliotecas, etc.) | MVP |
| **RF-35** | Unirse a evento | Los usuarios pueden confirmar asistencia a eventos | MVP |
| **RF-36** | Cancelar asistencia | Los usuarios pueden cancelar su asistencia | MVP |
| **RF-37** | Ver asistentes | Se puede ver la lista de asistentes confirmados | MVP |
| **RF-38** | Editar evento | El creador puede modificar los detalles del evento | MVP |
| **RF-39** | Cancelar evento | El creador puede cancelar un evento | MVP |
| **RF-40** | Límite de aforo | Los eventos respetan el aforo máximo establecido | MVP |
| **RF-41** | Reuniones virtuales | Integrar videollamadas dentro de eventos | Extra |
| **RF-42** | Grabación de reuniones | Guardar grabaciones de sesiones de estudio | Extra |

### 2.4 Gestión de Contenido/Apuntes

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-43** | Subir archivos | Los miembros pueden subir apuntes/archivos a la comunidad | MVP |
| **RF-44** | Organizar por categorías | Los archivos se organizan según las categorías del admin | MVP |
| **RF-45** | Vincular a evento | Los archivos pueden asociarse a un evento específico | MVP |
| **RF-46** | Visualizar archivos | Los miembros pueden ver los archivos subidos | MVP |
| **RF-47** | Acceso a descarga | Los usuarios  pueden descargar archivos pagando | MVP |
| **RF-48** | Eliminar archivos | El autor o admin puede eliminar archivos | MVP |
| **RF-49** | Límite de descargas premium | Usuarios premium tienen X descargas gratuitas al mes | Extra |

### 2.5 Suscripciones y Pagos

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-50** | Visualizar planes | Los usuarios pueden ver los diferentes planes disponibles | MVP |
| **RF-51** | Suscribirse a plan premium | Proceso de pago para plan premium individual | MVP |
| **RF-52** | Integración de pagos | Integración con pasarela de pagos (Stripe o similar) | MVP |
| **RF-53** | Cancelar suscripción | Los usuarios pueden cancelar su suscripción | MVP |
| **RF-54** | Plan corporativo | Instituciones pueden contratar plan para sus miembros | Extra |
| **RF-55** | Invitación corporativa | Instituciones invitan usuarios por correo electrónico | Extra |
| **RF-56** | Beneficios corporativos | Usuarios de comunidades corporativas tienen acceso premium dentro de ellas | Extra |

### 2.6 Publicidad

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-57** | Mostrar anuncios | Usuarios gratuitos ven publicidad en la aplicación | MVP |
| **RF-58** | Sin anuncios premium | Usuarios premium no ven publicidad | MVP |

### 2.7 Ajustes y Preferencias

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-59** | Panel de ajustes | Sección de configuración de la cuenta | MVP |
| **RF-60** | Configurar notificaciones | Preferencias de notificaciones por email | Extra |
| **RF-61** | Notificaciones por email | Envío de emails para eventos importantes | Extra |

---

## 3. Requisitos No Funcionales

### 3.1 Rendimiento

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RNF-01** | Tiempo de carga | Las páginas deben cargar rápido para no afectar a la experiencia de usuario | MVP |


### 3.2 Seguridad

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RNF-02** | Contraseñas seguras | Las contraseñas deben estar hasheadas | MVP |
| **RNF-03** | Tokens JWT | Autenticación basada en JSON Web Tokens | MVP |
| **RNF-04** | Protección CSRF | Implementar protección contra CSRF | MVP |
| **RNF-05** | Validación de entrada | Validar todos los datos de entrada | MVP |
| **RNF-06** | HTTPS | Toda comunicación debe ser cifrada | Extra |

### 3.3 Usabilidad

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RNF-07** | Diseño responsive | La aplicación debe ser usable en móviles y escritorio | MVP |
| **RNF-08** | Compatibilidad navegadores | Compatible con Chrome, Firefox, Safari, Edge | MVP |
| **RNF-09** | Idioma español | La interfaz debe estar en español | MVP |
| **RNF-10** | Multi-idioma | Soporte para múltiples idiomas | Extra |

### 3.4 Calidad de Código

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RNF-11** | Cobertura de tests | Mínimo 70% de cobertura de código | MVP |
| **RNF-12** | Tests unitarios | Implementar tests unitarios | MVP |
| **RNF-13** | Tests de integración | Implementar tests de integración | MVP |
| **RNF-14** | Análisis SonarQube | Código analizado con SonarQube | MVP |
| **RNF-15** | Linting | Código formateado con linters (ESLint, Checkstyle) | Extra |
| **RNF-16** | Pre-commit hooks | Validación de código antes de commits | Extra |

### 3.5 Infraestructura

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RNF-17** | Arquitectura monolítica modular | Monolito con módulos bien separados | MVP |
| **RNF-18** | Despliegue PaaS | Despliegue en plataforma como servicio (Render, Railway) | MVP |
| **RNF-19** | CI/CD | Pipeline de integración y despliegue continuo | MVP |
| **RNF-20** | Control de versiones | Git con flujo de ramas y Pull Requests | MVP |
| **RNF-21** | Almacenamiento de archivos | Uso de servicio de almacenamiento (AWS S3/MinIO) | MVP |

### 3.6 Documentación

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RNF-22** | Diagramas UML | Documentación con diagramas en UML | MVP |
| **RNF-23** | API documentada | Documentación de endpoints con OpenAPI/Swagger | Extra |

---

## 4. Requisitos de Información

| ID | Entidad | Atributos Principales | Tipo |
|----|---------|----------------------|-----|
| **RI-01** | Usuario | id, email, password, nombre, foto, bio, suscripcion_id, created_at | MVP |
| **RI-02** | Comunidad | id, nombre, descripcion, privada, imagen, creador_id, max_miembros, created_at | MVP |
| **RI-03** | MiembroComunidad | id, usuario_id, comunidad_id, rol, fecha_ingreso | MVP |
| **RI-04** | Evento | id, titulo, descripcion, fecha_hora, ubicacion, latitud, longitud, aforo, comunidad_id, creador_id | MVP |
| **RI-05** | AsistenciaEvento | id, evento_id, usuario_id, confirmado, created_at | MVP |
| **RI-06** | Archivo | id, nombre, url, tipo, tamaño, comunidad_id, evento_id, usuario_id, categoria_id, created_at | MVP |
| **RI-07** | Categoria | id, nombre, comunidad_id | MVP |
| **RI-08** | Suscripcion | id, plan, fecha_inicio, fecha_fin, activa | MVP |
| **RI-09** | SolicitudComunidad | id, usuario_id, comunidad_id, estado, created_at | MVP |

---

## 5. Reglas de Negocio

| ID | Regla | Tipo |
|----|-------|-----|
| **RN-01** | Un usuario gratuito puede crear máximo 3 comunidades | MVP |
| **RN-02** | Una comunidad gratuita puede tener máximo 10-20 miembros simultáneos | MVP |
| **RN-03** | Un usuario puede estar en máximo 3 comunidades como miembro (plan gratuito) | MVP |
| **RN-05** | Los eventos respetan el aforo máximo definido | MVP |
| **RN-06** | Si el único admin abandona la comunidad, debe transferir el rol | MVP |
| **RN-07** | Los usuarios de comunidades corporativas tienen acceso premium dentro de ellas | Extra |
| **RN-08** | Los usuarios premium pueden crear comunidades de más de 20 miembros | Extra |

---

## 6. Actores del Sistema

| Actor | Descripción |
|-------|-------------|
| **Usuario no autenticado** | Visitante que puede ver información pública y registrarse |
| **Usuario autenticado (Free)** | Usuario registrado con funcionalidades limitadas |
| **Usuario Premium** | Usuario con suscripción que tiene acceso completo |
| **Administrador de comunidad** | Usuario que gestiona una comunidad |
| **Sistema** | Procesos automáticos (notificaciones, limpieza, etc.) |

---
