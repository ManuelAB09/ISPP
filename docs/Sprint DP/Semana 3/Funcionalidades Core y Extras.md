# Lista de Funcionalidades con Prioridad

### Grupo D – Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Desarrollo  
**Sprint:** Sprint 0  
**Semana:** Semana 3  
**Estado:** Aprobado  
**Fecha:** 16/02/2026  
**Autor(es):** Squad de Backend

---

## Índice

1. [MVP](#1-mvp)  
1.1. [Autenticación y Gestión de Cuenta](#11-autenticación-y-gestión-de-cuenta)  
1.2. [Perfil de Profesor y Verificación](#12-perfil-de-profesor-y-verificación)  
1.3. [Comunidades](#13-comunidades)  
1.4. [Chat y Comunicación](#14-chat-y-comunicación)  
1.5. [Eventos de Estudio](#15-eventos-de-estudio)  
1.6. [Ubicación y Mapas](#16-ubicación-y-mapas)  
1.7. [Integración con Google Classroom](#17-integración-con-google-classroom)  
1.8. [Contratación de Profesores y Pagos](#18-contratación-de-profesores-y-pagos)  
1.9. [Monetización y Planes](#19-monetización-y-planes)  
2. [Funcionalidades Extra](#2-funcionalidades-extra)  
2.1. [Sistema de Puntuación y Ranking](#21-sistema-de-puntuación-y-ranking)  
2.2. [Gestión de Contenidos y Cuestionarios](#22-gestión-de-contenidos-y-cuestionarios)  
2.3. [Videoconferencias y Aulas Virtuales](#23-videoconferencias-y-aulas-virtuales)  
2.4. [Eventos y Calendarios](#24-eventos-y-calendarios)  
2.5. [Mapas y Ubicación](#25-mapas-y-ubicación)  
2.6. [Comunidades](#26-comunidades)  
2.7. [Notificaciones](#27-notificaciones)  
2.8. [Autenticación y Seguridad](#28-autenticación-y-seguridad)  
2.9. [Acceso Premium y Roles](#29-acceso-premium-y-roles)  
2.10. [Funcionalidades Adicionales](#210-funcionalidades-adicionales)  

---

## 1. MVP

### 1.1 Autenticación y Gestión de Cuenta

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Registro con email y contraseña | Permite a los usuarios crear una cuenta mediante email y contraseña. | Must Have |
| Inicio y cierre de sesión | Autenticación segura y posibilidad de cerrar sesión en cualquier momento. | Must Have |
| Cambio de contraseña | El usuario puede modificar su contraseña desde la configuración. | Must Have |
| Eliminación de cuenta | Permite eliminar permanentemente la cuenta del usuario. | Must Have |
| Edición de perfil | Los usuarios pueden modificar su información personal (foto, bio, datos básicos). | Must Have |
| Ver perfiles de otros usuarios | Permite visualizar perfiles públicos de otros miembros. | Must Have |
| Configuración de cuenta | Sección centralizada para ajustes de seguridad, notificaciones y privacidad. | Should Have |
| Control de visibilidad del perfil | Permite decidir si el perfil aparece en listados públicos. | Should Have |

---

### 1.2 Perfil de Profesor y Verificación

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Crear/editar perfil de profesor | Permite añadir especialidades, tarifas y disponibilidad. | Must Have |
| Solicitud de verificación de profesor | Los profesores pueden pagar para aparecer destacados con insignia "Verificado". | Must Have |
| Listado de profesores verificados | Sección destacada con profesores verificados y acceso a contacto directo. | Should Have |
| Pago para verificación/promoción | Flujo de pago para destacar el perfil profesional. | Must Have |

---

### 1.3 Comunidades

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Crear comunidad de estudio | Cualquier usuario puede crear una comunidad. | Must Have |
| Comunidad pública o privada | El creador decide el tipo de acceso. | Must Have |
| Límite de comunidades gratuitas (máx. 3) | Restricción para usuarios free. | Must Have |
| Límites de aforo según tipo y plan | Comunidades gratuitas tienen límite; premium amplían capacidad. | Must Have |
| Explorar comunidades públicas | Sección para descubrir comunidades abiertas. | Should Have |
| Solicitud de acceso a comunidades privadas | Los usuarios pueden pedir acceso. | Must Have |
| Unirse a comunidades públicas | Acceso directo sin aprobación previa. | Must Have |
| Gestión de solicitudes | El admin puede aceptar o rechazar solicitudes. | Must Have |
| Expulsar miembros | El administrador puede eliminar usuarios. | Must Have |
| Salir voluntariamente de comunidad | Cualquier miembro puede abandonar el grupo. | Must Have |
| Transferencia de administración | Si el admin abandona, debe designar sucesor. | Should Have |
| Eliminar comunidad | El administrador puede borrar completamente la comunidad. | Should Have |
| Definir categorías internas | Organización del contenido dentro de la comunidad. | Should Have |
| Conversión a comunidad premium | Permite ampliar aforo y desbloquear funciones adicionales. | Should Have |

---

### 1.4 Chat y Comunicación

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Chat en tiempo real | Mensajería instantánea dentro de cada comunidad. | Must Have |
| Adjuntar archivos limitados | Permite compartir archivos en eventos o mensajes (sin repositorio propio completo). | Should Have |
| Enlazar recursos externos | Permite compartir enlaces de Classroom, Drive, YouTube u otras URLs. | Must Have |

---

### 1.5 Eventos de Estudio

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Crear eventos de estudio | Los miembros pueden organizar eventos dentro de la comunidad. | Must Have |
| Eventos públicos o privados | Independiente de la visibilidad de la comunidad. | Must Have |
| Información completa del evento | Incluye título, descripción, fecha, hora, ubicación y aforo. | Should Have |
| Especificar materiales necesarios | Posibilidad de indicar qué llevar al evento. | Should Have |
| Confirmar asistencia | Los usuarios pueden apuntarse al evento. | Must Have |
| Cancelar asistencia | Posibilidad de desconfirmar asistencia. | Must Have |
| Lista de asistentes | Visualización de participantes confirmados. | Must Have |
| Modificar evento | El creador puede editar detalles. | Must Have |
| Cancelar evento | El organizador puede eliminar el evento. | Must Have |
| Control automático de aforo | El sistema bloquea inscripciones al alcanzar el límite. | Must Have |
| Visibilidad en mapa | Los organizadores pueden marcar si el evento aparece en el mapa (en comunidades públicas siempre visible). | Must Have |

---

### 1.6 Ubicación y Mapas

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Selección de ubicación con mapa interactivo | Permite fijar el lugar del evento visualmente. | Must Have |
| Mostrar lugares públicos recomendados | Sugerencias como bibliotecas o espacios de estudio. | Should Have  |
| Visualización de meetings en mapa | Eventos visibles geográficamente según configuración. | Must Have |

---

### 1.7 Integración con Google Classroom

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Integración con Google Classroom | Sincronización con clases y gestión académica externa. | Should Have |
| Asociación automática al contratar profesor | Al contratar, se conecta el Classroom del profesor al grupo. | Could Have |
| Gestión automática de permisos | Asignación mínima necesaria para alumnos y profesores. | Should Have |
| Sincronización de tareas y recursos | Asignaciones y materiales se sincronizan desde Classroom. | Should Have |

---

### 1.8 Contratación de Profesores y Pagos

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Contratar profesor desde la plataforma | Grupos pueden contratar profesores directamente. | Must have |
| Gestión de pagos asociados | Control del pago dentro de la plataforma. | Must Have |
| Pago a profesores con comisión | La plataforma aplica comisión y registra transacciones. | Must Have |
| Integración con pasarela de pago (Stripe o similar) | Procesamiento seguro de pagos online. | Should Have |
| Proceso de pago para plan premium individual | Permite suscripción premium para usuarios. | Must Have |
| Proceso de pago para convertir grupo a premium | Upgrade de comunidad mediante suscripción. | Should Have |
| Cancelación de suscripción | El usuario puede cancelar su plan en cualquier momento. | Must Have |
| Planes institucionales para academias | Oferta especial para instituciones educativas. | Must Have |
| Planes reducidos para academias | Versiones premium con condiciones especiales. | Should Have |

---

### 1.9 Monetización y Planes

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Visualización de planes disponibles | Página informativa con comparación de planes. | Must Have |
| Upgrade a premium | Permite desbloquear funciones avanzadas. | Must Have |



## 2. Funcionalidades Extra

### 2.1 Sistema de Puntuación y Ranking

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Sistema de puntuación de profesores | Los alumnos pueden valorar a los profesores tras cada clase o tutoría. Las valoraciones generan niveles de desempeño (principiante, avanzado, experto). | Must Have |
| Ranking de usuarios | Cada usuario tiene un ranking dentro de su comunidad según participación en chats, cuestionarios y eventos. | Must Have |
| Ranking de cuestionarios | Los resultados de los cuestionarios pueden ser utilizados para clasificar a los participantes. | Should Have |

### 2.2 Gestión de Contenidos y Cuestionarios

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Creación de cuestionarios con IA | Generación automática de cuestionarios por tema usando IA. | Could Have |
| Creación de cuestionarios de forma manual | Generación automática de cuestionarios por tema de forma manual ya sea por un alumno o por el profesor. | Should Have |
| Grabación de sesiones | Guardado de sesiones de estudio para revisión o subida a Google Classroom. | Should Have |
| Seguimiento de progreso y feedback | Visualización del avance de cada alumno y retroalimentación personalizada. | Could Have |
| Perfil del profesor con calendario | Visualización de disponibilidad y clases reservadas. | Must Have |
| Reserva de clases | Los alumnos pueden reservar clases directamente desde la plataforma. | Must Have |
| Mensajería con profesores | Comunicación directa para confirmar reservas o resolver dudas. | Must Have |
| Chatbot de búsqueda de profesores | Asistente inteligente para encontrar profesores según tema. | Could have |
| Tutorías programadas | Organización de sesiones individuales o grupales con profesores. | Should Have |

### 2.3 Videoconferencias y Aulas Virtuales

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Google Meet integrado o herramienta similar | Videollamadas de hasta 60 minutos dentro de la plataforma. | Could Have |
| Grabación de sesiones | Guardado de videollamadas para revisión o subida a Classroom. | Could Have |

### 2.4 Eventos y Calendarios

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Listado de eventos con alertas | Visualización de reuniones, exámenes, cuestionarios o tutorías con alertas automáticas. | Should Have |
| Google Calendar | Sincronización de eventos y notificaciones con Google Calendar. | Could Have |
| Recordatorios por email | Alertas automáticas de eventos importantes. | Should Have |
| Alarmas personalizables | Configuración de alarmas para eventos específicos. | Should Have |

### 2.5 Mapas y Ubicación

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Búsqueda por ubicación | Localización de profesores cercanos geográficamente. | Must Have |
| Mapa de meetings | Visualización de reuniones y eventos en Google Maps. | Must Have |

### 2.6 Comunidades

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Creación de comunidades ilimitadas | Academias o instituciones pueden crear comunidades con cualquier número de miembros. | Must have |
| Múltiples administradores | Permite que varios usuarios gestionen la comunidad. | Must Have |
| Invitaciones por correo | Instituciones invitan alumnos y profesores mediante email. | Should Have |
| Chat en tiempo real | Comunicación instantánea dentro de la comunidad. | Must Have |
| Anuncios y avisos | Mensajes importantes separados del chat principal. | Must Have |
| Sugerencias de comunidades | Recomendación de comunidades según intereses del usuario. | Should Have |

### 2.7 Notificaciones

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Notificaciones en el chat | Alertas automáticas sobre mensajes y eventos relevantes. | Must have |
| Preferencias de notificación por email | Personalización de qué notificaciones recibir por correo. | Should Have |
| Recordatorios de eventos | Alertas por correo de reuniones, exámenes o cuestionarios. | Should Have |

### 2.8 Autenticación y Seguridad

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Inicio de sesión con Google | Registro y acceso simplificado mediante Google. | Should Have |
| Permisos para Google Classroom | Autorización para integrar recursos y cursos de Classroom. | Should Have |
| Autenticación de doble factor | Seguridad adicional con segundo nivel de verificación. | Must Have |

### 2.9 Acceso Premium y Roles

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Acceso premium para comunidades corporativas | Funcionalidades avanzadas para usuarios corporativos. | Must Have |
| Roles diferenciados | Distinción entre administradores, profesores y alumnos con permisos específicos. | Must Have |
| Múltiples administradores | Permite asignar varios responsables de la comunidad. | Must Have |

### 2.10 Funcionalidades Adicionales

| Funcionalidad | Descripción | Prioridad |
|---------------|-------------|-----------|
| Reservas directas de servicios y clases | Contratación de clases o servicios directamente desde la plataforma. | Should Have |
| Integración completa con Google Classroom | Subida de grabaciones, tareas y recursos educativos. | Could Have |
| IA para recomendaciones | Sugerencias inteligentes de contenidos, profesores y cuestionarios según actividad y preferencias. | Could Have |