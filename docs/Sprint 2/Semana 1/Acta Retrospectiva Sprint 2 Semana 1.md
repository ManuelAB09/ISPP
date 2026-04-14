# Acta de Reunión

## Tipo de reunión: Retrospectiva

### Grupo 9 – Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Acta de Reunión  
**Sprint:** Sprint 2  
**Semana:** Semana 1  
**Estado:** Aprobado  
**Fecha:** 08/03/2026  
**Hora:** 17:00 – 18:30
**Lugar:** Online (Teams)  
**Autor:** Manuel Artero Bellido  

---

## Asistentes

- Manuel Artero Bellido
- Iana Miranda Caramé  
- Beatriz Gutierrez Arazo  
- Manuel Nuño García
- Manuel María Calderón Rodríguez
- Álvaro Luque Buzón
- Alejandro Soult Toscano

---

## Objetivo general de la reunión

Realizar la retrospectiva del Sprint 1 (revisión de casos de uso reales vs. ideales), definir las nuevas normativas y flujos de trabajo restrictivos para asegurar la calidad , estandarizar el repositorio y realizar el reparto de responsabilidades y funcionalidades para el Sprint 2.

---

## Desarrollo de la reunión

### 1. Revisión de Casos de Uso y Planificación

Se ha realizado un balance del Sprint 1 para ajustar el alcance del proyecto de cara a esta nueva iteración.

- **Retrospectiva S1:** Revisión, añadido y eliminación de los casos de uso desarrollados *actualmente* frente a los que *idealmente* se debían terminar.
- **Planificación S2:** Se han definido los casos de uso futuros que entrarán en el Sprint 2. Las funcionalidades que no den tiempo se dejarán planeadas para el **Sprint 3**.
- **Deadline Crítico:** La fecha límite para funcionalidades es el **¡¡¡DOMINGO 15 DE MARZO!!!** (Solo funcionalidades, sin incluir tests).

### 2. Normativa de Repositorio, PRs y Clean Code

Se establecen reglas estrictas para evitar problemas de integración y código sucio que puedan llevar al suspenso.

- **Gestión de Ramas y PRs:** * Prohibido que dos personas trabajen en la misma rama.
  - Se requiere 1 review positivo por PR para poder mergear.
  - **Flujo obligatorio antes de hacer PR (<-- THISSS):** Hacer `pull` de `develop` -> Mergear `develop` con tu rama -> Resolver conflictos -> Mergear tu rama con `develop`.
- **Tolerancia Cero (Penalizaciones):** Quien no compile, pruebe o ejecute tests antes de hacer commit y rompa `develop`, se lleva aviso y penalización.
- **Congelación de Código:** Para el martes por la noche tiene que estar el **100% de los errores de S1 arreglados**. A partir de entonces, las ramas se bloquean y no se aceptan PRs sin permiso de Dirección. Un commit no puede romper la app.
- **Limpieza (Clean Code):** Hay que dejar todo limpio, intentando alinear `trunk` y `main`. Hay que borrar TODOS los comentarios "tontos" y dejar código profesional.

### 3. Migración y Estandarización

- **Adiós OneDrive:** Hay que migrar TODA la carpeta "Trabajo" de OneDrive al repositorio en formato Markdown y **ELIMINAR EL RASTRO EN ONEDRIVE** para evitar múltiples versiones.
  - *Nueva regla:* Solo se usará Word para documentos que requieran actualización concurrente y constante (ej. errores de feedback).

- **Variables de Entorno:** Se estandarizan las variables de `application.properties` (incluyendo tests) para que todo el equipo use las mismas. Se creará un `copy.env` (como en EGC) para facilitar el copiar/pegar.
- **Gestión de Secretos:** Anotación y protección de TODOS los secretos en GitHub, Azure o Render según corresponda.

### 4.  Reglas de Calidad

Se deben evitar rigurosamente los siguientes fallos:

- **T-10:** Una interacción legal resulta en un error HTTP percibido por el usuario.
- **T-11:** Una interacción legal resulta en un *panic* (crash) percibido por el usuario.
- **T-12:** Una interacción legal no tiene el comportamiento esperado.
- **T-13:** Enviar un formulario con datos obligatorios faltantes o erróneos no es detectado (fallo en validación).
- **T-14:** Un actor puede listar, editar o borrar datos que pertenecen a otro actor y que solo el admin debería gestionar.

---

## Reparto de Tareas y Roles

### Roles Generales

- **Dirección:** Gestión, reuniones, generación de artefactos, plantillas y presentación.

- **RRSS (Maca):** Gestionar usuarios piloto, recoger feedback en Word, REHACER todo el análisis de presupuesto al detalle (OPEX, CAPEX, re-análisis S1) y probar apps asignadas (Current y Donde Siempre). Más adelante: pensar en campañas para la ETSI.
- **Arquitectura (Álvaro):** Velar por despliegue, CI/CD. Resolver errores, dudas, definir normas en caso de conflicto y asegurar el avance técnico.
- **Marketing:** *Bye bye! :D*

### Reparto de Desarrollo

**Equipos (Squads y Dúos):**

- **Squad A:** Manu Backend, Fran, Josema, Juanan
- **Squad B:** Julio, Mario, Cynthia, Laura
- **Squad C:** Nora, Bea, Manu Frontend, Juan
- **Squad R:** Iana, Maca, Ale Ruiz
- *Dúos A - F:* Pendientes de asignar nombres exactos.

**Cronograma:**

| Semana | Tarea / Funcionalidad | Asignado a |
| :--- | :--- | :--- |
| **Semana 1** | **Arreglar errores S1 (PAL LUNES 100% :D)** | **Todos** |
| Semana 1 | Eventos y Calendarios | Dúo A |
| Semana 1 | Acceso premium y roles | Dúo B |
| Semana 1 | Seguridad en la autenticación | Dúo C |
| Semana 1 | Videoconferencias y Aulas Virtuales | Dúo D |
| Semana 1 | Ubicación y Mapas 2.0 | Dúo E |
| Semana 1 | Comunidades 2.0 | Dúo F |
| Semana 1 | Sistema de puntuación y ranking | Squad R |
| **Semana 2** | Gestión de contenidos y cuestionarios | Squad A |
| Semana 2 | Gestión de notificaciones | Squad B |
| Semana 2 | Otras funcionalidades adicionales | Squad C |
| **Semana 2** | **Testing Unitario y de Integración (16-18 de marzo)** | **Todos** |

---

## Observaciones adicionales

- **Presentación (PPT):** Hay cambios en la ppt (Iana sabe los detalles). Queda pendiente pegar el esquema y repartir las partes desde ya.
