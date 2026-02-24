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

* **Riesgos de Equipo y Organización**
   * **R1:** Incumplimiento de la dedicación horaria (10h/semana) por parte de algún miembro.
   * **R2:** Abandono de la asignatura o del grupo por parte de uno o más integrantes.
   * **R3:** Mala comunicación interna o falta de asistencia a las *Daily* (lunes) o *Retrospectivas* (viernes).
   * **R4:** Desalineación entre squads (Backend, Frontend, Arquitectura, Marketing), generando bloqueos.
   * **R5:** Delegación inadecuada de tareas y sobrecarga desigual entre squads.
* **Riesgos Técnicos y de Requisitos**
   * **R6:** Curva de aprendizaje lenta con las tecnologías del proyecto.
   * **R7:** Pérdida de código o conflictos graves en el control de versiones (Git).
   * **R8:** Cambios drásticos en los requisitos tras el feedback de la *Sprint Review* de los jueves.
   * **R9:** Sobrecarga del *Sprint Backlog* por mala estimación en la planificación.
   * **R10:** Fallos en la integración o sincronización con Google Classroom (errores de permisos, tokens, cambios en API).
   * **R11:** Pérdida, corrupción o fallo en la base de datos.
* **Riesgos de Usuarios Piloto**
   * **R12:** Los usuarios piloto no proporcionan feedback útil o no participan.
   * **R13:** La propuesta de valor de la aplicación no termina de convencer a los usuarios piloto.
* **Riesgos Organizacionales y de Uso (Externos)**
   * **R14:** Creación masiva de grupos de estudio pero con escasa o nula actividad real.
   * **R15:** Fuga de transacciones (ej. contactar con un profesor por la app pero pagar por Bizum).
   * **R16:** Falta de confianza inicial en la plataforma por ser una marca nueva.
* **Riesgos de Mercado y Económicos**
   * **R17:** Sustitución por herramientas generalistas (estudiantes prefieren WhatsApp o Discord).
   * **R18:** Alto poder del usuario y baja conversión hacia el modelo premium.
   * **R19:** No captación suficiente de profesores para generar un marketplace funcional.
* **Riesgos Tecnológicos Operativos**
   * **R20:** Dependencia de APIs externas (aumento de costes imprevisto o cambios de condiciones).
   * **R21:** Falta de masa crítica (número insuficiente de usuarios en una misma zona para ser útil).

---

## 3. Evaluación de Riesgos (Probabilidad e Impacto)

### Escala utilizada

**Probabilidad:**
- Baja (≤30%)
- Media (30–60%)
- Alta (>60%)

**Impacto:**
- Bajo: Retraso menor dentro del sprint.
- Medio: Afecta a una funcionalidad relevante.
- Alto: Afecta al core del producto o al cumplimiento del sprint.
- Muy Alto: Compromete la viabilidad del proyecto.

| ID  | Riesgo | Probabilidad | Impacto | Nivel |
|:---|:---|:---:|:---:|:---:|
| **R1**  | Incumplimiento de horas | Media | Alto | **Alto** |
| **R2**  | Abandono de miembro | Baja | Muy Alto | **Alto** |
| **R3**  | Fallos de comunicación | Media | Medio | **Medio** |
| **R4**  | Desalineación entre squads | Media | Alto | **Alto** |
| **R5**  | Delegación inadecuada de tareas | Media | Alto | **Alto** |
| **R6**  | Curva de aprendizaje tecnológica | Alta | Medio | **Alto** |
| **R7**  | Conflictos en Git / pérdida código | Baja | Alto | **Medio** |
| **R8**  | Cambios drásticos de requisitos | Alta | Medio | **Alto** |
| **R9**  | Mala estimación del Sprint Backlog | Media | Medio | **Medio** |
| **R10** | Fallos en integración con Classroom | Media | Muy Alto | **Alto** |
| **R11** | Pérdida o corrupción de base de datos | Baja | Muy Alto | **Alto** |
| **R12** | Falta de feedback de usuarios piloto | Media | Medio | **Medio** |
| **R13** | La app no convence a pilotos | Media | Muy Alto | **Alto** |
| **R14** | Muchos grupos sin actividad real | Alta | Medio | **Alto** |
| **R15** | Pagos fuera de la plataforma | Alta | Alto | **Alto** |
| **R16** | Falta de confianza inicial | Alta | Alto | **Alto** |
| **R17** | Sustitución por herramientas generalistas | Alta | Alto | **Alto** |
| **R18** | Baja conversión a premium | Alta | Medio | **Medio** |
| **R19** | No captación suficiente de profesores | Media | Muy Alto | **Crítico** |
| **R20** | Problemas con APIs externas | Baja | Alto | **Medio** |
| **R21** | Falta de masa crítica en zonas | Alta | Muy Alto | **Crítico** |

