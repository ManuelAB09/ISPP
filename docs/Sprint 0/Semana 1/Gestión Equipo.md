# Documento de Gestión de Equipo

### Grupo D - Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Normativa  
**Sprint:** Sprint 0  
**Semana:** Semana 1  
**Estado:** Aprobado  
**Fecha:** 04/02/2026  
**Autor(es):** Alejandro Soult Toscano

---

## Índice
1. [Información del proyecto](#1-información-del-proyecto)
2. [Organización del equipo](#2-organización-del-equipo)
3. [Miembros del equipo](#3-miembros-del-equipo)
4. [Gestión de la comunicación](#4-gestión-de-la-comunicación)
5. [Gestión de incidencias](#5-gestión-de-incidencias)
6. [Gestión de tareas](#6-gestión-de-tareas)
7. [Gestión de reporte del tiempo](#7-gestión-de-reporte-del-tiempo)
8. [Política de ramas](#8-política-de-ramas)
9. [Política de commits](#9-política-de-commits)
10. [Estandarización del código](#10-estandarización-del-código)

---

## 1. Información del proyecto
El proyecto a realizar trata de una plataforma web de comunidades de estudio llamada **StudYshare** que permite crear y unirse a grupos por materia o curso, organizar eventos y quedadas con geolocalización, y compartir y gestionar apuntes y recursos académicos. Incluye gestión de roles (administradores y miembros), herramientas de coordinación (eventos, repositorios y notificaciones) y un modelo freemium con opciones premium e institucionales para capacidades y permisos ampliados.

---

## 2. Organización del equipo
El equipo se organiza en squads con responsabilidades claras para el desarrollo e lanzamiento del MVP:

1. Dirección
   - Coordinación general, toma de decisiones, organización y gestión de tareas del equipo, gestión de alcance y relación con el profesorado y cliente.
2. Arquitectura
   - Diseño técnico, definición de módulos, decisiones de infraestructura.
3. RRSS
   - Gestión de redes sociales, comunicación externa y construcción de comunidad.
4. Marketing
   - Diseño de logos y colores, estrategia de lanzamiento, posicionamiento, adquisición de usuarios y material promocional.
5. Backend
   - Desarrollo del API, lógica de negocio, base de datos e integraciones (auth, storage, pagos).
6. Frontend
   - Desarrollo de la interfaz, experiencia de usuario, accesibilidad y consumo del API.

Además, cada squad cuenta con un líder que coordina tareas, asegura la calidad y reporta el estado al Project Manager.

---

## 3. Miembros del equipo
| Nombre del Participante         | Rol / Cargo        | Squad       |
|---------------------------------|--------------------|-------------|
| Alejandro Soult Toscano         | Project Manager    | Dirección   |
| Manuel Artero Bellido           | Scrum Master       | Dirección   |
| Manuel María Calderón Rodríguez | Líder subgrupo     | Arquitectura|
| Raimundo Jímenez Lara           | Miembro            | Arquitectura|
| Iana Miranda Caramé             | Líder subgrupo     | RRSS        |
| Alejandro Ruiz Martín           | Miembro            | RRSS        |
| Álvaro Luque Buzón              | Líder subgrupo     | Marketing   |
| Macarena Pereira Campos         | Miembro            | Marketing   |
| Beatriz Gutierrez Arazo         | Líder subgrupo     | Backend     |
| Manuel Jesús Benito Merchán     | Miembro            | Backend     |
| Julio García Barrena            | Miembro            | Backend     |
| Francisco Fernández Noguerol    | Miembro            | Backend     |
| Nora Peñaloza Friqui            | Miembro            | Backend     |
| Mario Benítez Galván            | Miembro            | Backend     |
| Manuel Nuño García              | Líder subgrupo     | Frontend    |
| Cynthia Castaño Juan            | Miembro            | Frontend    |
| Juan Antonio Ruiz López         | Miembro            | Frontend    |
| Juan Moreno Ríos                | Miembro            | Frontend    |
| Laura Perez Franco              | Miembro            | Frontend    |
| José Manuel Márquez Guitérrez   | Miembro            | Frontend    |

---

## 4. Gestión de la comunicación
Medios y normas de uso:

- **WhatsApp**
  - Canal "Avisos": envío de avisos importantes a todo el equipo.
  - Grupo "Dirección": decisiones estratégicas y coordinación de alto nivel.
  - Grupos por squad: comunicación y coordinación interna de cada squad.

- **Teams**
  - Canal por tipo de reunión; convocatorias y asistencia oficiales vía Teams.
  - Conversaciones y material relacionado con reuniones se mantienen en el canal correspondiente.

- **OneDrive**
  - Carpeta compartida en el equipo de Teams para presentaciones y recursos binarios (no .md).

- **GitHub**
  - Repositorio para código y todos los archivos .md.
  - Tablero Kanban por squad (issues) para gestionar tareas.

Reuniones:
- **Daily** (standup semanal): lunes (horario flexible).
- **Sprint Review y Retrospective**: al finalizar cada sprint.

---

## 5. Gestión de incidencias
Cualquier incidencia debe comunicarse al líder del squad correspondiente, que intentará resolverla. Si no es posible solucionarla a nivel de squad, el líder debe escalarla a Dirección para buscar una solución a mayor nivel. De todas formas, **las incidencias se tratarán en una reunión (online o presencial)** cuando sea necesario.

---

## 6. Gestión de tareas
Para la gestión de tareas se usarán los tableros de issues de GitHub. Estas tendrán los siguientes usos:
- Planificar y coordinar el desarrollo de funcionalidades.
- Registrar errores o problemas técnicos.
- Documentar cambios o mejoras en la aplicación.
- Facilitar la revisión y control del progreso de las tareas.

Cada issue debe ser clasificada según su naturaleza:
- **task**: Tareas generales como workflows, configuración...
- **feature**: Nueva funcionalidad del proyecto.
- **testing**: Verificación y pruebas de funciones existentes.
- **bug**: Arreglos de problemas que afectan el comportamiento normal de la aplicación.
- **documentation**: Actualizaciones o mejoras en la documentación.
Igualmente, se provee de plantillas en GitHub para las issues de funcionalidad, tareas o configuración.

Las issues se gestionan mediante el tablero de tareas del repositorio, y deben atravesar estos estados:
1. **Pendiente**: Identificada pero aún no iniciada.
2. **En Desarrollo**: Actualmente en ejecución.
3. **En Revisión**: Completada y lista para pruebas o validación por otro miembro del equipo.
4. **Finalizada**: Verificada y cerrada.

Cada issue debe tener una prioridad asignada para facilitar la planificación del trabajo:
- **Alta**: Requiere atención inmediata, afecta el funcionamiento crítico del proyecto.
- **Media**: Importante pero no urgente; puede planificarse dentro del milestone actual.
- **Baja**: Mejora o ajuste que puede posponerse sin impacto significativo.

En cada issue deben identificarse los siguientes roles:
- **Responsable**: Persona o conjunto de personas encargadas de desarrollar o resolver la issue.
- **Revisor/a**: Persona encargada de validar, probar y aprobar la resolución antes de su cierre.

Se crearán tantos tableros como squads menos en **frontend** y **backend**, que se creará un tablero unificado de desarrollo. Para ello, se usarán etiquetas para determinar si una issue corresponde a frontend o backend.

---

## 7. Gestión de reporte del tiempo
Se usará **Clockify** para el registro de horas. Dentro de la plataforma, se creará un Project por cada squad (Dirección, Arquitectura, RRSS, Marketing, Backend, Frontend) y habrá uno para tareas comunes (como reuniones). Dentro de cada Project se añadirán tantas Tasks como sean necesarias para reflejar el trabajo. Igualmente, se utilizarán tags por sprint para facilitar el filtrado y los informes.

---

## 8. Política de ramas
Las ramas pueden ser de dos tipos:  

Ramas principales
- **main**: Rama principal para releases (solo merges desde hotfix o trunk tras QA).
- **trunk**: Rama de trabajo integrada: aquí se unen todas las features aprobadas.
- **hotfix**: Ramas para arreglos urgentes en producción; se crean a partir de main y se mergean a main y trunk.

Ramas auxiliares
- **task**: Para desarrollar tareas y partes de nuevas funcionalidades. Se crean a partir de trunk.
  - Convención de nombre: {feature,doc,test}/#{IDissue}_{NombreDescriptivoEnUpperCamelCase}
  - Ejemplo: feature/#123_AñadirLocalizaciónDeEvento
- **bugfix**: Para corregir bugs detectados en trunk. Se crean a partir de trunk.
  - Convención de nombre: bugfix/#{IDissue}_{NombreDescriptivoEnUpperCamelCase}
  - Ejemplo: bugfix/#210_ArreglarLoginTimeout

Reglas generales
- Cada rama debe referenciar el ID del issue correspondiente.
- No se harán uso de Pull Requests.
- Los merges a trunk y main deben pasar CI y revisión.

---

## 9. Política de commits
Los commits tendrán que ser los más unitarios posibles y frecuentes. En cuanto al contenido del mensaje, se hará uso de la estandarización **"Conventional Commits"**.

La estructura que deberán de seguir los mensajes de commits es la siguiente:
```
<type>(<scope>): <subject>

[body]

[footer]
```

Donde:
**type** (obligatorio): una palabra que indica el tipo de cambio (ver más abajo).  
**scope** (opcional): la parte del proyecto afectada (un módulo, componente, paquete).  
**subject** (obligatorio): breve descripción del cambio, en imperativo, sin punto final (máximo 50 caracteres).  
**body** (opcional): explicación más detallada del “qué” y “por qué” del cambio.  
**footer** (opcional): notas especiales, como referencias a issues (ej. Closes #123), breaking changes (BREAKING CHANGE: <descripción>), metadata adicional.  

Estos son los tipos estándares de Conventional Commits:
**feat**: Se introduce una nueva funcionalidad  
**fix**: Se corrige un error  
**docs**: Cambios en documentación  
**style**: Formato, estilo, espacios, puntos y comas (sin alterar lógica)  
**refactor**: Cambios que no agregan funcionalidad ni corrigen un error, pero mejoran estructura / legibilidad  
**perf**: Mejora de rendimiento  
**test**: Añadir o modificar pruebas  
**build**: Cambios que afectan el sistema de compilación o dependencias externas  
**ci**: Cambios en la integración continua / scripts del pipeline  
**chore**: Tareas auxiliares del proyecto (scripts de mantenimiento, configuración)  
**revert**: Revertir algún commit anterior  

---

## 10. Estandarización del código
Las directrices de estilo para el código Java se basan en las guías de Oracle y Google y se aplican como norma para todo el repositorio.

- Nomenclatura
  - Clases e interfaces: PascalCase (Ej.: MiClase, UserInfo).
  - Métodos y variables: lowerCamelCase (Ej.: calcularTotal, userName).
  - Constantes: MAYÚSCULAS_CON_GUIONES_BAJOS (Ej.: MAX_VALUE).
  - Paquetes: minúsculas, normalmente en formato de dominio invertido (Ej.: com.empresa.proyecto).

- Formato y estructura
  - Sangrado: 4 espacios por nivel (no tabuladores).
  - Llaves: llave de apertura en la misma línea de la declaración; cierre en nueva línea alineada.
  - Longitud de línea: preferible limitar a 80–100 caracteres para mejorar legibilidad.
  - Espaciado: espacios alrededor de operadores y después de comas; no poner espacio entre nombre de método y paréntesis de apertura.

- Buenas prácticas
  - Métodos pequeños y con una única responsabilidad.
  - Documentar APIs públicas con Javadoc.
  - Manejar excepciones explícitamente; evitar catch vacíos y no capturar Exception/Throwable genéricos.
  - Encapsulación: campos private y acceso mediante getters/setters cuando proceda.
  - Orden recomendado en clases: constantes, variables static, variables de instancia, constructores, métodos.

- Herramientas y cumplimiento
  - Activar formateo y checks en el IDE (IntelliJ/Eclipse).
  - Integrar análisis estático (SonarQube) y linters en CI.
  - Añadir checks automáticos (pre-commit / CI) para asegurar cumplimiento antes de merges.

Estas reglas son la base mínima; se revisarán y ampliarán según necesidades del proyecto.

---