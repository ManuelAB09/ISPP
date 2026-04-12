# Documentación General de la Aplicación

## MeerKatters

### Grupo D - Turno de tarde

![Logo App](./images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Overview funcional y técnico  
**Sprint:** Sprint 3  
**Semana:** Semana 2  
**Estado:** Actualizado a partir del código fuente  
**Fecha:** 12/04/2026  
**Autor(es):** Análisis técnico del repositorio

---

## Índice
1. [Introducción](#1-introducción)
2. [Caso de uso y problema que resuelve](#2-caso-de-uso-y-problema-que-resuelve)
3. [Tipos de usuario](#3-tipos-de-usuario)
4. [Arquitectura general](#4-arquitectura-general)
5. [Pantallas o vistas principales](#5-pantallas-o-vistas-principales)
6. [Flujos de uso](#6-flujos-de-uso)
7. [Reglas de negocio](#7-reglas-de-negocio)
8. [Autenticación y permisos](#8-autenticación-y-permisos)
9. [Módulos o componentes principales](#9-módulos-o-componentes-principales)
10. [Integraciones externas](#10-integraciones-externas)
11. [Consideraciones de testing](#11-consideraciones-de-testing)
12. [Posibles mejoras futuras](#12-posibles-mejoras-futuras)
13. [Suposiciones explícitas](#13-suposiciones-explícitas)
14. [Referencias](#14-referencias)

---

## 1. Introducción

Este documento ofrece una visión general de **MeerKatters** a partir del análisis del código fuente, la configuración y la documentación disponible en el repositorio. Su objetivo es facilitar el onboarding de nuevos desarrolladores y servir como resumen funcional y técnico del estado actual del proyecto.

**MeerKatters** es una plataforma web orientada a comunidades de estudio y apoyo académico. La aplicación permite que estudiantes y tutores interactúen en un mismo ecosistema para crear grupos por asignatura o institución, organizar eventos, compartir recursos, comunicarse en tiempo real, localizar espacios de estudio y gestionar pagos relacionados con planes y servicios docentes.

La solución está implementada como una **SPA en React** que consume una **API REST en Spring Boot**, organizada por dominios funcionales como autenticación, comunidades, eventos, chats, tutores, suscripciones, notificaciones, formularios, ubicaciones, recomendaciones e integraciones con terceros.

---

## 2. Caso de uso y problema que resuelve

### 2.1 Problema que resuelve

El proyecto aborda la fragmentación habitual del trabajo colaborativo entre estudiantes. En lugar de depender de múltiples herramientas separadas para mensajería, coordinación, calendario, pagos, videollamadas y almacenamiento de recursos, MeerKatters unifica todo ese flujo en una sola plataforma.

La aplicación centraliza:

- La creación de comunidades de estudio.
- La gestión de miembros, anuncios y recursos.
- La organización de sesiones presenciales o virtuales.
- El acceso a tutores verificados.
- La coordinación mediante chat, notificaciones y calendarios.

### 2.2 Caso de uso principal

Un estudiante entra en la plataforma, encuentra o crea una comunidad de su asignatura, se coordina con otros usuarios, organiza eventos, asiste a reuniones virtuales o presenciales, comparte materiales y, si lo necesita, contrata refuerzo académico con un tutor dentro del mismo flujo de producto.

---

## 3. Tipos de usuario

Del código se deducen al menos estos perfiles funcionales:

- **Visitante:** puede navegar por la landing, ver comunidades públicas, consultar eventos y acceder a información legal.
- **Usuario autenticado:** puede gestionar perfil, unirse a comunidades, chatear, ver notificaciones y participar en eventos.
- **Administrador de comunidad:** crea comunidades, gestiona privacidad, categorías, miembros, solicitudes, anuncios e integraciones de su comunidad.
- **Miembro de comunidad:** participa en recursos, eventos, chats y contenidos según permisos.
- **Tutor:** crea perfil docente, define especialidades y disponibilidad, conecta Classroom o Calendar, gestiona contrataciones y consulta ganancias.
- **Tutor verificado:** aparece destacado en listados y puede operar dentro de los flujos de contratación de pago.
- **Institución:** el código sugiere soporte para planes institucionales y entidades académicas, aunque no se aprecia un panel diferenciado completo en el frontend analizado.

---

## 4. Arquitectura general

### 4.1 Frontend

El frontend está construido con **React 19** y **React Router**, y se organiza principalmente en:

- `src/screens` para pantallas por funcionalidad.
- `src/components` para componentes reutilizables.
- `src/api` para el acceso a endpoints.
- `src/contexts` para estado global de autenticación, sockets y notificaciones.
- `src/hooks` para lógica reutilizable.
- `src/utils` para utilidades compartidas.

También integra:

- **Stripe Elements** para pagos.
- **Leaflet / React Leaflet** para mapas.
- **SockJS / STOMP / Socket.IO Client** para comunicación en tiempo real.
- **Recharts** para visualizaciones.

### 4.2 Backend

El backend está construido con **Java 21** y **Spring Boot 4**, siguiendo una arquitectura por capas:

- `controller` para endpoints REST.
- `service` para reglas de negocio.
- `repository` para persistencia con Spring Data JPA.
- `entity` para entidades JPA.
- `dto` para contratos de entrada y salida.
- `security` para JWT y filtros de autenticación.
- `scheduler` para tareas programadas.
- `config` para configuración técnica y funcional.

Además, el backend expone **Swagger UI / OpenAPI**, soporta **WebSockets** y contiene componentes para inicialización de datos, correcciones de esquema y procesos programados.

### 4.3 Persistencia

La persistencia principal se apoya en **PostgreSQL**, mientras que **H2** aparece como soporte para determinados perfiles locales o de prueba. También se observan scripts y recursos SQL como:

- `data.sql`
- `fix-nulls.sql`

### 4.4 Dominios funcionales detectados

Los principales dominios presentes en el backend son:

- `users`
- `communities`
- `events`
- `chats`
- `tutors`
- `subscriptions`
- `notifications`
- `forms`
- `google`
- `maps`
- `recommendations`
- `zoom`

---

## 5. Pantallas o vistas principales

### 5.1 Landing

Presenta la propuesta de valor, beneficios, funcionalidades clave, planes y accesos a registro o login. El código la posiciona como puerta de entrada para usuarios no autenticados.

### 5.2 Home

Pantalla principal tras autenticación. Resume comunidades creadas por el usuario y comunidades de las que forma parte, además de accesos rápidos a eventos, reservas y otras áreas relevantes.

### 5.3 Comunidades

Incluye listado, creación y detalle. Desde el detalle se deduce soporte para:

- membresía,
- anuncios,
- edición,
- apuntes o recursos,
- roles y administración.

### 5.4 Eventos

Permite crear eventos, ver detalle, gestionar borradores, asistir, visualizar eventos en mapa y conectar flujos virtuales.

### 5.5 Chats

Hay soporte para chat privado y chat de comunidad. El backend incluye websockets y controladores específicos para mensajes, confirmación de lectura y previsualización de enlaces.

### 5.6 Tutores

Incluye directorio de profesores verificados, perfil público, creación y edición de perfil docente, disponibilidad, contratación, pasarela de pago, solicitudes y ganancias.

### 5.7 Planes y pagos

Contempla planes individuales e institucionales, flujos de checkout y pantallas de confirmación.

### 5.8 Perfil y ajustes

El usuario puede editar datos, gestionar privacidad, conectar Google Calendar y ajustar preferencias como 2FA o notificaciones.

### 5.9 Notificaciones

Existe una pantalla específica y un contexto dedicado. También aparecen banners y lógica de alertas, lo que sugiere una capa transversal de avisos de producto.

### 5.10 Cuestionarios

El frontend tiene editor, vista previa, resolución, resultado y listado público, por lo que el módulo parece orientado a crear y consumir cuestionarios de estudio.

---

## 6. Flujos de uso

### 6.1 Alta y acceso

1. El usuario se registra con email y contraseña o inicia sesión con Google.
2. Recibe verificación por email.
3. Si tiene 2FA activado, completa el desafío TOTP.
4. Accede a la home autenticada.

### 6.2 Creación de comunidad

1. El usuario autenticado entra en la pantalla de creación.
2. Define nombre, descripción, tipo y configuración.
3. El backend lo asigna como administrador.
4. Desde ese momento puede gestionar miembros, categorías, privacidad y eventos.

### 6.3 Acceso a comunidad privada

1. El usuario descubre una comunidad privada.
2. Envía una solicitud de acceso.
3. El administrador la aprueba o rechaza.
4. Si se aprueba, el usuario adquiere membresía en esa comunidad.

### 6.4 Organización de evento

1. Un administrador o perfil con permisos crea un evento.
2. Decide si es virtual o presencial.
3. Puede hacerlo visible en mapa y asociar ubicación.
4. Los miembros confirman asistencia y reciben notificaciones o recordatorios.

### 6.5 Contratación de tutor

1. El usuario explora tutores verificados.
2. Consulta disponibilidad.
3. Inicia el pago desde Stripe.
4. La contratación queda registrada y puede derivar en reservas, pagos y seguimiento.

### 6.6 Comunidad con servicios externos

1. Un administrador vincula Google Classroom a la comunidad.
2. Los miembros usan la comunidad como punto de coordinación.
3. Para eventos o reuniones pueden usar Zoom.
4. El calendario y las grabaciones complementan el seguimiento académico.

---

## 7. Reglas de negocio

A partir del código analizado se deducen estas reglas relevantes:

- La autenticación principal es JWT y la aplicación es stateless.
- La verificación por email forma parte del flujo de alta.
- El mismo usuario puede tener distintos permisos según la comunidad o recurso.
- El creador de una comunidad pasa a ser su administrador.
- Determinadas acciones de comunidad requieren rol de admin.
- Crear o editar eventos depende del contexto de membresía o del rol en la comunidad.
- No se puede editar un evento que ya ha comenzado.
- No se puede cancelar un evento una vez empezado.
- La cancelación de eventos está limitada hasta 30 minutos antes del inicio.
- Las comunidades pueden ser públicas o privadas.
- Las comunidades privadas requieren solicitud y aprobación.
- Existen límites asociados al plan gratuito y capacidades ampliadas en Premium.
- Un usuario con plan institucional activo no puede contratar al mismo tiempo un plan individual.
- Un tutor no debería solicitar verificación si ya está verificado o tiene una solicitud pendiente.
- La disponibilidad de tutores cruza reservas internas y franjas ocupadas en Google Calendar.
- Las reuniones Zoom de comunidad solo son accesibles para miembros.
- Determinadas operaciones quedan degradadas si faltan credenciales de Zoom, Google, Stripe o correo.

---

## 8. Autenticación y permisos

### 8.1 Autenticación

El sistema incluye:

- Login con email y contraseña.
- Verificación de email.
- Recuperación y reseteo de contraseña.
- Login y vinculación con Google.
- Soporte para 2FA mediante TOTP.

### 8.2 Autorización

La autorización es contextual. El backend no se apoya únicamente en un rol global estático, sino en pertenencia y rol sobre recursos concretos:

- miembro de comunidad,
- admin de comunidad,
- creador de evento,
- tutor,
- tutor verificado.

El `SecurityConfig` deja públicas rutas de autenticación, Swagger, algunos recursos estáticos, determinadas lecturas públicas y callbacks OAuth, mientras protege el resto con JWT.

---

## 9. Módulos o componentes principales

### 9.1 Módulo de usuarios

Gestiona registro, login, perfil, contraseña, privacidad, preferencias y autenticación reforzada.

### 9.2 Módulo de comunidades

Es el núcleo del producto. Incluye listado, detalle, categorías, miembros, solicitudes, anuncios y configuración.

### 9.3 Módulo de eventos

Gestiona creación, edición, cancelación, asistencia, visibilidad en mapa y posibles relaciones con Classroom o Zoom.

### 9.4 Módulo de chat

Permite comunicación privada y de comunidad, lectura de mensajes y vista previa de enlaces.

### 9.5 Módulo de tutores

Gestiona perfil docente, verificación, especialidades, contratación, disponibilidad, Stripe Connect y ganancias.

### 9.6 Módulo de suscripciones y pagos

Controla planes, checkouts, confirmaciones de sesión o pago y activación posterior de beneficios.

### 9.7 Módulo de notificaciones

Agrupa alertas, panel de notificaciones, preferencias y recordatorios.

### 9.8 Módulo de formularios y cuestionarios

Orientado a creación y resolución de cuestionarios dentro del ecosistema académico.

### 9.9 Módulo de ubicaciones y mapas

Permite crear ubicaciones y visualizar eventos o recomendaciones espaciales.

### 9.10 Módulo de recomendaciones

Por nombres de paquetes y DTOs, parece orientado a feedback, valoraciones y sugerencias de contenido o tutores.

### 9.11 Módulo de Zoom

Gestiona reuniones, participantes, grabaciones y descargas ligadas a comunidades o eventos.

---

## 10. Integraciones externas

Las integraciones externas detectadas son:

- **Stripe Checkout y Stripe Elements** para pagos.
- **Stripe Connect** para onboarding y cobro de tutores.
- **Google OAuth**.
- **Google Calendar** para disponibilidad y sincronización.
- **Google Classroom** para vincular cursos y tareas.
- **Zoom Server-to-Server OAuth** y webhooks.
- **SendGrid o SMTP** para emails.
- **Servicios cartográficos y de recomendación de ubicaciones**.
- **Supabase** opcional para almacenamiento de grabaciones.

---

## 11. Consideraciones de testing

El repositorio contiene una cantidad significativa de tests en ambas capas:

- Backend con tests de controladores, servicios, repositorios, seguridad, schedulers e integraciones concretas.
- Frontend con tests de APIs, contextos, hooks, componentes y pantallas.

Esto sugiere una estrategia centrada en validar:

- contratos de API,
- reglas de negocio,
- control de acceso,
- flujos de pago,
- comportamiento de UI por pantalla.

También hay configuración explícita de calidad:

- Checkstyle
- Spotless
- JaCoCo
- ESLint

---

## 12. Posibles mejoras futuras

- Añadir un documento de arquitectura técnica de despliegue separado del overview funcional.
- Centralizar la documentación viva de variables de entorno e integraciones para evitar duplicidad entre `.env.example`, scripts y perfiles Spring.
- Incorporar diagramas de secuencia para pagos, login con Google y contratación de tutores.
- Documentar mejor los estados de comunidad, evento, suscripción y contratación.
- Añadir una guía de onboarding para desarrolladores con comandos estándar de arranque, seeds y troubleshooting.
- Unificar la documentación histórica de `docs/` con una capa más actual y orientada a mantenimiento.
- Separar explícitamente qué funcionalidades están completamente productivas y cuáles están en evolución o con degradación parcial en local.

---

## 13. Suposiciones explícitas

- Se asume que la aplicación está orientada principalmente a entorno universitario, por referencias directas a universidad, asignaturas y profesorado.
- Se asume que el flujo principal de consumo es web, porque el repositorio inspeccionado contiene una SPA React y no un cliente móvil nativo.
- Se asume que el módulo de recomendaciones ya existe a nivel backend, aunque su comportamiento funcional exacto no se desprende por completo solo de los archivos revisados.
- Se asume que el soporte institucional está parcialmente implementado, ya que aparece en controladores, DTOs y pantallas de planes, pero no se aprecia un backoffice institucional completo en la parte revisada.

---

## 14. Referencias

- [README principal](../README.md)
- [Documentación API REST](./api/DOCUMENTACION_API.md)
- [Guía Backend](./api/GUIA_BACKEND.md)
- [Guía Frontend](./api/GUIA_FRONTEND.md)
- [OpenAPI](./api/openapi.yaml)
- [Diagramas](./diagramas/)
