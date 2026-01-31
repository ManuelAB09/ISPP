# Informe de Definición de Producto: Plataforma de Comunidades de Estudio


## 1. Resumen Ejecutivo
El objetivo es desarrollar una plataforma que conecte estudiantes para realizar actividades académicas conjuntas. La aplicación permite crear comunidades basadas en materias y cursos, gestionar encuentros presenciales o virtuales en ubicaciones específicas (bibliotecas, cafeterías) y compartir material de estudio.

**Modelo de Negocio:** Híbrido.
* **B2C:** Freemium (Ads/Suscripción).
* **B2B:** Licencias Institucionales (Universidades/Academias).

---

## 2. Alcance del MVP (Producto Mínimo Viable)


### A. Gestión de Comunidades y Actividades
* **Creación de Comunidades:** Segmentación por Materia, Curso/Nivel y Tipo de Actividad (Lectura, Exámenes, Debate, Dudas).
* **Gestión de Eventos (Meetups):**
    * **Geolocalización:** Integración con Google Maps para ubicación libre y selección de lista predefinida (Bibliotecas/Salas).
    * **Metadatos del evento:** Horario, Tecnología requerida, Aforo máximo.
* **Sistema de Roles:** Administrador de comunidad (creador), Asistente.

### B. Repositorio de Contenidos
* Subida y organización de archivos (PDFs de apuntes, imágenes de exámenes resueltos).
* Visualización de enlaces a grabaciones de reuniones pasadas.

### C. Sistema de Monetización y Accesos
* **Nivel Free:** Acceso a eventos, visualización de publicidad (Ads) y acceso de lectura restringido.
* **Nivel Premium:** Sin publicidad, descarga de apuntes, visualización de vídeos y exámenes resueltos.
* **Nivel Institucional (B2B):** Habilitación automática de funciones Premium para usuarios que se registren con un dominio de correo electrónico universitario/escolar contratado.

---

## 3. Hoja de Ruta: Puntos de Extensión (Futuro)

1.  **IA Pre-Screening (Chatbot Evaluador)**
    * *Descripción:* Un bot que genera 3-5 preguntas tipo test sobre el tema a tratar antes de permitir la inscripción a un evento.
    * *Objetivo:* Asegurar la homogeneidad del nivel del grupo y evitar frustraciones.

2.  **Marketplace de Apuntes (Peer-to-Peer)**
    * *Descripción:* Permitir que los estudiantes vendan sus resúmenes de alta calidad dentro de la plataforma, cobrando la app una comisión.
    * *Objetivo:* Incentivar la creación de contenido de calidad y generar ingresos extra.

3.  **Gamificación y Reputación**
    * *Descripción:* Sistema de medallas y niveles (ej: "Mentor", "Experto en Java").
    * *Objetivo:* Aumentar la retención premiando la asistencia y la colaboración.

4.  **Salas de Estudio Virtuales In-App**
    * *Descripción:* Integración de videollamada y pizarra compartida dentro de la propia app (sin salir a Zoom/Meet).
    * *Objetivo:* Mantener al usuario en el ecosistema el 100% del tiempo.

5.  **Dashboard Analítico para Instituciones**
    * *Descripción:* Panel para clientes B2B con métricas de uso (materias más estudiadas, horas de estudio, ocupación de salas).
    * *Objetivo:* Aportar valor tangible para renovar licencias B2B.

---

## 4. Arquitectura del sistema

### Estrategia: Monolito Modular
Para optimizar costes en fase temprana y facilitar el despliegue en PaaS, pero permitiendo una futura extracción a microservicios.

* **Backend:** `Java` + `Spring Boot`.
    * Organización interna por módulos: *Auth, Catalog, Meeting, Billing*...
* **Frontend:** `ReactJS`
* **Base de Datos:** `PostgreSQL` 
* **Storage:** `AWS S3` 

### Infraestructura
* **Entorno:** PaaS (Platform as a Service).
* **Proveedores candidatos:** Railway, Render o Heroku.
* **Justificación:** Coste bajo inicial, escalado vertical sencillo y configuración DevOps mínima.

---