---

## 4. Planes de Mitigación y Contingencia

A continuación, se detallan las estrategias para afrontar los riesgos:
* **Equipo y Organización (R1, R2, R3, R4, R5):**
    * *Mitigación:* Revisión semanal de horas en Clockify. Fomentar comunicación temprana de bloqueos. Realizar demos integradas semanales entre squads. Definir claramente las responsabilidades por rol. Establecer un tiempo mínimo de investigación individual antes de escalar un problema técnico. Documentar soluciones recurrentes en un repositorio compartido.
    * *Contingencia:* Redistribuir tareas críticas si se detecta sobrecarga. Aplicar evaluación asimétrica según el *Commitment Agreement*. En caso de delegación sistemática injustificada, aplicar las penalizaciones acordadas y revisar formalmente el reparto de responsabilidades en retrospectiva.
* **Técnicos y Requisitos (R6, R7, R8, R9, R10, R11):**
    * *Mitigación:* Establecer estándar de ramas en Git y uso obligatorio de Pull Requests con revisión. Realizar code reviews cruzadas y fomentar el *pair programming* en tareas complejas. Gestionar correctamente los tokens OAuth para la integración con Google Classroom. Configurar backups automáticos diarios de la base de datos. Utilizar entornos de pruebas antes de despliegue y realizar una planificación realista del Sprint Backlog.
    * *Contingencia:* Reducir el alcance del sprint si se producen cambios drásticos en requisitos. Restaurar el sistema desde backups en caso de fallo crítico. Desactivar temporalmente funcionalidades no críticas hasta estabilizar el sistema.
* **Usuarios Piloto (R12, R13):**
    * *Mitigación:* Seleccionar perfiles *early adopters* comprometidos. Realizar encuestas periódicas estructuradas. Ofrecer incentivos por participación activa y recoger métricas de uso reales para iterar rápidamente.
    * *Contingencia:* Ajustar la propuesta de valor si se detecta bajo interés. Simplificar o modificar funcionalidades que no generen adopción o engagement.
* **Uso y Mercado (R14, R15, R16, R17, R18, R19):**
    * *Mitigación:* Fomentar la unión a comunidades existentes en lugar de la creación masiva. Incentivar pagos dentro de la plataforma mediante beneficios visibles (mejor posicionamiento, insignias). Comunicar claramente el valor diferencial frente a herramientas generalistas. Lanzar campañas específicas para captación de profesores y destacar beneficios del modelo premium.
    * *Contingencia:* Aplicar promociones iniciales para generar tracción. Reducir temporalmente la comisión en fases tempranas. Realizar campañas hiperlocalizadas por campus para asegurar masa crítica inicial.
* **Tecnológicos Operativos (R20, R21):**
    * *Mitigación:* Diseñar una arquitectura modular que permita cambiar proveedores externos fácilmente. Monitorizar costes de APIs externas y establecer alertas tempranas. Realizar lanzamientos localizados para asegurar densidad de usuarios.
    * *Contingencia:* Cambiar de proveedor si aumentan costes o cambian condiciones de uso. Ajustar la estrategia de expansión geográfica si no se alcanza masa crítica suficiente.

---

## 5. Monitoreo y Control

El seguimiento de los riesgos es continuo y se evalúa en dos momentos clave:
* **Lunes (Daily de líderes):** Revisión de riesgos técnicos (R4, R5) y de equipo (R1, R2, R3).
* **Viernes (Retrospectiva):** Evaluación del feedback de los usuarios piloto tras la Review (R6, R8, R9) y análisis de métricas de negocio o tracción (R10 a R15) para aplicar medidas correctoras al siguiente *Sprint Backlog*.
