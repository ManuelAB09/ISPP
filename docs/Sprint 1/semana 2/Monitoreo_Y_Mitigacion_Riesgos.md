# Seguimiento y Evaluación de Riesgos

## Informe de Estado de Riesgos

### Grupo 9 – Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Plan de proyecto / Seguimiento  
**Sprint:** Sprint 1  
**Semana:** Semana 2  
**Estado:** Aprobado  
**Fecha:** 02/03/2026  
**Autor(es):** Manuel Artero Bellido

---

## Índice
1. [Introducción](#1-introducción)
2. [Riesgos Materializados y Acciones de Mitigación](#2-riesgos-materializados-y-acciones-de-mitigación)
3. [Riesgos en Monitorización Activa](#3-riesgos-en-monitorización-activa)
4. [Riesgos No Materializados](#4-riesgos-no-materializados)

---

## 1. Introducción

El presente documento tiene como objetivo evaluar el estado de los riesgos definidos inicialmente en el Plan de Gestión de Riesgos del proyecto MeerKatters. Se documentan aquellos riesgos que se han materializado durante el desarrollo reciente, detallando las acciones de mitigación y contingencia aplicadas. Asimismo, se registran los riesgos que se encuentran bajo vigilancia especial y aquellos que, hasta la fecha, no han ocurrido, asegurando así un control continuo sobre las posibles amenazas al éxito del proyecto.

---

## 2. Riesgos Materializados y Acciones de Mitigación

Durante este periodo, se han materializado tres riesgos identificados en el plan original. A continuación, se detalla el contexto de cada uno y las acciones tomadas para resolverlos:

### **R5: Delegación inadecuada de tareas y sobrecarga desigual entre squads**
* **Descripción del suceso:** Se detectó una distribución asimétrica en la carga de trabajo y responsabilidades. Concretamente, se delegaron al equipo de Backend tareas críticas que correspondían a Arquitectura, como el montaje y configuración del entorno de desarrollo y la base de datos PostgreSQL, hitos que debían haber quedado cerrados desde el Sprint 1. 
  Esta falta de configuración inicial en la primera semana del sprint generó un efecto dominó, frenando significativamente el desarrollo del equipo de Frontend en la segunda semana (al necesitar instalar y conectar la base de datos localmente para poder avanzar). Además, se detectó que la guía de instalación del proyecto para PostgreSQL estaba incompleta, obligando al equipo de Frontend a invertir tiempo en investigar errores de configuración ajenos a su ámbito, lo que generó frustración y bloqueos innecesarios.
* **Acciones de Mitigación / Contingencia aplicadas:**
  * Tal y como se contemplaba en el plan, se procedió a **redistribuir las tareas** y reasignar responsabilidades de configuración de infraestructura, clarificando los límites entre Arquitectura, Backend y Frontend.
  * Se implementaron mejoras en los canales de **comunicación interna**.
  * **Actualización urgente de la documentación:** Para solventar los bloqueos inmediatos de Frontend y evitar que tengan que deducir configuraciones de backend, se ha completado la guía de instalación oficial. Se han añadido los comandos SQL exactos que faltaban para la correcta configuración del usuario, base de datos y contraseñas. Los comandos estandarizados incluidos en la guía son:
    ```sql
    CREATE USER meerkatters_user WITH PASSWORD 'meerkatters_password';
    CREATE DATABASE meerkatters OWNER meerkatters_user;
    GRANT ALL PRIVILEGES ON DATABASE meerkatters TO meerkatters_user;
    ```

### **R6: Curva de aprendizaje lenta con las tecnologías del proyecto**
* **Descripción del suceso:** Este riesgo afectó a dos áreas distintas del equipo de desarrollo:
  * **Frontend:** El equipo experimentó dificultades técnicas para lograr la conexión exitosa con el Backend (agravadas por los bloqueos mencionados en R5).
  * **Backend:** Hubo retrasos originados por la pronunciada curva de aprendizaje durante la configuración inicial y estructuración del proyecto.
* **Acciones de Mitigación / Contingencia aplicadas:**
  * Se activó la estrategia de **pair programming** (programación en parejas) estipulada en el plan de mitigación. Esto permitió que los desarrolladores resolvieran los problemas de conexión e integración de manera conjunta, compartiendo conocimientos en tiempo real y acelerando significativamente la superación de los obstáculos técnicos iniciales.

### **R9: Sobrecarga del Sprint Backlog por mala estimación en la planificación**
* **Descripción del suceso:** Se incluyeron demasiadas funcionalidades y tareas correspondientes al *core* (núcleo) del producto en el Sprint Backlog, lo que derivó en una estimación de tiempos poco realista y una sobrecarga inasumible.
* **Acciones de Mitigación / Contingencia aplicadas:**
  * Para optimizar el tiempo de resolución de las tareas ya comprometidas, se recurrió nuevamente a la técnica de **pair programming**. Esto incrementó la velocidad de entrega y redujo la tasa de errores en las funcionalidades críticas, logrando estabilizar el volumen de trabajo del sprint.

---

## 3. Riesgos en Monitorización Activa

### **R20: Dependencia de APIs externas (Aumento de costes imprevisto)**
* **Estado Actual:** En Observación (Monitorización).
* **Motivo:** Aunque inicialmente este riesgo se enfocaba en las APIs utilizadas dentro de la propia aplicación, la alerta se ha activado en torno a la infraestructura de **Azure utilizada para el despliegue continuo (CI/CD)**. 
* **Acciones tomadas:** Se están realizando cálculos exhaustivos sobre el consumo actual, ya que existe la posibilidad real de agotar los créditos disponibles. Si los cálculos confirman esta tendencia, se evaluará la contingencia de cambiar de proveedor o ajustar los flujos de despliegue para optimizar el coste.

---

## 4. Riesgos No Materializados

Los siguientes riesgos identificados en el Plan de Gestión de Riesgos **no se han materializado** hasta la fecha y continúan en su estado de probabilidad e impacto original:

* **Equipo y Organización:**
  * **R1:** Incumplimiento de la dedicación horaria.
  * **R2:** Abandono de la asignatura o del grupo.
  * **R3:** Mala comunicación interna o falta de asistencia a ceremonias.
  * **R4:** Desalineación entre squads, generando bloqueos.
* **Técnicos y Requisitos:**
  * **R7:** Pérdida de código o conflictos graves en Git.
  * **R8:** Cambios drásticos en los requisitos tras feedback.
  * **R10:** Fallos en la integración o sincronización con Google Classroom.
  * **R11:** Pérdida, corrupción o fallo en la base de datos.
* **Usuarios Piloto:**
  * **R12:** Falta de feedback útil o participación nula.
  * **R13:** Propuesta de valor no convence a los usuarios piloto.
* **Organizacionales, Uso y Mercado:**
  * **R14:** Creación masiva de grupos de estudio sin actividad real.
  * **R15:** Fuga de transacciones (pagos fuera de la app).
  * **R16:** Falta de confianza inicial en la plataforma.
  * **R17:** Sustitución por herramientas generalistas (WhatsApp, Discord).
  * **R18:** Baja conversión hacia el modelo premium.
  * **R19:** No captación suficiente de profesores.
* **Tecnológicos Operativos:**
  * **R21:** Falta de masa crítica en zonas específicas.