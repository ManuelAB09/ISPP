# Acta de Reunión

## Tipo de reunión: Daily

### Grupo 9 – Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Acta  
**Sprint:** Sprint 1  
**Semana:** Semana 2  
**Estado:** Aprobado  
**Fecha:** 02/03/2026  
**Hora:** 18:00-18:45  
**Lugar:** Online (Teams)  
**Autor:** Manuel Artero Bellido  

---

## Asistentes
- Manuel Artero Bellido
- Alejandro Soult Toscano  
- Iana Miranda Caramé  
- Beatriz Gutierrez Arazo  
- Manuel Nuño García  
- Manuel María Calderón Rodríguez

---

## Objetivo general de la reunión
Sincronización diaria del equipo para revisar el estado de progreso de los diferentes departamentos (Backend, Frontend, Arquitectura, Marketing y RRSS), identificar bloqueos y establecer las pautas técnicas y organizativas para el cierre del Sprint 1.

---

## Desarrollo de la reunión

### 1. Estado de Backend
Se ha reportado el estado actual del desarrollo del lado del servidor. El progreso general es muy positivo y avanza a buen ritmo.
**Conclusión:** Todo está en orden para seguir avanzando. Queda pendiente finalizar la integración con Classroom.

### 2. Estado de Frontend
Se ha revisado el progreso de la interfaz de usuario. Faltan por pulir unos pocos detalles finales de las tareas actuales.
**Conclusión:** Se prevé que el trabajo de Frontend asignado esté completamente terminado para esta misma noche.

### 3. Arquitectura y Despliegue
Se debatió la posibilidad de incorporar a un miembro adicional al equipo de arquitectura (pendiente de decisión final). Se ha modificado la base de datos a PostgreSQL. El despliegue del Sprint 1 aún está incompleto: aunque se cuenta con Render, falta configurar la base de datos en Neon y conectarla. 
**Conclusión:** Se creará una rama específica `sprint1` con un flujo de CD configurado para bloquear subidas a partir del miércoles. Se establece como norma que cualquier merge realizado a `main` debe hacerse también obligatoriamente a la rama `sprint1`.

### 4. Marketing y Presupuesto
El equipo de Marketing ha reportado que todo avanza correctamente. Han estado prestando apoyo al equipo de Frontend y actualmente están trabajando en la elaboración del presupuesto. Durante la reunión se plantearon dudas sobre si la aplicación irá obligatoriamente desplegada en Azure y cómo se va a repartir el testing del Frontend.
**Conclusión:** Ambas preguntas fueron respondidas y aclaradas satisfactoriamente durante la sesión.

### 5. RRSS y Usuarios Piloto
El equipo de RRSS reporta un estado positivo, habiendo colaborado en tareas de Backend. Ya han finalizado la redacción del correo electrónico y el formulario de feedback dirigidos a los usuarios piloto de la aplicación.
**Conclusión:** El correo con la solicitud de feedback se enviará a los usuarios piloto mañana a primera hora.

### 6. Propuestas Técnicas y Evaluación de Rendimiento
Manuel Artero propuso establecer como norma obligatoria la creación de tests unitarios. El objetivo de esta medida es evitar que el código funcional se rompa por modificaciones de terceros sin que el sistema de Integración Continua (CI) lo detecte. Todo el equipo se mostró de acuerdo. Además, Manuel presentó y explicó un documento Excel diseñado para calcular de forma objetiva el nivel de contribución individual al proyecto.
**Conclusión:** Se aprueba por unanimidad la implementación obligatoria de tests unitarios como medida de seguridad. Se utilizará el Excel propuesto para evaluar las aportaciones de cada miembro.

---

## Observaciones adicionales
No aplica.