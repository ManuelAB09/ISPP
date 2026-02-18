# Elicitación de Requisitos

### Grupo 9 - Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Plan de proyecto  
**Sprint:** Sprint DP  
**Semana:** Semana 1  
**Estado:** Aprobado  
**Fecha:** 08/02/2026  
**Autor(es):** Manuel Artero Bellido, Manuel María Calderón Rodríguez

---

## Índice
1. [Introducción](#1-introduccion)  
2. [Requisitos Funcionales](#2-requisitos-funcionales)  
  2.1. [Gestión de Usuarios](#21-gestion-de-usuarios)  
  2.2. [Gestión de Comunidades](#22-gestion-de-comunidades)  
  2.3. [Gestión de Eventos/Quedadas](#23-gestion-de-eventosquedadas)  
  2.4. [Gestión de Contenido / Integración Classroom](#24-gestion-de-contenido--integracion-classroom)  
  2.5. [Suscripciones y Pagos](#25-suscripciones-y-pagos)  
  2.6. [Ajustes y Preferencias](#26-ajustes-y-preferencias)  
3. [Requisitos No Funcionales](#3-requisitos-no-funcionales)  
  3.1. [Rendimiento](#31-rendimiento)  
  3.2. [Seguridad](#32-seguridad)  
  3.3. [Usabilidad](#33-usabilidad)  
  3.4. [Calidad de Código](#34-calidad-de-codigo)  
  3.5. [Infraestructura](#35-infraestructura)  
  3.6. [Documentación](#36-documentacion)  
4. [Requisitos de Información](#4-requisitos-de-informacion)  
5. [Reglas de Negocio](#5-reglas-de-negocio)  
6. [Actores del Sistema](#6-actores-del-sistema)  

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
| **RF-07** | Inicio de sesión con Google (Classroom) | El sistema debe permitir OAuth con Google y solicitar permisos necesarios para integrar Google Classroom | MVP |
| **RF-08** | Autenticación de doble factor | El sistema permite activar 2FA opcional | Extra |
| **RF-09** | Cambiar contraseña | Los usuarios pueden cambiar su contraseña | MVP |
| **RF-10** | Eliminar cuenta | Los usuarios pueden eliminar su cuenta | MVP |
| **RF-11** | Perfil de profesor | Un usuario puede crear/editar un perfil de profesor con especialidades, tarifas y disponibilidad | MVP |
| **RF-12** | Verificación de profesor (pago) | Los profesores pueden solicitar verificación mediante pago para aparecer destacados y con marca de "verificado" | MVP |
| **RF-13** | Listado de profesores y contacto | La aplicación mostrará un listado de profesores verificados y permitirá contacto directo desde la plataforma | MVP |
| **RF-14** | Contratación de profesor | Los grupos pueden contratar a un profesor mediante la plataforma y gestionar pagos asociados | MVP |
| **RF-15** | Integración Classroom por profesor | El profesor puede conectar su Google Classroom para que, al ser contratado, su Classroom se asocie al grupo y otorgue permisos automáticos | MVP |

### 2.2 Gestión de Comunidades

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-16** | Crear comunidad | Cualquier usuario puede crear una comunidad de estudio | MVP |
| **RF-17** | Configurar privacidad | El creador decide si la comunidad es pública o privada | MVP |
| **RF-18** | Límite de comunidades (Free) | Usuarios gratuitos pueden crear máximo 3 comunidades | MVP |
| **RF-19** | Límite de miembros (Free) | Comunidades gratuitas tienen un límite de plazas; los grupos privados tendrán una limitación reducida y las comunidades públicas un aforo alto | MVP |
| **RF-20** | Unirse a comunidad pública | Los usuarios pueden unirse a comunidades públicas | MVP |
| **RF-21** | Solicitar acceso a comunidad privada | Los usuarios pueden solicitar acceso a comunidades privadas | MVP |
| **RF-22** | Buscar comunidades | El sistema permite buscar comunidades públicas por nombre/temática/localización | MVP |
| **RF-23** | Explorar comunidades | Existe una sección para explorar comunidades públicas | MVP |
| **RF-24** | Rol de administrador | El creador de la comunidad es automáticamente administrador | MVP |
| **RF-25** | Aceptar/rechazar solicitudes | El admin puede aceptar o rechazar solicitudes de acceso | MVP |
| **RF-26** | Expulsar miembros | El admin puede expulsar miembros de la comunidad | MVP |
| **RF-27** | Abandonar comunidad | Los usuarios pueden salir de una comunidad voluntariamente | MVP |
| **RF-28** | Transferir administración | Si el admin abandona, debe elegir un sucesor | MVP |
| **RF-29** | Eliminar comunidad | El admin puede eliminar la comunidad completa | MVP |
| **RF-30** | Definir categorías | El admin puede definir categorías para organizar contenido | MVP |
| **RF-31** | Múltiples administradores | Permitir varios admins por comunidad | Extra |
| **RF-32** | Comunidades premium (más miembros) | Los grupos pueden convertirse a premium para ampliar aforo y obtener funciones adicionales (pago/suscripción) | MVP |
| **RF-33** | Comunidades corporativas ilimitadas | Academias pueden crear comunidades sin límite de miembros | Extra |
| **RF-34** | Chat de comunidad | Chat en tiempo real dentro de cada comunidad | Extra |

### 2.3 Gestión de Eventos/Quedadas

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-35** | Crear evento | Los miembros de una comunidad pueden crear eventos de estudio | MVP |
| **RF-36** | Configurar privacidad de evento | Los eventos pueden ser públicos o privados (independiente de la comunidad) | MVP |
| **RF-37** | Información de evento | Eventos incluyen: título, descripción, fecha/hora, ubicación, aforo | MVP |
| **RF-38** | Qué llevar | Se puede especificar qué materiales llevar al evento | MVP |
| **RF-39** | Integración Google Maps | Selección de ubicación mediante mapa interactivo | MVP |
| **RF-40** | Ubicaciones recomendadas | Mostrar lugares públicos recomendados (bibliotecas, etc.) | MVP |
| **RF-41** | Unirse a evento | Los usuarios pueden confirmar asistencia a eventos | MVP |
| **RF-42** | Cancelar asistencia | Los usuarios pueden cancelar su asistencia | MVP |
| **RF-43** | Ver asistentes | Se puede ver la lista de asistentes confirmados | MVP |
| **RF-44** | Editar evento | El creador puede modificar los detalles del evento | MVP |
| **RF-45** | Cancelar evento | El creador puede cancelar un evento | MVP |
| **RF-46** | Límite de aforo | Los eventos respetan el aforo máximo establecido | MVP |
| **RF-47** | Visibilidad en mapa | Los organizadores pueden marcar si el meeting será visible en el mapa; en comunidades públicas los meetings serán siempre visibles | MVP |
| **RF-48** | Reuniones virtuales | Integrar videollamadas dentro de eventos | Extra |
| **RF-49** | Grabación de reuniones | Guardar grabaciones de sesiones de estudio para subirlo a Classroom| Extra |

### 2.4 Gestión de Contenido / Integración Classroom

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-50** | Integración con Google Classroom | La gestión principal de material y tareas se delega a Google Classroom mediante integración; las asignaciones y recursos se sincronizan cuando exista Classroom asociado | MVP |
| **RF-51** | Adjuntos mínimos | Se permite adjuntar archivos limitados (por ejemplo, para eventos o mensajes), pero no se mantiene un repositorio de contenidos propio completo | MVP |
| **RF-52** | Enlaces a recursos externos | Permitir enlazar a recursos almacenados en Classroom o en URLs externas (YouTube, Drive) | MVP |
| **RF-53** | Gestión de permisos de contenido | Cuando un profesor asocia su Classroom, la plataforma gestionará permisos mínimos necesarios para alumnos y profesores del grupo | MVP |


### 2.5 Suscripciones y Pagos

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-54** | Visualizar planes | Los usuarios pueden ver los diferentes planes disponibles | MVP |
| **RF-55** | Suscribirse a plan premium | Proceso de pago para plan premium individual y para convertir grupos a premium | MVP |
| **RF-56** | Integración de pagos | Integración con pasarela de pagos (Stripe o similar) | MVP |
| **RF-57** | Cancelar suscripción | Los usuarios pueden cancelar su suscripción | MVP |
| **RF-58** | Plan corporativo | Instituciones pueden contratar plan para sus miembros (oferta especial para academias) | MVP |
| **RF-59** | Invitación corporativa | Instituciones invitan usuarios por correo electrónico | Extra |
| **RF-60** | Beneficios corporativos | Usuarios de comunidades corporativas tienen acceso premium dentro de ellas | Extra |
| **RF-61** | Pago de verificación de profesores | Flujo de pago para que un profesor pueda abonar la verificación/promoción | MVP |
| **RF-62** | Gestión de pagos a profesores y comisiones | Soporte para pagar a profesores, aplicar una comisión por la plataforma y registrar transacciones | MVP |
| **RF-63** | Planes para academias | Oferta de planes reducidos para academias/instituciones con funcionalidades premium | MVP |



### 2.6 Ajustes y Preferencias

| ID | Requisito | Descripción | Tipo |
|----|-----------|-------------|-----|
| **RF-64** | Panel de ajustes | Sección de configuración de la cuenta | MVP |
| **RF-65** | Configurar notificaciones | Preferencias de notificaciones por email | Extra |
| **RF-66** | Notificaciones por email | Envío de emails para eventos importantes | Extra |
| **RF-67** | Preferencias de visibilidad | Permitir al usuario configurar si su perfil aparece en listados públicos (profesores/miembros) | MVP |

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
| **RNF-21** | Almacenamiento de archivos | Integración con Google Classroom para la gestión principal de contenidos; para adjuntos menores usar almacenamiento PaaS o MinIO según proveedor elegido | MVP |

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
| **RI-06** | ArchivoAdjunto | id, nombre, url, tipo, tamaño, asociado_a (evento|grupo), usuario_id, classroom_link (opcional), created_at | MVP |
| **RI-07** | Categoria | id, nombre, comunidad_id | MVP |
| **RI-08** | Suscripcion | id, plan, fecha_inicio, fecha_fin, activa | MVP |
| **RI-09** | SolicitudComunidad | id, usuario_id, comunidad_id, estado, created_at | MVP |
| **RI-10** | Profesor | id, usuario_id, especialidades, tarifa_hora, verificado, classroom_id, bio, disponibilidad | MVP |
| **RI-11** | ClassroomAssociation | id, grupo_id, classroom_id, propietario_profesor_id, permisos, linked_at | MVP |
| **RI-12** | TransaccionPago | id, tipo(pago_verificacion|pago_profesor|suscripcion), monto, moneda, comision, estado, iniciado_at, completado_at | MVP |
| **RI-13** | EventoMetadatos | id, evento_id, visible_en_mapa (boolean), virtual (boolean) | MVP |

---

## 5. Reglas de Negocio

| ID | Regla | Tipo |
|----|-------|-----|
| **RN-01** | Un usuario gratuito puede crear máximo 3 comunidades | MVP |
| **RN-02** | Límite de miembros según tipo | Comunidades públicas admiten un aforo alto; grupos privados tienen una limitación reducida de plazas | MVP |
| **RN-03** | Límite de comunidades por usuario (free) | Un usuario puede estar en máximo 3 comunidades como miembro en plan gratuito | MVP |
| **RN-04** | Eventos respetan el aforo | Los eventos respetan el aforo máximo definido | MVP |
| **RN-05** | Transferencia de administración | Si el único admin abandona la comunidad, debe transferir el rol | MVP |
| **RN-06** | Regla de visibilidad de meetings | En comunidades públicas los meetings son siempre visibles en el mapa; en grupos privados los organizadores pueden ocultarlos | MVP |
| **RN-07** | Comisión sobre pagos a profesores | La plataforma aplicará una comisión sobre los pagos realizados a profesores (configurable) | MVP |
| **RN-08** | Verificación de profesor | Sólo los profesores que abonen la verificación aparecerán destacados como "verificados" en listados | MVP |
| **RN-09** | Planes premium para ampliar aforo | Los grupos premium permiten ampliar aforo y otras funcionalidades según plan contratado | MVP |

---

## 6. Actores del Sistema

| Actor | Descripción |
|-------|-------------|
| **Usuario no autenticado** | Visitante que puede ver información pública y registrarse |
| **Usuario autenticado (Free)** | Usuario registrado con funcionalidades limitadas |
| **Usuario Premium** | Usuario con suscripción que tiene acceso completo |
| **Administrador de comunidad** | Usuario que gestiona una comunidad |
| **Profesor** | Usuario que ofrece servicios docentes: tiene perfil, puede verificarse y conectar Classroom |
| **Sistema** | Procesos automáticos (notificaciones, limpieza, pagos, sincronizaciones con Classroom) |

---