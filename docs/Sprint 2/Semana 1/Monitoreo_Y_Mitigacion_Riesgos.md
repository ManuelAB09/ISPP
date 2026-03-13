# Seguimiento y Evaluación de Riesgos

## Informe de Estado de Riesgos

### Grupo 9 – Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Plan de proyecto / Seguimiento  
**Sprint:** Sprint 2  
**Semana:** Semana 1  
**Estado:** Aprobado  
**Fecha:** 11/03/2026  
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

Durante este periodo, se ha materializado un riesgo técnico crítico identificado en el plan original. A continuación, se detalla el contexto y las acciones tomadas para resolverlo de forma inmediata:

### **R7: Pérdida de código o conflictos graves en Git**

* **Descripción del suceso:** Durante la integración de nuevas funcionalidades mediante *Pull Request* (PR), se produjo una resolución inadecuada de conflictos de fusión (*merge conflicts*). Aunque las ramas principales (`main` y `dev`) ya estaban protegidas contra subidas directas, el error ocurrió durante la propia fusión del PR. La resolución precipitada provocó la sobrescritura y pérdida accidental de una funcionalidad que ya estaba implementada y validada en la rama de destino.
* **Acciones de Mitigación / Contingencia aplicadas:**
  * **Restauración del código:** Se intervino inmediatamente realizando un *rollback* (reversión) en Git hasta el *commit* previo a la fusión del PR, logrando recuperar la funcionalidad perdida y estabilizando el entorno de desarrollo.
  * **Resolución conjunta:** Los desarrolladores implicados se reunieron para resolver los conflictos e integrar los cambios paso a paso de forma coordinada, asegurando la preservación de todo el código funcional.
  * **Endurecimiento de la política de Pull Requests:** Como medida preventiva definitiva, se han modificado las reglas del repositorio. A partir de ahora, se exige obligatoriamente que el encargado de aprobar y hacer el *merge* del PR sea un revisor distinto al autor que sube el código. Además, el sistema bloquea la fusión hasta que exista un comentario explícito de aprobación positiva por parte de dicho revisor.

---

## 3. Riesgos en Monitorización Activa

Actualmente, dos riesgos se encuentran bajo estrecha vigilancia debido a fricciones detectadas tanto a nivel de infraestructura como de experiencia de usuario:

### **R17: Sustitución por herramientas generalistas (WhatsApp, Discord)**

* **Estado Actual:** En Observación (Monitorización).
* **Motivo:** Se ha detectado que la integración con **Google Classroom** está resultando un poco "pesada" para los usuarios piloto. La constante necesidad de conceder permisos y el flujo de autenticación o descargas generan una fricción notable. Existe un pequeño riesgo de que los estudiantes abandonen la plataforma al considerarla tediosa y opten por volver a métodos más directos y generalistas como crear grupos de WhatsApp o servidores de Discord.
* **Acciones tomadas:** Se está monitorizando la tasa de abandono (*drop-off rate*) justo en la pantalla de vinculación con Classroom. Si la frustración de los usuarios continúa siendo un bloqueante, se evaluará la contingencia de hacer que esta integración sea completamente opcional y secundaria, permitiendo un uso más ligero y rápido de la app en su primer contacto.

### **R20: Dependencia de APIs externas (Aumento de costes imprevisto)**

* **Estado Actual:** En Observación (Monitorización).
* **Motivo:** La alerta sigue activada en torno a la infraestructura de **Azure utilizada para el despliegue continuo (CI/CD)**. Los procesos de compilación y despliegue están consumiendo los créditos de la capa gratuita más rápido de lo estimado.
* **Acciones tomadas:** El equipo de Arquitectura está revisando la frecuencia de ejecución de los *pipelines* de integración. Se está evaluando la posibilidad de limitar los despliegues automáticos a una sola vez al día o migrar ciertos flujos a herramientas alternativas como GitHub Actions si los costes en Azure se vuelven insostenibles.

---

## 4. Riesgos No Materializados

Los siguientes riesgos identificados en el Plan de Gestión de Riesgos **no se han materializado** en este periodo y continúan en su estado de probabilidad e impacto original:

* **Equipo y Organización:**
  * **R1:** Incumplimiento de la dedicación horaria.
  * **R2:** Abandono de la asignatura o del grupo.
  * **R3:** Mala comunicación interna o falta de asistencia a ceremonias.
  * **R4:** Desalineación entre squads, generando bloqueos.
  * **R5:** Delegación inadecuada de tareas y sobrecarga desigual. *(Corregido en el sprint anterior)*
  * **R6:** Curva de aprendizaje lenta con las tecnologías. *(Superado en su fase crítica)*
* **Técnicos y Requisitos:**
  * **R8:** Cambios drásticos en los requisitos tras feedback.
  * **R9:** Sobrecarga del Sprint Backlog por mala estimación. *(Estabilizado)*
  * **R10:** Fallos técnicos directos en la sincronización con Google Classroom (independiente de la fricción del usuario).
  * **R11:** Pérdida, corrupción o fallo en la base de datos.
* **Usuarios Piloto:**
  * **R12:** Falta de feedback útil o participación nula.
  * **R13:** Propuesta de valor no convence a los usuarios piloto.
* **Organizacionales, Uso y Mercado:**
  * **R14:** Creación masiva de grupos de estudio sin actividad real.
  * **R15:** Fuga de transacciones (pagos fuera de la app).
  * **R16:** Falta de confianza inicial en la plataforma.
  * **R18:** Baja conversión hacia el modelo premium.
  * **R19:** No captación suficiente de profesores.
* **Tecnológicos Operativos:**
  * **R21:** Falta de masa crítica en zonas específicas.
