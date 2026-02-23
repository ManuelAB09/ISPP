# Plan de Gestión de Riesgos

## Identificación, Evaluación y Mitigación

### Grupo 9 – Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Plan de proyecto  
**Sprint:** Sprint 1  
**Semana:** Semana 1  
**Estado:** Aprobado  
**Fecha:** 23/02/2026  
**Autor(es):** Manuel Artero Bellido

---

## Índice
1. [Introducción](#1-introducción)
2. [Identificación de Riesgos](#2-identificación-de-riesgos)
3. [Evaluación de Riesgos (Probabilidad e Impacto)](#3-evaluación-de-riesgos-probabilidad-e-impacto)
4. [Planes de Mitigación y Contingencia](#4-planes-de-mitigación-y-contingencia)
5. [Monitoreo y Control](#5-monitoreo-y-control)

---

## 1. Introducción

Este documento establece el marco integral para identificar, evaluar, mitigar y monitorear los riesgos del proyecto MeerKatters. Al ser una plataforma orientada a la creación de comunidades de estudio y promoción de profesores, contemplamos tanto los riesgos internos (equipo, tecnología, metodología ágil) como los externos (adopción de usuarios piloto, retención financiera y competencia).

---

## 2. Identificación de Riesgos

Los riesgos se han clasificado en diversas categorías para abarcar todas las dimensiones del proyecto:

* **Riesgos de Equipo y Organización:**
    * **R1:** Incumplimiento de la dedicación horaria (10h/semana) por parte de algún miembro.
    * **R2:** Abandono de la asignatura o del grupo por parte de uno o más integrantes.
    * **R3:** Mala comunicación interna o falta de asistencia a las *Daily* (lunes) o *Retrospectivas* (viernes).
* **Riesgos Técnicos y de Requisitos:**
    * **R4:** Curva de aprendizaje lenta con las tecnologías del proyecto.
    * **R5:** Pérdida de código o conflictos graves en el control de versiones (Git).
    * **R6:** Cambios drásticos en los requisitos tras el feedback de la *Sprint Review* de los jueves.
    * **R7:** Sobrecarga del *Sprint Backlog* por mala estimación en la planificación.
* **Riesgos de Usuarios Piloto:**
    * **R8:** Los usuarios piloto no proporcionan feedback útil o no participan.
    * **R9:** La propuesta de valor de la aplicación no termina de convencer a los usuarios piloto.
* **Riesgos Organizacionales y de Uso (Externos):**
    * **R10:** Creación masiva de grupos de estudio pero con escasa o nula actividad real.
    * **R11:** Fuga de transacciones (ej. contactar con un profesor por la app pero pagar por Bizum).
* **Riesgos de Mercado y Económicos:**
    * **R12:** Sustitución por herramientas generalistas (estudiantes prefieren WhatsApp o Discord).
    * **R13:** Alto poder del usuario y baja conversión hacia el modelo premium.
* **Riesgos Tecnológicos Operativos:**
    * **R14:** Dependencia de APIs externas (aumento de costes imprevisto o cambios de condiciones).
    * **R15:** Falta de masa crítica (número insuficiente de usuarios en una misma zona para ser útil).

---

## 3. Evaluación de Riesgos (Probabilidad e Impacto)

| ID | Riesgo | Probabilidad | Impacto | Nivel de Riesgo |
|:---|:---|:---:|:---:|:---:|
| **R1** | Incumplimiento de horas/tareas | Media | Alto | **Alto** |
| **R2** | Abandono de un miembro | Baja | Muy Alto | **Alto** |
| **R3** | Fallos de comunicación | Media | Medio | **Medio** |
| **R4** | Curva de aprendizaje tecnológica | Alta | Medio | **Alto** |
| **R5** | Conflictos en Git/Pérdida código | Baja | Alto | **Medio** |
| **R6** | Cambios drásticos de requisitos | Alta | Medio | **Alto** |
| **R7** | Mala estimación de tareas | Media | Medio | **Medio** |
| **R8** | Falta de feedback de pilotos | Media | Medio | **Medio** |
| **R9** | La app no convence a pilotos | Media | Muy Alto | **Alto** |
| **R10**| Muchos grupos, poca actividad | Alta | Medio | **Alto** |
| **R11**| Pagos por fuera (Bizum, etc.) | Alta | Alto | **Alto** |
| **R12**| Uso de herramientas generalistas | Alta | Alto | **Alto** |
| **R13**| Baja conversión a premium | Alta | Medio | **Medio** |
| **R14**| Problemas con APIs externas | Baja | Alto | **Medio** |
| **R15**| Falta de masa crítica en zonas | Alta | Muy Alto | **Crítico** |

---

## 4. Planes de Mitigación y Contingencia

A continuación, se detallan las estrategias para afrontar los riesgos:

* **Equipo y Organización (R1, R2, R3):**
    * *Mitigación:* Revisión semanal en Clockify. Fomentar comunicación temprana.
    * *Contingencia:* Aplicar evaluación asimétrica (*Commitment Agreement*) y redistribuir tareas críticas.
* **Técnicos y Requisitos (R4, R5, R6, R7):**
    * *Mitigación:* Estándar de ramas en Git, *Pull Requests* con revisión, y mantener el *Product Backlog* flexible.
    * *Contingencia:* *Pair programming* para desbloqueos técnicos. Reducir el alcance (*scope*) del sprint si hay cambios drásticos de requisitos tras la Review.
* **Usuarios Piloto (R8, R9):**
    * *Mitigación:* Seleccionar perfiles específicos (early adopters) e incentivar el feedback. 
    * *Contingencia:* Iterar rápidamente la interfaz o pivotar la funcionalidad central si se detecta rechazo en las primeras Sprint Reviews.
* **Uso y Transacciones (R10, R11):**
    * *Mitigación (Grupos):* Mostrar sugerencias de eventos ya creados para fomentar la unión frente a la creación desde cero.
    * *Mitigación (Pagos):* Premiar a los usuarios (alumnos y profesores) que reciban y realicen pagos a través de la plataforma (mejor posicionamiento, insignias).
* **Mercado y Negocio (R12, R13):**
    * *Mitigación (Generalistas):* Comunicar un valor añadido claro: eventos presenciales + geolocalización + comunidad 100% académica.
    * *Mitigación (Premium):* Mostrar beneficios claros e incentivar con pequeñas demos gratuitas del modelo premium.
* **Tecnológicos Operativos (R14, R15):**
    * *Mitigación (APIs):* Arquitectura modular para poder cambiar de proveedor fácilmente y monitorización de costes.
    * *Mitigación (Masa crítica):* Lanzamiento hiperlocalizado por campus universitarios y captación dirigida para asegurar densidad desde el primer día.

---

## 5. Monitoreo y Control

El seguimiento de los riesgos es continuo y se evalúa en dos momentos clave:
* **Lunes (Daily de líderes):** Revisión de riesgos técnicos (R4, R5) y de equipo (R1, R2, R3).
* **Viernes (Retrospectiva):** Evaluación del feedback de los usuarios piloto tras la Review (R6, R8, R9) y análisis de métricas de negocio o tracción (R10 a R15) para aplicar medidas correctoras al siguiente *Sprint Backlog*.