# Informe de Producto: Plataforma de Comunidades de Estudio

### Grupo 9 - Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Plan de proyecto  
**Sprint:** Sprint DP
**Semana:** Semana 1    
**Estado:** Aprobado  
**Fecha:** 08/02/2026  
**Autor(es):** Manuel Artero Bellido

---

## Índice
1. [Resumen Ejecutivo](#1-resumen-ejecutivo)
2. [Alcance del MVP (Producto Mínimo Viable)](#2-alcance-del-mvp-producto-minimo-viable)
  - [En qué consiste nuestra aplicación (MVP)](#en-que-consiste-nuestra-aplicacion-mvp)
  - [Monetización (MVP)](#monetizacion-mvp)
3. [Hoja de Ruta: Puntos de Extensión (Futuro)](#3-hoja-de-ruta-puntos-de-extension-futuro)
4. [Arquitectura del sistema](#4-arquitectura-del-sistema)
  - [Estrategia: Monolito Modular](#estrategia-monolito-modular)
  - [Infraestructura](#infraestructura)

---

## 1. Resumen Ejecutivo
El objetivo es desarrollar una plataforma centrada en comunidades de aprendizaje que conecte alumnos entre sí y con profesores para organizar sesiones, gestionar material y coordinar tareas. El MVP integra Google Classroom para la gestión docente, permite la creación de grupos públicos y privados, gestión de meetings (visibles u ocultos en mapa) y funcionalidades de contratación y pago a profesores.

**Modelo de Negocio:** Híbrido y orientado a transacciones y suscripciones.
- **Profesores (B2C - sellers):** Pago por verificación/promoción para ser reconocidos oficialmente en la plataforma.
- **Alumnos/Grupos (B2C - usuarios):** Suscripción o pago único para convertir grupos a premium (mayor aforo y funciones adicionales).
- **Comisión por transacción:** Pequeña comisión sobre los pagos realizados a profesores (interés sobre la gestión de sueldos) como fuente recurrente.
- **B2B (Academias):** Venta del modelo premium a academias.

---

## 2. Alcance del MVP (Producto Mínimo Viable)

### En qué consiste nuestra aplicación (MVP)

- Los alumnos pueden crear grupos sobre temas específicos de aprendizaje.
- Estos grupos pueden o no tener profesores asociados.
- Un grupo puede tener asociado un Classroom (Google Classroom) para gestionar material y tareas.
- Los profesores pueden publicitarse en la aplicación.
- Los profesores aparecen en un listado para que los alumnos puedan contactar con ellos.
- Los grupos de alumnos pueden decidir contratar a un profesor.
- En los grupos que tengan profesor se asociará automáticamente el Classroom del profesor, otorgando los permisos necesarios.
- Alumnos y profesores usarán ese Classroom integrado para gestionar el material y las tareas.
- El sueldo del profesor se gestionará a través de la aplicación (pagos y cobros integrados).
- Se pueden crear meetings para quedar; pueden ser visibles o no en el mapa.
- Hay 2 tipos de grupos: comunidad y privado.
- Las comunidades son públicas: cualquiera puede unirse y tienen un límite alto de personas.
- En las comunidades no se pueden contratar profesores y los meetings se verán siempre en el mapa.
- Los grupos privados pueden contratar profesores, tienen limitación de plazas y en ellos los meetings pueden ocultarse del mapa.

### Monetización (MVP)

- Los profesores deberán pagar para ser reconocidos oficialmente como tales en la plataforma (pago por verificación/promoción).
- Los grupos pueden ampliarse convirtiéndose a premium (aumento de aforo mediante suscripción/pago único).
- La aplicación cobrará un pequeño interés/comisión sobre los pagos realizados a los profesores.
- La aplicación se venderá también a academias para que tengan la misma funcionalidad premium descrita a un plan más rebajado.

---

## 3. Hoja de Ruta: Puntos de Extensión (Futuro)

1. **Chatbot y Asistente IA**
    - Descripción: Chatbot para ayudar a preparar reuniones, responder dudas y generar preguntas tipo test.
    - Objetivo: Mejorar la calidad de los encuentros y facilitar la moderación.

2. **Salas de Estudio Virtuales In-App**
    - Descripción: Integración de videollamada y pizarra compartida dentro de la app. Además la sesión será grabada se subirá a Classroom.
    - Objetivo: Facilitar sesiones síncronas sin salir del ecosistema.

3. **Sistema de Reputación y Feedback**
    - Descripción: Medallas, reseñas y métricas de asistencia/participación para profesores y miembros.
    - Objetivo: Generar confianza y mejorar el matching entre profesores y grupos.

4. **Gestión Avanzada de Pagos y Nóminas para Profesores**
    - Descripción: Funcionalidades para la planificación de cobros, liquidaciones periódicas y gestión fiscal básica.
    - Objetivo: Simplificar la gestión económica de profesores y facilitar contratos dentro de la plataforma.

5. **Herramientas de Matching y Búsqueda Avanzada**
    - Descripción: Algoritmos para recomendar profesores a grupos, sugerir grupos a alumnos y filtrar por especialidad, precio, valoración y disponibilidad.
    - Objetivo: Aumentar la tasa de contratación y retención.

---

## 4. Arquitectura del sistema

### Estrategia: Monolito Modular
Para optimizar costes en fase temprana y facilitar el despliegue en PaaS, permitiendo extracción a microservicios en el futuro.

- **Backend:** `Java` + `Spring Boot`.
    - Organización por módulos: `Auth`, `Groups`, `Meeting`, `Billing`, `Integration`.
- **Frontend:** `ReactJS` + `JS`.
- **Base de Datos:** `PostgreSQL`.
- **Integraciones clave:** Google Classroom (gestión de material y tareas), Google Maps (ubicación de meetings), pasarelas de pago para gestión de sueldos.

### Infraestructura
- **Entorno:** PaaS (Platform as a Service).
- **Proveedores candidatos:** Railway, Render o Heroku.
- **Justificación:** Coste bajo inicial, escalado vertical sencillo y configuración DevOps mínima.

---