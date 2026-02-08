# Elicitación de Requisitos

### Grupo D - Turno de tarde

![Logo App](./images/logoapp.jpeg)

---

**Proyecto:** VibeStudy  
**Documento:** Plan de proyecto  
**Sprint:** Sprint 0  
**Estado:** Borrador  
**Fecha:** 08/02/2026  
**Autor(es):** Manuel Artero Bellido, Manuel María Calderón Rodríguez

---

## Índice
1. [Introducción](#1-introducción)  
2. [Requisitos Funcionales](#2-requisitos-funcionales)  
  2.1 [Gestión de Usuarios](#21-gestión-de-usuarios)  
  2.2 [Gestión de Comunidades](#22-gestión-de-comunidades)  
  2.3 [Gestión de Eventos/Quedadas](#23-gestión-de-eventos-quedadas)  
  2.4 [Gestión de Contenido/Apuntes](#24-gestión-de-contenido-apuntes)  
  2.5 [Suscripciones y Pagos](#25-suscripciones-y-pagos)  
  2.6 [Publicidad](#26-publicidad)  
  2.7 [Ajustes y Preferencias](#27-ajustes-y-preferencias)  
  2.8 [Funcionalidades Premium](#28-funcionalidades-premium)  
3. [Requisitos No Funcionales](#3-requisitos-no-funcionales)  
  3.1 [Rendimiento](#31-rendimiento)  
  3.2 [Seguridad](#32-seguridad)  
  3.3 [Usabilidad](#33-usabilidad)  
  3.4 [Calidad de Código](#34-calidad-de-código)  
  3.5 [Infraestructura](#35-infraestructura)  
  3.6 [Documentación](#36-documentación)  
4. [Requisitos de Información](#4-requisitos-de-información)  
5. [Reglas de Negocio](#5-reglas-de-negocio)  
  5.1 [Plan Gratuito](#51-plan-gratuito)  
  5.2 [Plan Premium](#52-plan-premium)  
  5.3 [Plan Institucional B2B](#53-plan-institucional-b2b)  
  5.4 [Reglas Generales](#54-reglas-generales)  
6. [Actores del Sistema](#6-actores-del-sistema)  

---

## 1. Introducción

Este documento recoge los requisitos elicitados para la plataforma **VibeStudy**, una comunidad de estudiantes que facilita la organización de actividades de estudio colaborativo.

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
| **RF-10** | Recuperar contraseña | El sistema permite recuperar la contraseña mediante email | MVP |
| **RF-11** | Deteccion de dominio institucional | Si el email pertenece a una institucion contratada, activar funcionalidad Premium en las comunidades de la institución | MVP |

### 2.2 Gestión de Comunidades

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-12** | Crear comunidad | Cualquier usuario puede crear una comunidad de estudio | MVP |
| **RF-13** | Configurar privacidad | El creador decide si la comunidad es pública o privada | MVP |
| **RF-14** | Límite de comunidades (Free) | Usuarios gratuitos pueden crear y unirse a comunidades con límites establecidos | MVP |
| **RF-15** | Más comunidades (Premium) | Usuarios premium pueden crear y administrar más comunidades que usuarios gratuitos | MVP |
| **RF-16** | Unirse a comunidad pública | Los usuarios pueden unirse a comunidades públicas | MVP |
| **RF-17** | Solicitar acceso a comunidad privada | Los usuarios pueden solicitar acceso a comunidades privadas | MVP |
| **RF-18** | Buscar comunidades | El sistema permite buscar comunidades públicas por nombre/temática/localización | MVP |
| **RF-19** | Explorar comunidades | Existe una sección para explorar comunidades públicas | MVP |
| **RF-20** | Rol de administrador | El creador de la comunidad es automáticamente administrador | MVP |
| **RF-21** | Aceptar/rechazar solicitudes | El admin puede aceptar o rechazar solicitudes de acceso | MVP |
| **RF-22** | Expulsar miembros | El admin puede expulsar miembros de la comunidad | MVP |
| **RF-23** | Abandonar comunidad | Los usuarios pueden salir de una comunidad voluntariamente | MVP |
| **RF-24** | Transferir administración | Si el admin abandona, debe elegir un sucesor | MVP |
| **RF-25** | Eliminar comunidad | El admin puede eliminar la comunidad completa | MVP |
| **RF-26** | Definir categorías | El admin puede definir categorías para organizar contenido | MVP |
| **RF-27** | Chat de comunidad | Chat en tiempo real dentro de cada comunidad | MVP |
| **RF-28** | Múltiples administradores | Permitir varios admins por comunidad | Extra |
| **RF-29** | Comunidades premium (más miembros) | Usuarios premium pueden crear comunidades con más miembros | Extra |
| **RF-30** | Comunidades B2B ilimitadas | Instituciones B2B pueden crear comunidades de tamaño ilimitado para gestión de sus alumnos | Extra |

### 2.3 Gestión de Eventos/Quedadas

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-31** | Crear evento | Los miembros de una comunidad pueden crear eventos de estudio | MVP |
| **RF-32** | Configurar privacidad de evento | Los eventos pueden ser públicos (visibles para todos los miembros de la comunidad) o privados (solo accesibles para miembros invitados por el creador del evento) | MVP |
| **RF-32b** | Invitar a evento privado | El creador de un evento privado puede invitar a miembros específicos de la comunidad | MVP |
| **RF-33** | Información de evento | Eventos incluyen: título, descripción, fecha/hora, ubicación, aforo | MVP |
| **RF-34** | Qué llevar | Se puede especificar qué materiales llevar al evento | MVP |
| **RF-35** | Integración Google Maps | Selección de ubicación mediante mapa interactivo | MVP |
| **RF-36** | Ubicaciones recomendadas | Mostrar lugares públicos recomendados (bibliotecas, etc.) | MVP |
| **RF-37** | Unirse a evento | Los usuarios pueden confirmar asistencia a eventos | MVP |
| **RF-38** | Cancelar asistencia | Los usuarios pueden cancelar su asistencia | MVP |
| **RF-39** | Ver asistentes | Se puede ver la lista de asistentes confirmados | MVP |
| **RF-40** | Editar evento | El creador puede modificar los detalles del evento | MVP |
| **RF-41** | Cancelar evento | El creador puede cancelar un evento | MVP |
| **RF-42** | Límite de aforo | Los eventos respetan el aforo máximo establecido | MVP |
| **RF-43** | Reuniones virtuales | Integrar videollamadas dentro de eventos virtuales | Extra |
| **RF-44** | Grabación de reuniones | Usuarios premium pueden grabar las videollamadas de eventos virtuales | Extra |

### 2.4 Gestión de Contenido/Apuntes

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-45** | Subir archivos | Los miembros pueden subir apuntes/archivos a la comunidad | MVP |
| **RF-46** | Organizar por categorías | Los archivos se organizan según las categorías del admin | MVP |
| **RF-47** | Vincular a evento | Los archivos pueden asociarse a un evento específico | MVP |
| **RF-48** | Visualizar archivos | Los miembros pueden ver los archivos subidos | MVP |
| **RF-49** | Descargar con anuncios (Free) | Usuarios gratuitos pueden descargar apuntes pero deben ver anuncios durante el proceso | MVP |
| **RF-50** | Descargar sin anuncios (Premium) | Usuarios premium pueden descargar archivos sin publicidad | MVP |
| **RF-51** | Eliminar archivos | El autor o admin puede eliminar archivos | MVP |
| **RF-52** | Resumen IA de apuntes | Usuarios premium pueden generar resúmenes automáticos de apuntes mediante IA | Extra |

### 2.5 Suscripciones y Pagos

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-53** | Visualizar planes | Los usuarios pueden ver los tres planes disponibles (Gratuito, Premium, Institucional B2B) | MVP |
| **RF-54** | Suscribirse a plan premium | Proceso de pago para plan premium individual | MVP |
| **RF-55** | Integración de pagos | Integración con pasarela de pagos (Stripe o similar) | MVP |
| **RF-56** | Cancelar suscripción | Los usuarios pueden cancelar su suscripción | MVP |
| **RF-57** | Historial de compras | Los usuarios pueden ver su historial de compras y transacciones | MVP |
| **RF-58** | Plan institucional B2B | Instituciones pueden contratar plan B2B con comunidades ilimitadas y todos sus alumnos con premium | Extra |
| **RF-59** | Registro de institución | Las instituciones pueden registrarse con perfil corporativo | Extra |
| **RF-60** | Invitación corporativa | Instituciones invitan usuarios por correo electrónico | Extra |
| **RF-61** | Gestión de invitaciones | Las instituciones pueden ver y gestionar invitaciones enviadas | Extra |
| **RF-62** | Premium automático B2B | Usuarios de instituciones B2B tienen automáticamente plan premium completo | Extra |
| **RF-63** | Dashboard analítico B2B | Instituciones B2B tienen acceso a dashboard analítico de uso | Extra |
| **RF-64** | Planes de estudio B2B | Instituciones B2B pueden crear y gestionar planes de estudio para sus alumnos | Extra |

### 2.6 Publicidad

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-65** | Mostrar anuncios en descargas | Usuarios gratuitos ven publicidad al descargar apuntes de reuniones | MVP |
| **RF-66** | Sin anuncios premium | Usuarios premium y B2B no ven publicidad | MVP |

### 2.7 Ajustes y Preferencias

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-67** | Panel de ajustes | Sección de configuración de la cuenta | MVP |
| **RF-68** | Configurar notificaciones | Preferencias de notificaciones por email | Extra |
| **RF-69** | Notificaciones por email | Envío de emails para eventos importantes | Extra |

### 2.8 Funcionalidades Premium

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-70** | Iconos estéticos Discord | Usuarios premium tienen acceso a iconos y estética mejorada tipo Discord | Extra |
| **RF-71** | Promoción como Profesor | Usuarios premium pueden promocionarse como profesores en la plataforma | Extra |
| **RF-72** | Chatbot de comunidad | Todos los usuarios tienen acceso a chatbot de preguntas sobre el tópico de la comunidad | MVP |
| **RF-73** | Invitar amigos a reuniones | Los usuarios pueden invitar amigos a reuniones/eventos | MVP |

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
| **RI-01** | Usuario | id, email, password, nombre, foto, bio, intereses, google_id, two_factor_enabled, created_at | MVP |
| **RI-02** | Comunidad | id, nombre, descripcion, privada, imagen, creador_id, max_miembros, tipoPlan (FREE/PREMIUM/CORPORATIVO), congelada, created_at | MVP |
| **RI-03** | MiembroComunidad | id, usuario_id, comunidad_id, rol, fecha_ingreso | MVP |
| **RI-04** | Evento | id, titulo, descripcion, fecha_hora, ubicacion, latitud, longitud, aforo, privado, que_llevar, es_virtual, enlace_virtual, comunidad_id, creador_id, created_at | MVP |
| **RI-04b** | InvitacionEvento | id, evento_id, usuario_id, estado, created_at | MVP |
| **RI-05** | AsistenciaEvento | id, evento_id, usuario_id, estado, created_at | MVP |
| **RI-06** | Archivo | id, nombre, url, tipo, tamaño, precio, descargas, comunidad_id, evento_id, usuario_id, categoria_id, created_at | MVP |
| **RI-07** | Categoria | id, nombre, descripcion, orden, comunidad_id | MVP |
| **RI-08** | Suscripcion | id, plan, fecha_inicio, fecha_fin, activa, descargas_restantes, auto_renovar, fecha_gracia_fin | MVP |
| **RI-09** | SolicitudComunidad | id, usuario_id, comunidad_id, estado, mensaje, respondido_por, created_at | MVP |
| **RI-10** | Compra | id, usuario_id, archivo_id, monto, metodo_pago, transaccion_id, estado, fecha_compra | MVP |
| **RI-11** | Mensaje | id, comunidad_id, usuario_id, contenido, editado, created_at | MVP |
| **RI-12** | Institucion | id, nombre, email, logo, dominio, plan_contratado, max_usuarios, created_at | Extra |
| **RI-13** | InvitacionCorporativa | id, institucion_id, email, token, estado, fecha_envio, fecha_expiracion | Extra |
| **RI-14** | Notificacion | id, usuario_id, tipo, titulo, mensaje, leida, enviada_por_email, created_at | Extra |
| **RI-15** | PreferenciasNotificacion | id, usuario_id, email_eventos, email_mensajes, email_archivos, email_solicitudes | Extra |
| **RI-16** | GrabacionReunion | id, evento_id, url, duracion, tamaño, created_at | Extra |

---

## 5. Reglas de Negocio

### 5.1 Plan Gratuito
| ID | Regla | Tipo |
|----|-------|-----|
| **RN-01** | Usuarios gratuitos pueden crear y unirse a comunidades con límites establecidos | MVP |
| **RN-02** | Usuarios gratuitos pueden descargar apuntes de reuniones pero deben ver anuncios | MVP |
| **RN-03** | Usuarios gratuitos pueden invitar amigos a reuniones | MVP |
| **RN-04** | Usuarios gratuitos pueden aceptar o rechazar miembros (si son admin) | MVP |
| **RN-05** | Todos los usuarios tienen acceso al chatbot de preguntas sobre el tópico de la comunidad | MVP |

### 5.2 Plan Premium
| ID | Regla | Tipo |
|----|-------|-----|
| **RN-06** | Usuarios premium descargan archivos SIN anuncios | MVP |
| **RN-07** | Usuarios premium tienen acceso a IA para resumen de apuntes | Extra |
| **RN-08** | Usuarios premium tienen acceso a iconos y estética mejorada tipo Discord | Extra |
| **RN-09** | Usuarios premium pueden crear más comunidades que usuarios gratuitos | MVP |
| **RN-10** | Usuarios premium pueden promocionarse como Profesores en la plataforma | Extra |

### 5.3 Plan Institucional B2B
| ID | Regla | Tipo |
|----|-------|-----|
| **RN-11** | Instituciones B2B tienen acceso a Dashboard Analítico de uso | Extra |
| **RN-12** | Instituciones B2B pueden crear comunidades de tamaño ilimitado para gestión de alumnos | Extra |
| **RN-13** | Instituciones B2B pueden gestionar Planes de estudio | Extra |
| **RN-14** | Todos los alumnos de instituciones B2B tienen automáticamente el plan premium | Extra |

### 5.4 Reglas Generales
| ID | Regla | Tipo |
|----|-------|-----|
| **RN-15** | Los eventos respetan el aforo máximo definido | MVP |
| **RN-16** | Si el único admin abandona la comunidad, debe transferir el rol | MVP |
| **RN-17** | Los eventos de una comunidad solo son accesibles para sus miembros. Los eventos privados además requieren invitación del creador | MVP |
| **RN-18** | Si un usuario deja de ser premium, tiene 7 días de gracia para renovar su suscripción | MVP |
| **RN-19** | Tras los 7 días sin renovar: pierde beneficios premium y vuelve a plan gratuito | MVP |

---

## 6. Actores del Sistema

| Actor | Descripción |
|-------|-------------|
| **Usuario no autenticado** | Visitante que puede ver información pública y registrarse |
| **Usuario Gratuito** | Usuario registrado con funcionalidades básicas: comunidades limitadas, descargas con anuncios, chatbot de comunidad |
| **Usuario Premium** | Usuario con suscripción: sin anuncios, IA resumen, iconos Discord, más comunidades, promoción como Profesor |
| **Usuario B2B** | Alumno de institución con plan B2B, tiene automáticamente todos los beneficios premium |
| **Administrador de comunidad** | Usuario que gestiona una comunidad |
| **Institución B2B** | Organización (universidad, centro) con dashboard analítico, comunidades ilimitadas y planes de estudio |
| **Profesor** | Usuario premium promocionado como profesor en la plataforma |
| **Sistema** | Procesos automáticos (notificaciones, IA, chatbot, etc.) |
| **Pasarela de Pago** | Sistema externo (Stripe) que procesa transacciones |

---
