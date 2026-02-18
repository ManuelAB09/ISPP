# Decisión del Stack Tecnológico

## MeerKatters - Plataforma de Comunidades de Estudio

### Grupo 9 – Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Arquitectura / Decisión Técnica  
**Sprint:** Sprint DP  
**Semana:** Semana 2  
**Estado:** En revisión  
**Fecha:** 09/02/2026  
**Autor(es):** Raimundo Jiménez Lara, Manuel Maria Calderon Rodriguez.

---

## Índice
1. [Introducción](#1-introducción)
2. [Criterios de Evaluación](#2-criterios-de-evaluación)
3. [Análisis del Frontend](#3-análisis-del-frontend)
4. [Análisis del Backend](#4-análisis-del-backend)
5. [Análisis de Base de Datos](#5-análisis-de-base-de-datos)
6. [Análisis de Plataforma de Despliegue (PaaS)](#6-análisis-de-plataforma-de-despliegue-paas)
7. [Herramientas de CI/CD y DevOps](#7-herramientas-de-cicd-y-devops)
8. [Gestión de Dependencias y Build](#8-gestión-de-dependencias-y-build)
9. [Integraciones y Servicios Externos](#9-integraciones-y-servicios-externos)
10. [Resumen de Decisiones](#10-resumen-de-decisiones)
11. [Análisis de Riesgos del Equipo](#11-análisis-de-riesgos-del-equipo)
12. [Plan de Contingencia](#12-plan-de-contingencia)
13. [Conclusiones](#13-conclusiones)

---

## 1. Introducción

Este documento recoge el análisis y las decisiones tomadas respecto al stack tecnológico para el desarrollo de **MeerKatters**, una plataforma de comunidades de estudio que conecta alumnos entre sí y con profesores.

El objetivo es seleccionar tecnologías que:
- Sean adecuadas para un MVP con posibilidad de escalado futuro
- Permitan un desarrollo ágil y eficiente
- Minimicen los costes de infraestructura
- Aprovechen la experiencia previa del equipo

---

## 2. Criterios de Evaluación

Para cada categoría tecnológica se han evaluado las siguientes dimensiones:

| Criterio | Descripción |
|----------|-------------|
| **Experiencia del equipo** | Conocimiento previo del equipo en la tecnología |
| **Curva de aprendizaje** | Facilidad para que nuevos miembros se adapten |
| **Ecosistema** | Disponibilidad de librerías, plugins y comunidad |
| **Rendimiento** | Capacidad para manejar la carga esperada |
| **Coste** | Licencias y costes de infraestructura asociados |
| **Mantenibilidad** | Facilidad para mantener y evolucionar el código |
| **Integración** | Compatibilidad con otras herramientas del stack |

---

## 3. Análisis del Frontend

### 3.1 Alternativas Consideradas

| Framework | Descripción |
|-----------|-------------|
| **React + JavaScript** | Librería de UI basada en componentes |
| **Vue.js + JavaScript** | Framework progresivo con sintaxis más sencilla |
| **Angular** | Framework completo con arquitectura opinada |
| **Svelte** | Compilador que genera código vanilla JS |

### 3.2 Análisis Comparativo

| Criterio | React + JS | Vue.js + JS | Angular | Svelte |
|----------|------------|-------------|---------|--------|
| Experiencia del equipo | Alta | Media | Baja | Muy Baja |
| Curva de aprendizaje | Baja | Media | Baja | Media |
| Ecosistema | Alta | Alta | Alta | Media |
| Rendimiento | Alta | Alta | Media | Muy Alta |
| Mantenibilidad | Media | Media | Alta | Media |

### 3.3 Riesgos y Ventajas

#### React + JavaScript
| Ventajas | Riesgos |
|----------|--------|
| Amplia experiencia del equipo | Mayor verbosidad que Vue |
| Ecosistema maduro y extenso | Múltiples formas de hacer lo mismo (puede generar inconsistencias) |
| Configuración inicial más sencilla | Sin tipado estático (más propenso a errores en runtime) |
| Gran demanda laboral (facilita incorporaciones) | |
| Excelente integración con Google Maps API | |
| Componentes reutilizables para UI responsive | |

#### Vue.js + JavaScript
| Ventajas | Riesgos |
|----------|---------|
| Sintaxis más limpia y fácil de aprender | Menor experiencia del equipo |
| Documentación excelente | Ecosistema ligeramente menor que React |
| Single File Components intuitivos | Tiempo de adaptación necesario |

#### Angular
| Ventajas | Riesgos |
|----------|---------|
| Framework completo (routing, forms, HTTP incluidos) | Curva de aprendizaje pronunciada |
| Arquitectura muy estructurada | Escasa experiencia del equipo |
| CLI potente para generación de código | Requiere TypeScript (añade complejidad) |
| Inyección de dependencias nativa | Overhead para proyectos pequeños/medianos |

#### Svelte
| Ventajas | Riesgos |
|----------|---------|
| Excelente rendimiento | Sin experiencia en el equipo |
| Código más limpio | Ecosistema inmaduro |
| Bundle size reducido | Menor comunidad y recursos |

### 3.4 Decisión: React + JavaScript

**Justificación:** La experiencia previa del equipo con React es el factor determinante. Se opta por JavaScript en lugar de TypeScript para simplificar la configuración inicial y acelerar el desarrollo del MVP. Esto permite:
- Desarrollo más rápido desde el inicio
- Menor tiempo de onboarding para nuevos miembros
- Configuración más sencilla sin necesidad de compilación TS
- Aprovechar el amplio ecosistema para integraciones (Google Maps, pasarelas de pago)

---

## 4. Análisis del Backend

### 4.1 Alternativas Consideradas

| Framework | Lenguaje | Descripción |
|-----------|----------|-------------|
| **Spring Boot** | Java | Framework empresarial robusto y maduro |
| **Node.js + Express/NestJS** | JavaScript/TypeScript | Runtime JS para servidor |
| **Django** | Python | Framework "batteries included" |
| **ASP.NET Core** | C# | Framework de Microsoft para APIs |

### 4.2 Análisis Comparativo

| Criterio | Spring Boot | Node.js | Django | ASP.NET Core |
|----------|-------------|---------|--------|--------------|
| Experiencia del equipo | Alta | Media | Baja | Baja |
| Curva de aprendizaje | Media | Alta | Alta | Media |
| Ecosistema | Alta | Alta | Alta | Alta |
| Rendimiento | Alta | Alta | Media | Muy Alta |
| Seguridad integrada | Alta | Media | Alta | Alta |
| Integración con Google APIs | Alta | Alta | Alta | Media |

### 4.3 Riesgos y Ventajas

#### Java + Spring Boot
| Ventajas | Riesgos |
|----------|---------|
| Amplia experiencia del equipo | Mayor consumo de memoria que Node.js |
| Spring Security robusto (JWT, OAuth2, CSRF) | Tiempo de arranque más lento (cold start en PaaS) |
| Arquitectura modular (Auth, Groups, Meeting, Billing, Integration) | Verbosidad del lenguaje |
| Excelente soporte para tests (JUnit, Mockito) | |
| Spring Data JPA simplifica acceso a datos | |
| Documentación automática con OpenAPI/Swagger | |
| Madurez para aplicaciones empresariales | |

#### Node.js + Express/NestJS
| Ventajas | Riesgos |
|----------|---------|
| Mismo lenguaje en frontend y backend | Menor experiencia del equipo en backend Node |
| Arranque rápido (ideal para PaaS con cold start) | Gestión de tipos más compleja |
| NPM tiene muchas librerías | Callback hell si no se estructura bien |

#### Django
| Ventajas | Riesgos |
|----------|---------|
| Desarrollo rápido con ORM incluido | Sin experiencia del equipo |
| Admin panel automático | Menos flexible en arquitectura |
| Buena documentación | Rendimiento inferior en APIs REST |

#### ASP.NET Core
| Ventajas | Riesgos |
|----------|---------|
| Alto rendimiento | Sin experiencia del equipo |
| Soporte empresarial de Microsoft | Ecosistema más orientado a Microsoft |
| Buena integración con Azure | Curva de aprendizaje |

### 4.4 Decisión: Java + Spring Boot

**Justificación:** La experiencia del equipo con Java y Spring Boot es decisiva. Además:
- Spring Security facilita la implementación de autenticación (JWT, OAuth2 para Google Classroom)
- La arquitectura modular permite una evolución hacia microservicios si fuera necesario
- Excelentes herramientas de testing para cumplir con el requisito de 70% de cobertura (RNF-11)
- Integración probada con pasarelas de pago (Stripe)

---

## 5. Análisis de Base de Datos

### 5.1 Alternativas Consideradas

| Base de Datos | Tipo | Descripción |
|---------------|------|-------------|
| **PostgreSQL** | Relacional | BD open source más avanzada |
| **MySQL** | Relacional | BD open source popular |
| **MongoDB** | NoSQL (Documentos) | BD orientada a documentos |
| **SQLite** | Relacional embebida | BD ligera para desarrollo |

### 5.2 Análisis Comparativo

| Criterio | PostgreSQL | MySQL | MongoDB | SQLite |
|----------|------------|-------|---------|--------|
| Experiencia del equipo | Alta | Alta | Baja | Media |
| Soporte en PaaS | Alta | Alta | Alta | Baja |
| Funcionalidades avanzadas | Alta | Media | Alta | Baja |
| Integridad referencial | Alta | Alta | Baja | Alta |
| Escalabilidad | Alta | Alta | Muy Alta | Muy Baja |
| Coste en cloud | Alta | Alta | Media | Muy Alta |

### 5.3 Riesgos y Ventajas

#### PostgreSQL
| Ventajas | Riesgos |
|----------|---------|
| Excelente soporte para datos geoespaciales (PostGIS) - útil para meetings en mapa | Configuración inicial más compleja que MySQL |
| ACID compliant con integridad referencial robusta | Consumo de recursos ligeramente mayor |
| JSON nativo para flexibilidad de esquema | |
| Amplio soporte en todos los PaaS principales | |
| Integración excelente con Spring Data JPA | |
| Full-text search integrado | |

#### MySQL
| Ventajas | Riesgos |
|----------|---------|
| Muy conocido y documentado | Menos funcionalidades avanzadas |
| Buen rendimiento para lecturas | Peor soporte geoespacial |
| Amplio soporte en PaaS | |

#### MongoDB
| Ventajas | Riesgos |
|----------|---------|
| Flexibilidad de esquema | Sin experiencia del equipo |
| Escalabilidad horizontal | Integridad referencial manual |
| Bueno para datos no estructurados | Modelo de datos requiere rediseño |

### 5.4 Decisión: PostgreSQL

**Justificación:** PostgreSQL ofrece el mejor balance para los requisitos del proyecto:
- Soporte geoespacial para la funcionalidad de meetings en mapa (PostGIS)
- Integridad referencial necesaria para las relaciones complejas (usuarios, comunidades, eventos, pagos)
- Excelente integración con Spring Data JPA
- Disponible en Azure Database for PostgreSQL con tier gratuito para estudiantes

---

## 6. Análisis de Plataforma de Despliegue (PaaS)

### 6.1 Requisitos del Despliegue

| Requisito | Descripción | Prioridad |
|-----------|-------------|-----------|
| Persistencia de datos | No perder base de datos entre despliegues | Alta |
| Despliegue continuo | Integración con GitHub Actions | Alta |
| Coste | Gratuito o con opción para estudiantes | Alta |
| Experiencia del equipo| Conocimiento previo del equipo en la tecnología | Alta |
| Disponibilidad | Preferiblemente 24/7 sin hibernación | Media |
| Envío de correos | Soporte para notificaciones por email | Media |

### 6.2 Alternativas Consideradas

| Plataforma | Coste/Tier Gratuito | Persistencia BD | Despliegue Continuo | Disponibilidad 24/7 | Email | Archivos | Experiencia Equipo |
|------------|---------------------|-----------------|---------------------|---------------------|-------|----------|--------------------|
| **Azure (App Service + PostgreSQL + Blob)** | $100 créditos estudiantes | Sí (Managed) | Sí (GitHub Actions) | Sí (sin hibernación) | Sí (Communication Services) | Sí (Blob Storage) | Baja |
| **Render** | Free tier con limitaciones | Requiere tier pago | Sí (desde GitHub) | No (hibernación tras inactividad) | Limitado (problemas puertos) | Limitado (efímero) | Alta |
| **Railway** | $5/mes créditos | Sí (incluido) | Sí (desde GitHub) | Sí (según créditos) | No (externo) | Limitado | Baja |
| **Heroku** | Sin tier gratuito | Sí (add-on) | Sí (desde GitHub) | Sí (add-on) | No (add-on externo) | Limitado (efímero) | Baja |
| **Fly.io** | Free tier limitado | Sí (volumes) | Sí (flyctl/GitHub) | Sí (según tier) | No (externo) | Sí (volumes) | Baja |

### 6.3 Análisis Detallado

#### Azure (App Service + Database + Blob Storage + Communication Services)

| Ventajas | Riesgos |
|----------|---------|
| $100 USD gratis para estudiantes (Azure for Students) | **Sin experiencia previa del equipo en Azure** |
| Ecosistema completo y consistente (App, DB, Storage, Email) | Créditos pueden agotarse antes de lo esperado |
| Azure Database for PostgreSQL con backups automáticos | **Curva de aprendizaje significativa para configuración inicial** |
| Blob Storage para archivos adjuntos | Costes pueden escalar si no se monitorizan |
| Azure Communication Services para emails transaccionales | **Complejidad de la plataforma (portal, CLI, ARM templates)** |
| Despliegue continuo integrado con GitHub Actions | |
| SSL/TLS gratuito | |
| Escalado vertical y horizontal | |
| Sin hibernación en tiers básicos | |

#### Desafíos Específicos de Azure para el Equipo

Dado que **ningún miembro del equipo tiene experiencia previa con Azure**, se identifican los siguientes desafíos concretos:

| Área | Desafío | Impacto | Mitigación |
|------|---------|---------|------------|
| **Portal Azure** | Interfaz compleja con cientos de servicios | Alto - Tiempo perdido navegando | Usar Azure CLI en lugar del portal cuando sea posible |
| **Nomenclatura** | Términos diferentes a otros clouds (Resource Groups, App Service Plans, Deployment Slots) | Medio - Confusión inicial | Crear glosario interno de términos Azure |
| **Configuración App Service** | Application Settings, Connection Strings, Deployment Center | Alto - Errores de configuración | Seguir tutoriales oficiales paso a paso, documentar cada paso |
| **PostgreSQL Flexible Server** | Networking (VNet, firewall rules), parámetros de servidor | Alto - Problemas de conectividad | Empezar con "Allow public access" para desarrollo |
| **Deployment Slots** | No aplica en B1 (requiere S1 ~€58/mes); se usan 2 Web Apps como alternativa | Bajo - Concepto descartado | Documentado en CI_CD.md y Guia_Despliegue_Azure.md |
| **GitHub Actions + Azure** | Service Principal, RBAC, secretos de conexión | Alto - Bloquea CI/CD si falla | Seguir guía oficial de Microsoft, reservar tiempo extra |
| **Monitoreo de costes** | Cost Management, alertas de presupuesto, métricas | Medio - Riesgo de gastar créditos | Configurar alertas desde el día 1 |
| **Logs y diagnóstico** | Application Insights, Log Analytics, métricas | Medio - Debugging más difícil | Habilitar logs básicos, aprender a usarlos progresivamente |

#### Estimación de Tiempo Extra por Curva de Aprendizaje

| Tarea | Sin experiencia Azure | Con experiencia Azure | Diferencia |
|-------|----------------------|----------------------|------------|
| Crear App Service + configurar | 4-6 horas | 30 min | +5 horas |
| Configurar PostgreSQL | 3-4 horas | 20 min | +3.5 horas |
| Configurar GitHub Actions CD | 4-5 horas | 1 hora | +4 horas |
| Configurar 2 Web Apps (staging + prod) | 2-3 horas | 15 min | +2.5 horas |
| Troubleshooting inicial | 8-10 horas | 2 horas | +8 horas |
| **Total Sprint 1** | **~25 horas extra** | - | - |

> **Nota**: Este tiempo extra está contemplado en el Sprint 1 de Arquitectura (ver Planning_Sprint_Arquitectura.md).

#### Render

| Ventajas | Riesgos |
|----------|---------|
| Interfaz simple e intuitiva | **Hibernación por inactividad** (necesita cron job para mantener despierto) |
| Experiencia del equipo elevada | **Persistencia de BD requiere tier de pago** |
| Despliegue desde GitHub automático | **Problemas con puertos SMTP** para envío de emails |
| SSL automático | Cold start de ~30 segundos |
| Preview environments | Almacenamiento efímero en free tier |

#### Railway

| Ventajas | Riesgos |
|----------|---------|
| Buena experiencia de desarrollador | Créditos mensuales limitados ($5) |
| PostgreSQL incluido | Puede agotar créditos rápidamente |
| Variables de entorno fáciles | Sin servicio de email integrado |

### 6.4 Análisis de Costes Azure (Estimación)

| Servicio | Tier | Coste Estimado/Mes |
|----------|------|-------------------|
| App Service | B1 (Básico) | ~€11 |
| Azure Database for PostgreSQL | Burstable B1ms | ~€12.50 |
| Blob Storage | 5GB | ~€0.10 |
| Communication Services | 1000 emails | ~€0.25 |
| **Total estimado** | | **~€23-24/mes** |

Con €83.72 de créditos ($100 USD): **~3.5 meses de desarrollo/testing**

### 6.5 Decisión: Azure como Plataforma Principal

**Justificación:**
- Los $100 de créditos gratuitos para estudiantes cubren el período de desarrollo del MVP
- Ecosistema completo que evita integrar múltiples proveedores
- Azure Communication Services resuelve el envío de notificaciones por email
- Persistencia de datos garantizada sin coste adicional
- Sin problemas de hibernación o cold start prolongados

**Nota importante:** Aunque el equipo no tiene experiencia previa con Azure, se ha seleccionado por los beneficios económicos y técnicos mencionados. Como medida de seguridad, se ha documentado un [Plan de Contingencia](#11-plan-de-contingencia) detallado basado en **Render**, plataforma con la que el equipo sí tiene alta experiencia (ver tabla 6.2). Este fallback permite migrar rápidamente en caso de que surjan problemas con Azure o se agoten los créditos antes de lo esperado.

---

## 7. Herramientas de CI/CD y DevOps

### 7.1 Alternativas Consideradas

| Herramienta | Descripción |
|-------------|-------------|
| **GitHub Actions** | CI/CD integrado en GitHub |
| **GitLab CI** | CI/CD de GitLab |
| **Jenkins** | Servidor CI/CD self-hosted |
| **CircleCI** | CI/CD cloud |

### 7.2 Análisis Comparativo

| Criterio | GitHub Actions | GitLab CI | Jenkins | CircleCI |
|----------|----------------|-----------|---------|----------|
| Experiencia del equipo | Alta | Media | Baja | Baja |
| Integración con repo | Alta | Alta | Media | Alta |
| Coste | Alta | Alta | Media | Media |
| Configuración | Alta | Alta | Baja | Alta |
| Marketplace/Plugins | Alta | Media | Alta | Media |

### 7.3 Riesgos y Ventajas

#### GitHub Actions
| Ventajas | Riesgos |
|----------|---------|
| Integración nativa con repositorio GitHub | Minutos limitados en repos privados (2000/mes gratis) |
| Gratuito para repos públicos | Debugging más difícil que Jenkins |
| Marketplace con acciones pre-construidas | |
| Sintaxis YAML familiar | |
| Secrets management integrado | |
| Integración directa con Azure | |

### 7.4 Decisión: GitHub Actions

**Justificación:** El repositorio del proyecto está en GitHub, por lo que GitHub Actions ofrece la integración más natural. Además:
- Gratuito para el uso esperado
- Experiencia previa del equipo
- Fácil integración con Azure para despliegue continuo
- Soporte para SonarQube en el pipeline (RNF-14)

### 7.5 Contenedores: Docker

| Ventajas | Riesgos |
|----------|---------|
| Consistencia entre entornos (dev/staging/prod) | Overhead inicial de configuración |
| Facilita el despliegue en cualquier PaaS | Imágenes pueden crecer si no se optimizan |
| Docker Compose para desarrollo local | |
| Experiencia previa del equipo | |

**Decisión: Docker** para containerización, con Docker Compose para el entorno de desarrollo local.

---

## 8. Gestión de Dependencias y Build

### 8.1 Backend: Maven

| Ventajas | Riesgos |
|----------|---------|
| Estándar en proyectos Spring Boot | XML verbose (pom.xml) |
| Gestión de dependencias robusta | Builds más lentos que Gradle |
| Integración con SonarQube | |
| Experiencia del equipo | |
| Plugins para tests y cobertura (JaCoCo) | |

**Decisión: Maven** por experiencia del equipo y compatibilidad con el ecosistema Spring.

### 8.2 Frontend: npm

| Ventajas | Riesgos |
|----------|---------|
| Estándar para proyectos React | node_modules puede crecer mucho |
| Amplio registro de paquetes | Vulnerabilidades en dependencias |
| Scripts personalizados en package.json | |
| Integración con ESLint, Prettier | |

**Decisión: npm** como gestor de paquetes del frontend.

---

## 9. Integraciones y Servicios Externos

### 9.1 Pasarela de Pagos

La plataforma requiere gestión de pagos para:
- Verificación de profesores (RF-12, RF-61)
- Suscripciones premium de grupos (RF-55)
- Pagos a profesores con comisión (RF-62)
- Planes para academias (RF-63)

#### Alternativas Consideradas

| Pasarela | Descripción |
|----------|-------------|
| **Stripe** | Pasarela líder con API moderna y amplia documentación |
| **PayPal** | Pasarela muy conocida, popular entre usuarios |
| **Square** | Enfocada en comercios, menos flexible para SaaS |
| **Adyen** | Enfoque empresarial, más compleja |

#### Análisis Comparativo

| Criterio | Stripe | PayPal | Square | Adyen |
|----------|--------|--------|--------|-------|
| Experiencia del equipo | Alta | Baja | Baja | Baja |
| Documentación/API | Alta | Media | Media | Alta |
| Comisiones | 1.4% + 0.25€ (UE) | 2.9% + 0.35€ | 1.9% + 0.25€ | Variable |
| Soporte suscripciones | Alta | Media | Baja | Alta |
| Integración Spring Boot | Alta | Media | Baja | Media |
| Split payments (pagos a profesores) | Sí (Connect) | Sí (limitado) | No | Sí |

#### Riesgos y Ventajas

**Stripe**
| Ventajas | Riesgos |
|----------|---------|
| API muy bien documentada y moderna | Requiere verificación de cuenta para Stripe Connect |
| Stripe Connect permite pagos a profesores directamente | Comisiones adicionales en Connect |
| Soporte nativo para suscripciones recurrentes | |
| Dashboard completo para gestión | |
| Webhooks para eventos de pago | |
| SDK oficial para Java | |
| Modo test sin coste | |

**PayPal**
| Ventajas | Riesgos |
|----------|---------|
| Muy conocido por usuarios | API menos moderna |
| Alta confianza del consumidor | Comisiones más altas |
| | Soporte de suscripciones más limitado |
| | Split payments más complejo |

#### Decisión: Stripe

**Justificación:**
- **Stripe Connect** permite implementar el modelo de pagos a profesores con comisión de forma nativa
- API moderna y bien documentada, con SDKs oficiales para Java
- Soporte completo para suscripciones (grupos premium)
- Comisiones competitivas para el mercado europeo
- Modo sandbox gratuito para desarrollo y testing
- Webhooks robustos para sincronizar estados de pago

---

### 9.2 Chat en Tiempo Real

El chat de comunidad (RF-34) es una funcionalidad extra pero relevante para la experiencia de usuario.

#### Alternativas Consideradas

| Solución | Tipo | Descripción |
|----------|------|-------------|
| **WebSocket (Spring)** | Self-hosted | Implementación propia con Spring WebSocket |
| **Pusher** | SaaS | Servicio de mensajería en tiempo real |
| **Ably** | SaaS | Plataforma de mensajería pub/sub |
| **Firebase Realtime DB** | SaaS | Base de datos en tiempo real de Google |
| **Socket.io** | Self-hosted | Librería popular para WebSockets |

#### Análisis Comparativo

| Criterio | WebSocket Spring | Pusher | Ably | Firebase |
|----------|------------------|--------|------|----------|
| Coste | Gratis (self-hosted) | Free tier: 200k msg/día | Free tier: 6M msg/mes | Free tier: 100 conexiones |
| Complejidad | Alta | Baja | Baja | Baja |
| Escalabilidad | Requiere configuración | Alta | Alta | Alta |
| Integración React | Manual | SDK oficial | SDK oficial | SDK oficial |
| Persistencia mensajes | Manual (PostgreSQL) | No incluido | Incluido | Incluido |

#### Riesgos y Ventajas

**WebSocket con Spring Boot**
| Ventajas | Riesgos |
|----------|---------|
| Control total sobre la implementación | Mayor complejidad de desarrollo |
| Sin costes adicionales de terceros | Requiere gestionar escalabilidad |
| Integración directa con el backend existente | Persistencia debe implementarse manualmente |
| STOMP sobre WebSocket bien soportado en Spring | |

**Pusher/Ably**
| Ventajas | Riesgos |
|----------|---------|
| Implementación rápida | Dependencia de servicio externo |
| Escalabilidad gestionada | Costes pueden escalar con uso |
| SDKs para múltiples plataformas | Free tier puede quedarse corto |

#### Decisión: WebSocket con Spring Boot (STOMP)

**Justificación:**
- El chat es funcionalidad extra, no crítica para MVP
- Spring Boot tiene soporte nativo para WebSocket con STOMP
- Evita dependencias y costes adicionales
- Los mensajes se persisten en PostgreSQL (ya elegido)
- Se puede migrar a solución SaaS si la carga lo requiere

---

### 9.3 Videollamadas (Punto de Extensión)

Las reuniones virtuales (RF-48) y grabación (RF-49) son funcionalidades extra para fases posteriores.

#### Alternativas Consideradas

| Solución | Tipo | Descripción |
|----------|------|-------------|
| **Daily.co** | SaaS | API de video con embebido fácil |
| **Twilio Video** | SaaS | Servicio de video de Twilio |
| **Jitsi** | Self-hosted/SaaS | Solución open source |
| **Zoom SDK** | SaaS | SDK del popular Zoom |
| **100ms** | SaaS | Plataforma moderna de video |

#### Análisis Comparativo

| Criterio | Daily.co | Twilio | Jitsi | 100ms |
|----------|----------|--------|-------|-------|
| Tier gratuito | 10k min/mes | 5k min/mes (trial) | Gratis (self-hosted) | 10k min/mes |
| Facilidad integración | Alta | Media | Media | Alta |
| Grabación | Sí (de pago) | Sí (de pago) | Sí (self-hosted) | Sí (de pago) |
| Calidad | Alta | Alta | Media | Alta |
| Embebido en web | Sí | Sí | Sí | Sí |

#### Riesgos y Ventajas

**Daily.co**
| Ventajas | Riesgos |
|----------|---------|
| API muy simple, embed en minutos | Grabación requiere plan de pago |
| 10.000 minutos gratis al mes | Dependencia de tercero |
| Rooms prec-configuradas | Costes pueden escalar |
| Documentación excelente | |

**Jitsi (Self-hosted)**
| Ventajas | Riesgos |
|----------|---------|
| Completamente gratuito | Requiere servidor dedicado |
| Open source, sin dependencias | Mantenimiento y escalado complejo |
| Grabación incluida | Calidad depende de infraestructura |

#### Decisión: Daily.co (para implementación futura)

**Justificación:**
- Las videollamadas son punto de extensión, no MVP
- Daily.co ofrece la integración más rápida cuando se implemente
- 10.000 minutos/mes gratuitos son suficientes para fase inicial
- Para grabación (RF-49): evaluar Jitsi self-hosted o pagar tier de Daily cuando sea necesario
- Decisión provisional, se revisará cuando la funcionalidad entre en desarrollo

---

### 9.4 Chatbot e IA (Punto de Extensión)

El chatbot y asistente IA está definido como punto de extensión futuro para preparar reuniones, responder dudas y generar preguntas tipo test.

#### Alternativas Consideradas

| Solución | Descripción |
|----------|-------------|
| **OpenAI API (GPT)** | API de modelos GPT-4/GPT-4o |
| **Anthropic Claude** | API del modelo Claude |
| **Google Gemini** | API de Gemini de Google |
| **Modelos Open Source (Llama, Mistral)** | Self-hosted con Ollama o similar |

#### Análisis Comparativo

| Criterio | OpenAI | Claude | Gemini | Open Source |
|----------|--------|--------|--------|-------------|
| Coste | $0.01-0.03/1k tokens | $0.008-0.024/1k tokens | $0.0005-0.002/1k tokens | Gratis (infra propia) |
| Calidad respuestas | Alta | Alta | Alta | Variable |
| Límites tier gratuito | $5 crédito inicial | Trial limitado | Tier gratuito generoso | N/A |
| Integración | SDK Java disponible | SDK Java disponible | SDK Java disponible | Requiere infraestructura |
| Latencia | Baja | Baja | Baja | Depende de hardware |

#### Riesgos y Ventajas

**OpenAI API**
| Ventajas | Riesgos |
|----------|---------|
| Modelo líder en calidad | Costes pueden escalar con uso |
| Amplia documentación y comunidad | Dependencia de tercero |
| Function calling para integraciones | Políticas de uso pueden cambiar |
| Fine-tuning disponible | |

**Google Gemini**
| Ventajas | Riesgos |
|----------|---------|
| Tier gratuito muy generoso | API menos madura que OpenAI |
| Buena integración con ecosistema Google | Documentación aún en desarrollo |
| Costes competitivos | |

#### Decisión: OpenAI API (para implementación futura)

**Justificación:**
- El chatbot es punto de extensión, no MVP
- OpenAI tiene el ecosistema más maduro y documentado
- Function calling permite integración con Classroom para generar tests
- Se evaluará Google Gemini como alternativa de coste si el uso es alto
- Decisión provisional, se revisará cuando la funcionalidad entre en desarrollo

---

### 9.5 Mapas e Integración Geoespacial

Los meetings pueden mostrarse en un mapa (RF-39, RF-40, RF-47).

#### Alternativas Consideradas

| Solución | Descripción |
|----------|-------------|
| **Google Maps API** | API de mapas de Google |
| **Mapbox** | Plataforma de mapas personalizable |
| **OpenStreetMap + Leaflet** | Solución open source |

#### Análisis Comparativo

| Criterio | Google Maps | Mapbox | OSM + Leaflet |
|----------|-------------|--------|---------------|
| **Experiencia equipo** | **Ninguna** | Ninguna | Ninguna |
| Coste | $200 crédito/mes gratis | 50k cargas/mes gratis | Gratis |
| Calidad datos | Alta | Alta | Variable por zona |
| POIs (bibliotecas, etc.) | Sí | Limitado | Limitado |
| Integración React | Alta (librerías maduras) | Alta | Media |
| Geocoding | Incluido | Incluido | Requiere servicio externo |
| Documentación | Excelente | Buena | Variable |

#### Riesgos y Ventajas

**Google Maps API**
| Ventajas | Riesgos |
|----------|---------|
| Documentación extensa y ejemplos | Sin experiencia previa del equipo |
| $200/mes de crédito gratuito | Requiere aprender la API y configuración |
| Datos de POIs (bibliotecas, cafeterías) | Posibles costes si se excede el tier gratuito |
| Librerías React maduras (@react-google-maps/api) | Configuración de API Key y restricciones |
| Geocoding y Places incluidos | |

#### Decisión: Google Maps API

**Justificación:**
- $200 de crédito mensual gratuito cubre el uso esperado
- Datos de POIs (bibliotecas, espacios de estudio) más completos
- Mejor documentación que alternativas (importante dado que el equipo no tiene experiencia)
- Librería `@react-google-maps/api` bien mantenida con buenos tutoriales
- Consistente con otras integraciones Google (Classroom)
- Geocoding y Places API incluidos

**Nota sobre experiencia**: Aunque el equipo no ha trabajado con Google Maps API, se considera riesgo medio (no alto) porque:
- La documentación de Google es excelente
- La librería de React tiene muchos ejemplos
- Es una integración aislada (no afecta al core de la app)
- Se puede implementar progresivamente

---

### 9.6 Integración con Google Classroom

Es requisito MVP integrar Google Classroom para gestión de material y tareas (RF-50, RF-07, RF-15).

#### Consideraciones Técnicas

| Aspecto | Detalle |
|---------|--------|
| Autenticación | OAuth 2.0 con scopes de Classroom |
| API | Google Classroom API v1 |
| SDK | Google API Client Library for Java |
| Scopes necesarios | classroom.courses, classroom.coursework, classroom.rosters |

#### Riesgos y Ventajas

| Ventajas | Riesgos |
|----------|---------|
| API oficial bien documentada | Requiere verificación de app para producción |
| SDK oficial para Java | Límites de cuota (10k peticiones/100s) |
| OAuth 2.0 estándar | Proceso de verificación puede tardar semanas |
| Sin coste de uso | Cambios en API requieren adaptación |

#### Decisión: Google Classroom API con OAuth 2.0

**Justificación:**
- Es la única opción para integrar con Google Classroom
- SDK oficial para Java disponible
- Spring Security OAuth2 facilita la implementación
- Iniciar proceso de verificación de app temprano para evitar retrasos

---

## 10. Resumen de Decisiones

| Categoría | Decisión | Justificación Principal |
|-----------|----------|------------------------|
| **Frontend** | React + JavaScript | Experiencia del equipo, configuración sencilla |
| **Backend** | Java + Spring Boot | Experiencia del equipo, seguridad robusta |
| **Base de Datos** | PostgreSQL | Soporte geoespacial, integridad referencial |
| **PaaS Principal** | Azure | $100 créditos estudiantes, ecosistema completo |
| **CI/CD** | GitHub Actions | Integración nativa con repo, gratuito |
| **Contenedores** | Docker | Consistencia entre entornos |
| **Build Backend** | Maven | Estándar Spring, integración SonarQube |
| **Build Frontend** | npm | Estándar React |
| **Email** | Azure Communication Services | Integración nativa con Azure |
| **Almacenamiento** | Azure Blob Storage | Consistencia con plataforma principal |
| **Pasarela de Pagos** | Stripe | Stripe Connect para pagos a profesores, suscripciones |
| **Chat Tiempo Real** | WebSocket (Spring STOMP) | Sin costes adicionales, control total |
| **Mapas** | Google Maps API | $200 crédito/mes, POIs completos |
| **Google Classroom** | API oficial + OAuth 2.0 | Única opción, SDK Java disponible |
| **Videollamadas** | Daily.co (futuro) | Integración rápida, 10k min/mes gratis |
| **Chatbot/IA** | OpenAI API (futuro) | Ecosistema maduro, function calling |

---

## 11. Análisis de Riesgos del Equipo

### 11.1 Matriz de Experiencia del Equipo

| Tecnología | Experiencia | Nivel | Riesgo |
|------------|-------------|-------|--------|
| React + JavaScript | Sí | Alta | Bajo |
| Spring Boot + Java | Sí | Alta | Bajo |
| PostgreSQL | Sí | Alta | Bajo |
| Docker | Sí | Media | Medio-Bajo |
| GitHub Actions | Sí | Media | Medio-Bajo |
| Maven | Sí | Alta | Bajo |
| **Azure (todos los servicios)** | No | Ninguna | **Alto** |
| Stripe API | Sí | Media | Medio-Bajo |
| **Google Maps API** | No | Ninguna | **Medio** |
| Google Classroom API | No | Baja | Medio |
| WebSocket/STOMP | Parcial | Baja | Medio |

### 11.2 Análisis Detallado por Categoría de Riesgo

#### Riesgo Alto: Azure

**Situación**: Ningún miembro del equipo ha trabajado con Azure anteriormente. Es la única tecnología del stack donde el equipo parte de cero.

**Problemas potenciales**:

| Problema | Probabilidad | Impacto | Descripción |
|----------|--------------|---------|-------------|
| Configuración incorrecta de App Service | Alta | Alto | Variables de entorno mal configuradas, puertos incorrectos, problemas con Docker |
| Problemas de conectividad con PostgreSQL | Alta | Alto | Firewall rules, connection strings, SSL certificates |
| Fallos en pipeline CI/CD | Alta | Alto | Service Principal mal configurado, permisos RBAC insuficientes |
| Gasto excesivo de créditos | Media | Alto | No configurar alertas, dejar recursos encendidos innecesariamente |
| Tiempo de resolución de problemas elevado | Alta | Medio | Desconocimiento de herramientas de diagnóstico de Azure |
| Conflictos de recursos entre 2 Web Apps | Media | Medio | Ambas apps comparten 1 vCPU y 1.75 GB en plan B1, riesgo de saturación |

**Mitigaciones implementadas**:

| Mitigación | Estado | Responsable |
|------------|--------|-------------|
| Reservar 25 horas extra en Sprint 1 para curva de aprendizaje | Planificado | Squad Arquitectura |
| Documentar cada paso de configuración | Pendiente | Squad Arquitectura |
| Configurar alertas de coste desde el día 1 | Pendiente | Squad Arquitectura |
| Crear entorno de prueba antes de staging/prod | Pendiente | Squad Arquitectura |
| Tener Plan B con Render preparado | Documentado | Squad Arquitectura |

**Recursos de aprendizaje recomendados**:

| Recurso | Tipo | Duración | URL |
|---------|------|----------|-----|
| Azure Fundamentals (AZ-900) | Curso gratuito | 4-6 horas | Microsoft Learn |
| Deploy Spring Boot to Azure | Tutorial | 1-2 horas | learn.microsoft.com |
| GitHub Actions + Azure | Tutorial | 1 hora | docs.github.com |
| Azure for Students | Documentación | 30 min | azure.microsoft.com/students |

#### Riesgo Medio: Google Maps API

**Situación**: El equipo no ha trabajado con Google Maps API anteriormente. Necesario para mostrar meetings en mapa (RF-39, RF-40, RF-47).

**Problemas potenciales**:

| Problema | Probabilidad | Impacto |
|----------|--------------|--------|
| Configuración de API Key y restricciones | Media | Medio |
| Integración con React (react-google-maps) | Media | Medio |
| Geocoding y Places API | Media | Bajo |
| Costes si se excede tier gratuito ($200/mes) | Baja | Medio |

**Mitigaciones**:
- Usar librería `@react-google-maps/api` (bien documentada)
- Restringir API Key por dominio para evitar uso no autorizado
- Implementar caché de geocoding para reducir llamadas
- Monitorear uso en Google Cloud Console

**Recursos de aprendizaje**:
| Recurso | Duración |
|---------|----------|
| Google Maps Platform Documentation | 2-3 horas |
| react-google-maps tutorial | 1-2 horas |
| Codelabs de Google Maps | 1-2 horas |

#### Riesgo Medio: Google Classroom API

**Situación**: El equipo no ha integrado Google Classroom anteriormente. Requiere OAuth 2.0 y verificación de app.

**Problemas potenciales**:

| Problema | Probabilidad | Impacto |
|----------|--------------|--------|
| Proceso de verificación de app tarda semanas | Alta | Alto - Bloquea funcionalidad |
| Scopes OAuth2 insuficientes | Media | Medio |
| Límites de cuota (10k peticiones/100s) | Baja | Bajo |

**Mitigaciones**:
- Iniciar proceso de verificación de app en Sprint 2 (no esperar a tener el código listo)
- Usar cuenta de servicio para testing mientras se verifica
- Implementar caché de datos de Classroom para reducir peticiones

#### Riesgo Medio: WebSocket/STOMP

**Situación**: Experiencia parcial del equipo. Spring STOMP es conocido pero no en producción.

**Problemas potenciales**:

| Problema | Probabilidad | Impacto |
|----------|--------------|--------|
| Gestión de sesión WebSocket | Media | Medio |
| Escalabilidad con múltiples instancias | Baja | Alto |
| Reconexiones y heartbeats | Media | Medio |

**Mitigaciones**:
- Empezar con implementación básica single-instance
- Redis como message broker si se necesita escalar
- Documentar para migrar a Pusher/Ably si hay problemas

#### Riesgo Bajo: React, Spring Boot, PostgreSQL

**Situación**: El equipo tiene experiencia sólida con estas tecnologías.

**Ventajas de esta base sólida**:
- Permite dedicar tiempo a aprender Azure sin retrasos en desarrollo core
- Debugging y troubleshooting más rápido
- Estimaciones de tiempo más precisas
- Menos errores en código de negocio
- Onboarding de nuevos miembros más fácil

### 11.3 Impacto en Timeline del Proyecto

| Sprint | Tecnología crítica | Riesgo | Tiempo buffer |
|--------|-------------------|--------|---------------|
| Sprint 1 | Azure (App Service, PostgreSQL, CD) | Alto | +25 horas |
| Sprint 2 | Azure (PostgreSQL avanzado, monitoring) | Medio | +8 horas |
| Sprint 3 | Azure (optimización, costes) | Bajo | +4 horas |
| Sprint N | Google Maps API | Medio | +6 horas |
| Sprint N | Google Classroom API | Medio | +10 horas |
| Sprint N | WebSocket/STOMP | Medio | +6 horas |

### 11.4 Estrategia de Reducción de Riesgos

```
┌───────────────────────────────────────────────────────────┐
│           ESTRATEGIA DE REDUCCIÓN DE RIESGOS              │
├───────────────────────────────────────────────────────────┤
│                                                           │
│  1. CONOCIMIENTO SÓLIDO (React, Spring, PostgreSQL)       │
│     └──→ Desarrollo core sin bloqueos                     │
│     └──→ Estimaciones precisas                            │
│                                                           │
│  2. APRENDIZAJE FOCALIZADO (Azure)                        │
│     └──→ Sprint 1 dedicado a infraestructura              │
│     └──→ Documentar cada configuración                    │
│     └──→ Pair programming en tareas Azure                 │
│                                                           │
│  3. PLAN DE CONTINGENCIA LISTO (Render)                   │
│     └──→ El equipo YA conoce Render                       │
│     └──→ Migración posible en <4 horas si falla Azure     │
│                                                           │
│  4. INTEGRACIONES PROGRESIVAS                             │
│     └──→ Google Classroom: Iniciar verificación pronto    │
└───────────────────────────────────────────────────────────┘
```

---

## 12. Plan de Contingencia

### 12.1 Escenario: Agotamiento de Créditos Azure

**Trigger:** Créditos de Azure se agotan antes de finalizar el proyecto o surgen problemas técnicos con Azure.

**Plan B - Stack Alternativo:**

| Componente | Alternativa | Justificación |
|------------|-------------|---------------|
| **Frontend Hosting** | Render (Free tier) | Despliegue React gratuito, SSL incluido |
| **Backend Hosting** | Render (Free tier) | Soporta Docker, despliegue desde GitHub |
| **Base de Datos** | Neon / Filess.io | PostgreSQL serverless con tier gratuito |
| **Almacenamiento** | AWS S3 / Cloudflare R2 | R2 sin egress fees, S3 tier gratuito |
| **Email** | Resend / SendGrid | APIs externas con tier gratuito |

### 12.2 Mitigaciones para Render

| Problema | Mitigación |
|----------|------------|
| Hibernación por inactividad | Implementar cron job externo (cron-job.org) para ping cada 14 minutos |
| Persistencia de BD en free tier | Usar Neon (750 horas/mes gratis) o Filess.io como BD externa |
| Problemas con puertos SMTP | Usar API REST de Resend o SendGrid en lugar de SMTP directo |
| Cold start (~30s) | Aceptable para MVP, optimizar con lazy loading |

### 12.3 Comparativa BD Alternativas

| Servicio | Tier Gratuito | Limitaciones |
|----------|---------------|--------------|
| **Neon** | 0.5 GB storage, 750 horas compute/mes | Sin backups automáticos en free |
| **Filess.io** | 100 MB storage | Muy limitado para producción |
| **Supabase** | 500 MB storage | Pausa tras 1 semana inactividad |
| **PlanetScale** | 5 GB storage | Solo MySQL (requiere migración) |

**Recomendación de Plan B para BD:** Neon por balance entre capacidad y fiabilidad.

### 12.4 Comparativa Almacenamiento Alternativo

| Servicio | Tier Gratuito | Ventajas |
|----------|---------------|----------|
| **AWS S3** | 5 GB, 12 meses | Muy establecido, SDKs maduros |
| **Cloudflare R2** | 10 GB, sin egress fees | Sin costes de salida de datos |
| **Backblaze B2** | 10 GB | Económico, compatible S3 |

**Recomendación de Plan B para Storage:** Cloudflare R2 por el tier gratuito generoso y sin costes de egress.

### 12.5 Servicios de Email Alternativos

| Servicio | Tier Gratuito | Integración |
|----------|---------------|-------------|
| **Resend** | 3000 emails/mes | API REST moderna, fácil integración |
| **SendGrid** | 100 emails/día | API REST, amplia documentación |
| **Mailgun** | 5000 emails/mes (3 meses) | API REST |

**Recomendación de Plan B para Email:** Resend por su API moderna y generoso tier gratuito.

---

## 13. Conclusiones

El stack tecnológico seleccionado prioriza la **experiencia previa del equipo** como factor principal, lo que permitirá:

1. **Reducir el tiempo de desarrollo** al no requerir curvas de aprendizaje pronunciadas
2. **Minimizar riesgos técnicos** al usar tecnologías conocidas
3. **Facilitar el onboarding** de nuevos miembros del equipo

La elección de **Azure** como plataforma principal proporciona un ecosistema completo que cubre todas las necesidades del proyecto (hosting, base de datos, almacenamiento, email) con los créditos gratuitos para estudiantes.

Las **integraciones externas** seleccionadas cubren todos los requisitos funcionales:
- **Stripe Connect** para el modelo de pagos a profesores con comisión
- **Google Classroom API** para la gestión de material y tareas
- **Google Maps API** para la visualización de meetings en mapa
- **WebSocket con Spring STOMP** para chat en tiempo real sin costes adicionales

Los **puntos de extensión** (videollamadas con Daily.co, chatbot con OpenAI) están documentados con decisiones provisionales que se revisarán cuando entren en desarrollo.

El **Plan B con Render + Neon + Cloudflare R2 + Resend** está documentado y listo para implementarse si los créditos de Azure se agotan o surgen problemas técnicos inesperados.

### Diagrama de Arquitectura Resumido

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              CLIENTE                                    │
│                         (Navegador Web)                                 │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       FRONTEND (React + JS)                             │
│                    Azure Static Web Apps / Render                       │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    BACKEND (Spring Boot + Java)                         │
│                      Azure App Service / Render                         │
│  ┌─────────┬─────────┬─────────┬─────────┬─────────┬─────────┐          │
│  │  Auth   │ Groups  │ Meeting │ Billing │  Chat   │  Integ  │          │
│  │ OAuth2  │         │  Maps   │ Stripe  │WebSocket│Classroom│          │
│  └─────────┴─────────┴─────────┴─────────┴─────────┴─────────┘          │
└─────────────────────────────────────────────────────────────────────────┘
       │           │           │           │           │           │
       ▼           ▼           ▼           ▼           ▼           ▼
┌───────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
│PostgreSQL │ │  Blob   │ │  Email  │ │ Stripe  │ │ Google  │ │ Google  │
│ Azure DB  │ │ Storage │ │  Azure  │ │ Connect │ │  Maps   │ │Classroom│
│  / Neon   │ │Azure/R2 │ │ /Resend │ │         │ │   API   │ │   API   │
└───────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘

                     EXTENSIONES FUTURAS (No MVP)
              ┌─────────────────┬─────────────────┐
              │    Daily.co     │   OpenAI API    │
              │  Videollamadas  │  Chatbot / IA   │
              └─────────────────┴─────────────────┘
```

---
