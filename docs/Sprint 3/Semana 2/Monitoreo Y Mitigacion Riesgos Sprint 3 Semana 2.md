# Seguimiento y Evaluación de Riesgos

## Informe de Estado de Riesgos

### Grupo 9 – Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Plan de proyecto / Seguimiento  
**Sprint:** Sprint 3  
**Semana:** Semana 2  
**Estado:** Aprobado  
**Fecha:** 15/04/2026  
**Autor(es):** Manuel Artero Bellido

---

## Índice

1. [Introducción](#1-introducción)
2. [Riesgos Materializados y Acciones de Mitigación](#2-riesgos-materializados-y-acciones-de-mitigación)
3. [Riesgos en Monitorización Activa](#3-riesgos-en-monitorización-activa)
4. [Riesgos No Materializados](#4-riesgos-no-materializados)

---

## 1. Introducción

El presente documento tiene como objetivo evaluar el estado de los riesgos definidos inicialmente en el Plan de Gestión de Riesgos del proyecto MeerKatters. Se documentan aquellos riesgos que se han materializado durante el desarrollo reciente, detallando las acciones de mitigación y contingencia aplicadas. Asimismo, se registran los riesgos que, aunque han ocurrido, han sido mitigados mediante planes concretos y la definición y análisis de medidas concretas, permitiendo mantener el control sobre la evolución del proyecto.

---

## 2. Riesgos Materializados y Acciones de Mitigación

Durante este periodo, la incidencia de riesgos ha sido mínima, materializándose únicamente un riesgo técnico de forma leve, el cual ya ha sido subsanado.

### **R6: Dificultades técnicas con nuevas herramientas o tecnologías (Selenium)**

* **Descripción del suceso:** Se ha materializado en poca medida debido a la configuración inicial de los tests automatizados de integración (Selenium). Los tests presentaban fallos y bloqueos al principio porque las ejecuciones alcanzaban rápidamente el límite de llamadas permitidas por la API de Selenium.
* **Acciones de Mitigación / Contingencia aplicadas:**
  * Investigación y aprendizaje sobre el manejo eficiente de la herramienta.
  * Reconfiguración y optimización de los tests para reducir el número de peticiones, evitando así agotar la cuota de la API.
* **Medidas de validación:**
  * Ejecución completa de la suite de tests de Selenium sin errores de límite de tasa casi siempre, ya que muy de vez en cuando el CI si lo agota.

---

## 3. Riesgos en Monitorización Activa

En la fase actual del proyecto, se requiere especial atención sobre riesgos que impactan el modelo de negocio, por lo que se mantienen bajo vigilancia constante.

### **R16: Falta de confianza inicial en la plataforma**

* **Estado de monitorización:** Aunque este riesgo se materializó durante el sprint anterior y se aplicaron soluciones efectivas para mitigarlo, se ha decidido mantenerlo en monitorización activa.
* **Motivo:** La adopción y confianza por parte de los usuarios piloto es un factor que afecta de manera directa y crítica a la monetización de la aplicación. Cualquier fluctuación en la confianza del usuario podría impactar la validación del modelo de negocio de cara al entregable final, por lo que requiere vigilancia y ajustes continuos en la propuesta de valor.

---

## 4. Riesgos No Materializados

Los siguientes riesgos identificados en el Plan de Gestión de Riesgos no se han materializado durante este periodo y permanecen controlados:

* **Equipo y Organización:**
  * **R1:** Incumplimiento de la dedicación horaria.
  * **R2:** Abandono de la asignatura o del grupo.
  * **R3:** Mala comunicación interna o falta de asistencia a ceremonias.
  * **R4:** Desalineación entre squads.
  * **R5:** Delegación inadecuada de tareas y sobrecarga desigual.

* **Técnicos y Requisitos:**
  * **R7:** Pérdida de código o conflictos graves en Git.
  * **R8:** Cambios drásticos en los requisitos tras feedback.
  * **R9:** Sobrecarga del Sprint Backlog por mala estimación.
  * **R10:** Fallos en la integración con Google Classroom.
  * **R11:** Pérdida, corrupción o fallo en la base de datos.

* **Usuarios Piloto:**
  * **R12:** Falta de feedback útil o participación nula.
  * **R13:** Propuesta de valor no convence a los usuarios piloto.

* **Organizacionales, Uso y Mercado:**
  * **R14:** Creación masiva de grupos de estudio sin actividad real.
  * **R15:** Fuga de transacciones (pagos fuera de la app).
  * **R18:** Baja conversión hacia el modelo premium.
  * **R19:** No captación suficiente de profesores.
  * **R21:** Falta de masa crítica de usuarios.