# Integración Continua y Despliegue Continuo (CI/CD)

## MeerKatters - Plataforma de Comunidades de Estudio

### Grupo 9 – Turno de tarde

---

**Proyecto:** MeerKatters
**Documento:** Arquitectura / DevOps
**Sprint:** Sprint DP
**Semana:** Semana 3
**Estado:** Aprobado
**Fecha:** 17/02/2026
**Autor(es):** Raimundo Jiménez Lara, Manuel María Calderón Rodríguez

---

## Índice

1. [Introducción](#1-introducción)
2. [Visión general del pipeline](#2-visión-general-del-pipeline)
3. [Hooks locales](#3-hooks-locales-pre-commit--commit-msg)
4. [Integración Continua (CI)](#4-integración-continua-ci)
5. [Despliegue Continuo (CD)](#5-despliegue-continuo-cd)
6. [Dependabot](#6-dependabot)
7. [Validación de nombres de rama](#7-validación-de-nombres-de-rama)
8. [Plantillas de Issues y Pull Requests](#8-plantillas-de-issues-y-pull-requests)
9. [Configuración necesaria](#9-configuración-necesaria)
10. [Estructura de archivos](#10-estructura-de-archivos)
11. [Supuestos y ajustes](#11-supuestos-y-ajustes)
12. [Problemas conocidos](#12-problemas-conocidos)

---

## 1. Introducción

Este documento describe la estrategia completa de CI/CD del proyecto **MeerKatters**. El pipeline garantiza la calidad del código en cada push, valida estándares del equipo, ejecuta tests y despliega automáticamente a Azure.

**Herramientas principales:**

| Herramienta | Función |
|-------------|---------|
| GitHub Actions | Motor de CI/CD |
| Docker | Containerización del backend |
| GitHub Container Registry (ghcr.io) | Registro de imágenes Docker |
| Azure App Service B1 (1 plan, 2 Web Apps) | Backend staging y producción, ambos con Always On |
| Azure Static Web Apps | Frontend staging y producción (gratis) |
| Azure PostgreSQL Flexible | BD compartida con 2 schemas |

**Modelo de doble verificación:**
1. **Local** — Git hooks (`commit-msg` + `pre-commit`) validan Conventional Commits y ejecutan linters antes del commit.
2. **Remota** — GitHub Actions ejecuta la suite completa de checks, tests y análisis de calidad en cada push.

---

## 2. Visión general del pipeline

```
┌──────────────────────────────────────────────────────────────────────┐
│                        DESARROLLO LOCAL                              │
│                                                                      │
│  git add + git commit                                                │
│       ├── pre-commit hook ──→ ESLint (frontend) + Checkstyle (back)  │
│       │      Si falla → commit rechazado                             │
│       └── commit-msg hook ──→ Valida Conventional Commits            │
│              Si falla → commit rechazado                             │
│       │                                                              │
│       ▼                                                              │
│  git push                                                            │
└──────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    CI — GitHub Actions (CI.yml)                      │
│                                                                      │
│  Conventional Commits ──→ Lint Backend + Lint Frontend               │
│                              │                 │                     │
│                              ▼                 ▼                     │
│                        Tests Backend    Tests Frontend               │
│                       (JUnit+JaCoCo     (Jest+coverage)              │
│                        +PostgreSQL)                                  │
│                              │                 │                     │
│                              └────────┬────────┘                     │
│                                       ▼ (solo trunk/main)            │
│                                   SonarQube                          │
└──────────────────────────────────────────────────────────────────────┘
          │ CI exitoso
          ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    CD — GitHub Actions                               │
│                                                                      │
│  trunk ──→ CD_staging.yml ──→ Docker → GHCR → Azure Web App Staging  │
│  main  ──→ CD_production.yml → Docker → GHCR → Azure Web App Prod    │
└──────────────────────────────────────────────────────────────────────┘
```

### Estrategia de ramas

| Rama | Entorno | Despliegue |
|------|---------|------------|
| `feature/*`, `bugfix/*`, `hotfix/*`, `doc/*`, `test/*` | — | Solo CI (sin despliegue) |
| `trunk` | Staging | CI + CD automático → Web App staging (B1) |
| `main` | Producción | CI + CD automático → Web App producción (B1) |

---

## 3. Hooks locales (pre-commit / commit-msg)

Los hooks se ejecutan **localmente** y proporcionan feedback inmediato sin esperar al pipeline remoto.

| Hook | Archivo | Función |
|------|---------|---------|
| `pre-commit` | `.githooks/pre-commit` | Ejecuta ESLint (frontend) y Checkstyle (backend) sobre archivos staged |
| `commit-msg` | `.githooks/commit-msg` | Valida Conventional Commits |

### Activación (una vez por desarrollador)

```bash
git config core.hooksPath .githooks
```

### Formato de commits: Conventional Commits

```
<tipo>(<scope opcional>): <descripción>
```

**Tipos permitidos:**

| Tipo | Uso |
|------|-----|
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de errores |
| `docs` | Cambios en documentación |
| `style` | Formato, estilo (sin alterar lógica) |
| `refactor` | Reestructuración sin cambiar funcionalidad |
| `perf` | Mejora de rendimiento |
| `test` | Añadir o modificar pruebas |
| `build` | Sistema de compilación o dependencias |
| `ci` | Cambios en CI/CD |
| `chore` | Tareas auxiliares de mantenimiento |
| `revert` | Revertir un commit anterior |

**Ejemplos:**
```bash
feat: añade búsqueda de comunidades
fix(auth): corrige validación de tokens JWT
refactor(groups)!: cambia estructura de módulo  # con BREAKING CHANGE footer
```

**Reglas adicionales del hook:**
- Si hay cuerpo, debe haber línea en blanco entre título y cuerpo.
- Si se usa `!` (breaking change), debe incluirse un footer `BREAKING CHANGE: <descripción>`.
- Advertencia (no bloqueante) si el subject supera 50 caracteres o termina en punto.

> Consultar la [Guía de Git Hooks](Guia_Desarrolladores_GitHooks.md) para detalles de instalación, troubleshooting y desactivación temporal.

---

## 4. Integración Continua (CI)

**Workflow:** `.github/workflows/CI.yml`
**Trigger:** Push a `main`, `trunk`, `feature/**`, `bugfix/**`, `hotfix/**`, `doc/**`, `test/**`

### Jobs

| Job | Descripción | Depende de |
|-----|-------------|------------|
| `conventional-commits` | Valida mensajes de commit del push | — |
| `lint-backend` | Checkstyle sobre código Java | — |
| `lint-frontend` | ESLint sobre código JS/React | — |
| `test-backend` | JUnit + JaCoCo + PostgreSQL 16. Cobertura mínima: **70%** | `lint-backend` |
| `test-frontend` | Jest + cobertura | `lint-frontend` |
| `sonarqube` | Análisis de calidad (solo en trunk/main) | `test-backend` + `test-frontend` |

### Detalles relevantes

- **Conventional Commits:** Script bash que valida todos los commits del push. Permite automáticamente `Merge` y `Revert` generados por Git.
- **Checkstyle:** Usa la configuración en `config/checkstyle/checkstyle.xml` (PascalCase clases, camelCase métodos/variables, 4 espacios, 100 chars/línea, sin `import *`, llave de apertura en la misma línea). Alineado con [Gestión Equipo §10](Gestión%20Equipo.md#10-estandarización-del-código).
- **Test Backend:** Levanta PostgreSQL 16 como servicio Docker con health checks. Ejecuta `mvn verify` con perfil `test`.
- **Cobertura:** JaCoCo verifica >=70% de instrucciones. Reportes subidos como artefactos (14 días).
- **SonarQube:** Solo en `trunk` y `main`. Requiere `SONAR_TOKEN` y `SONAR_HOST_URL` como secrets (opcionales).

---

## 5. Despliegue Continuo (CD)

### Arquitectura: 1 App Service Plan B1 con 2 Web Apps

Un único plan B1 (~€11/mes) aloja **2 Web Apps** (staging y producción) que comparten los mismos recursos (1 vCPU, 1.75 GB RAM). Ambas tienen **Always On** habilitado.

```
┌─────────────────────────────────────────────────────────────────────┐
│                              AZURE                                  │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │              Azure Static Web Apps (GRATIS)                    │ │
│  │         Frontend Staging    │    Frontend Producción           │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │              App Service Plan B1 (~€11/mes)                    │ │
│  │     1 vCPU  │  1.75 GB RAM  │  Always On  │  Linux             │ │
│  │                                                                │ │
│  │  ┌─────────────────────┐  ┌─────────────────────────────────┐  │ │
│  │  │  Web App: Staging   │  │  Web App: Producción            │  │ │
│  │  │  (Docker)           │  │  (Docker)                       │  │ │
│  │  │  Always On: Si      │  │  Always On: Si                  │  │ │
│  │  └─────────────────────┘  └─────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │              PostgreSQL Flexible B1ms (~€12.50/mes)            │ │
│  │         schema staging    │    schema production               │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│                    TOTAL: ~€23.50/mes                               │
└─────────────────────────────────────────────────────────────────────┘
```

**¿Por qué 1 plan con 2 Web Apps?**
- Un App Service Plan es el "servidor" (CPU + RAM). Las Web Apps son aplicaciones dentro de él.
- Se pueden meter **tantas Web Apps como se quiera** en cualquier plan de pago (B1 incluido), sin coste adicional.
- El tier B1 no soporta **deployment slots** (requiere Standard S1 ~€58/mes), por lo que usamos 2 Web Apps independientes como alternativa.
- Ambas Web Apps tienen **Always On** habilitado (disponible en B1), eliminando cold starts.
- Con 2 Spring Boot (~300-500 MB cada uno), queda ~0.75 GB libre. Suficiente para un proyecto universitario.
- Si se necesita más capacidad, se escala a **B2** (€22/mes, 2 vCPU, 3.5 GB) sin cambiar la estructura.

> **Precios:** Linux, West Europe, EUR. B1 = €11.002/mes (fuente: Azure Pricing Calculator). Crédito Azure for Students = $100 USD ≈ €83.72.

### CD Staging (`CD_staging.yml`)

**Trigger:** CI completa exitosamente en `trunk`

| Paso | Acción |
|------|--------|
| 1 | Build imagen Docker del backend → push a GHCR con tag `:staging` |
| 2 | Login en Azure con `AZURE_CREDENTIALS` (único Service Principal) |
| 3 | Deploy a la **Web App staging** (mismo App Service Plan B1) |
| 4 | Build frontend React → deploy a Static Web App staging |

### CD Producción (`CD_production.yml`)

**Trigger:** CI completa exitosamente en `main`

| Paso | Acción |
|------|--------|
| 1 | Build imagen Docker del backend → push a GHCR con tag `:latest` |
| 2 | Login en Azure con `AZURE_CREDENTIALS` (mismo Service Principal) |
| 3 | Deploy a la **Web App producción** (App Service Plan B1, Always On) |
| 4 | Build frontend React → deploy a Static Web App producción |

### ¿Un AZURE_CREDENTIALS o dos?

**Uno solo.** Como todos los recursos (Web Apps, PostgreSQL, Static Web Apps) están en un **único Resource Group**, basta con un Service Principal con rol `contributor` sobre ese Resource Group. Ambos workflows CD usan el mismo secreto `AZURE_CREDENTIALS`.

---

## 6. Dependabot

**Archivo:** `.github/dependabot.yml`

Servicio integrado de GitHub que crea PRs automáticamente cuando hay nuevas versiones de dependencias o vulnerabilidades de seguridad.

| Ecosistema | Directorio | Frecuencia | Límite PRs |
|------------|------------|------------|------------|
| Maven | `/backend` | Semanal (lunes) | 5 |
| npm | `/frontend` | Semanal (lunes) | 5 |
| GitHub Actions | `/` | Semanal (lunes) | 5 |

- **Labels automáticos:** `dependencies` + `backend`/`frontend`/`ci`
- **Prefijos de commits:** `build(deps)` o `ci(deps)` — compatibles con Conventional Commits
- **Seguridad:** Si detecta una CVE en una dependencia, crea PR de seguridad prioritaria automáticamente (incluso fuera del ciclo semanal)

---

## 7. Validación de nombres de rama

**Workflow:** `.github/workflows/branch_naming.yml`
**Trigger:** Push a cualquier rama excepto `main`, `trunk`, `develop`

**Formato requerido:** `tipo/descripcion` o `tipo/#issue_descripcion`
**Tipos válidos:** `feature`, `bugfix`, `hotfix`, `doc`, `test`, `release`

Ejemplos válidos:
```
feature/#42_login-google
bugfix/corregir-validacion-jwt
doc/actualizar-readme
```

> **Nota sobre Gestión Equipo:** La política de ramas (§8) especifica UpperCamelCase para la descripción (ej: `feature/#123_AñadirLocalizaciónDeEvento`). El workflow permite regex más permisiva, pero el equipo debe seguir la convención de Gestión Equipo.

---

## 8. Plantillas de Issues y Pull Requests

| Plantilla | Archivo | Uso |
|-----------|---------|-----|
| Pull Request | `.github/PULL_REQUEST_TEMPLATE.md` | Descripción, tipo, issue relacionada, checklist |
| Bug Report | `.github/ISSUE_TEMPLATE/bug_report.md` | Reportar errores |
| Feature | `.github/ISSUE_TEMPLATE/feature.md` | Solicitudes técnicas |
| Task | `.github/ISSUE_TEMPLATE/task.md` | Tareas no técnicas |

---

## 9. Configuración necesaria

### 9.1 Hooks locales

```bash
git config core.hooksPath .githooks
```

### 9.2 Secretos de GitHub

Configurar en **Settings → Secrets and variables → Actions → Secrets**:

| Secret | Descripción | Obligatorio |
|--------|-------------|-------------|
| `AZURE_CREDENTIALS` | JSON del Service Principal (único para todo el Resource Group) | Sí |
| `AZURE_STATIC_WEB_APPS_TOKEN_STAGING` | Token de Static Web App staging | Sí |
| `AZURE_STATIC_WEB_APPS_TOKEN_PRODUCTION` | Token de Static Web App producción | Sí |
| `SONAR_TOKEN` | Token de SonarQube | Opcional |
| `SONAR_HOST_URL` | URL del servidor SonarQube | Opcional |

> Ver [Configuración de Secretos GitHub](Configuracion_Secretos_GitHub.md) para instrucciones paso a paso.

### 9.3 Variables de entorno (GitHub)

Configurar en **Settings → Secrets and variables → Actions → Variables**:

| Variable | Valor de ejemplo | Obligatorio |
|----------|------------------|-------------|
| `AZURE_BACKEND_APP` | `meerkatters-backend` | Sí |
| `AZURE_BACKEND_APP_STAGING` | `meerkatters-backend-staging` | Sí |
| `STAGING_API_URL` | `https://meerkatters-backend-staging.azurewebsites.net` | Sí |
| `PRODUCTION_API_URL` | `https://meerkatters-backend.azurewebsites.net` | Sí |

> **Nota:** Se necesitan **2 variables de app-name** porque son Web Apps independientes (sin deployment slots).

### 9.4 Configuración del backend (Maven)

El `pom.xml` debe incluir:
- `maven-checkstyle-plugin` (3.5.0) apuntando a `../config/checkstyle/checkstyle.xml`
- `jacoco-maven-plugin` (0.8.12) con goals `prepare-agent` y `report`
- `sonar-maven-plugin` (4.0.0.4121)

### 9.5 Configuración del frontend (npm)

El `package.json` debe incluir los scripts:
```json
{
  "scripts": {
    "lint": "eslint src/",
    "test": "react-scripts test"
  }
}
```

---

## 10. Estructura de archivos

```
.githooks/
├── commit-msg              # Hook: validación Conventional Commits
└── pre-commit              # Hook: ESLint + Checkstyle sobre archivos staged

.github/
├── dependabot.yml          # Actualización automática de dependencias
├── PULL_REQUEST_TEMPLATE.md
├── ISSUE_TEMPLATE/         # Plantillas de issues (bug, feature, task)
└── workflows/
    ├── CI.yml              # Integración continua
    ├── CD_staging.yml      # Despliegue: trunk → Web App staging (B1)
    ├── CD_production.yml   # Despliegue: main → Web App producción (B1)
    └── branch_naming.yml   # Validación de nombres de rama

config/
└── checkstyle/
    └── checkstyle.xml      # Reglas de estilo Java (100 chars, §10 Gestión Equipo)
```

---

## 11. Supuestos y ajustes

Si alguno de estos supuestos cambia, hay que actualizar los archivos indicados.

| Supuesto | Valor actual | Si cambia, actualizar |
|----------|-------------|----------------------|
| Directorio backend | `backend/` | `CI.yml`, `CD_*.yml`, `.githooks/pre-commit` |
| Directorio frontend | `frontend/` | `CI.yml`, `CD_*.yml`, `.githooks/pre-commit` |
| Versión Java | 21 (Temurin) | `CI.yml` → `setup-java` |
| Versión Node.js | 24 | `CI.yml` → `setup-node` |
| PostgreSQL tests | 16 | `CI.yml` → servicio `postgres` |
| BD de test | `meerkatters_test` / `meerkatters_user` / `meerkatters_password` | `CI.yml` → `services.postgres.env` + `SPRING_DATASOURCE_*` |
| Perfil Spring Boot test | `test` | `CI.yml` → `SPRING_PROFILES_ACTIVE` |
| App Service Plan | 1 plan B1 (~€11/mes) con 2 Web Apps (staging + prod), Always On en ambas | `CD_*.yml`, `Guia_Despliegue_Azure.md` |
| Si B1 insuficiente | Escalar a B2 (~€22/mes, 2 vCPU, 3.5 GB) | Cambiar tier del plan, sin cambiar estructura |
| Si se usa H2 en tests | — | Eliminar bloque `services.postgres` y variables `SPRING_DATASOURCE_*` |
| Si se migra a Vite | — | Ajustar scripts en `package.json` o comandos en `CI.yml` |
| Si se cambia a Render | — | Reescribir steps de deploy en `CD_*.yml` |
| SonarCloud en vez de self-hosted | — | Usar `SonarSource/sonarcloud-github-action` |

---

## 12. Problemas conocidos

### [CRITICO] Falta `package-lock.json` en Frontend

El workflow CI y los Dockerfiles usan `npm ci`, que requiere `package-lock.json`. Sin este archivo:
- El cache de npm no será efectivo.
- Las builds no serán reproducibles.
- El Dockerfile del frontend fallará.

**Solución:**
```bash
cd frontend && npm install && git add package-lock.json
git commit -m "build: añade package-lock.json para builds reproducibles"
```

### [AVISO] SonarQube secrets opcionales

Los secrets `SONAR_TOKEN` y `SONAR_HOST_URL` son necesarios para el job de SonarQube. Si no están configurados, ese job fallará (solo afecta a pushes a trunk/main).

### [OK] Maven Wrapper recomendado

El Dockerfile del backend tiene fallback a `mvn`, pero se recomienda incluir el Maven Wrapper para mayor portabilidad:
```bash
cd backend && mvn -N wrapper:wrapper
git add .mvn mvnw mvnw.cmd
```

### [AVISO] Monitorización de recursos (1 plan B1, 2 apps)

Ambas Web Apps comparten 1 vCPU y 1.75 GB de RAM. Con 2 Spring Boot (~300-500 MB cada uno), debería haber margen suficiente. Monitorizar el uso de memoria con `az monitor metrics list` y escalar a B2 (€22/mes, 2 vCPU, 3.5 GB) si es necesario.

---

## Requisitos no funcionales cubiertos

| RNF | Requisito | Cómo se cubre |
|-----|-----------|---------------|
| RNF-11 | Cobertura de tests >= 70% | JaCoCo en CI con verificación automática |
| RNF-12 | Tests unitarios | JUnit + Mockito en cada push |
| RNF-13 | Tests de integración | `mvn verify` con PostgreSQL en contenedor |
| RNF-14 | Análisis SonarQube | Job dedicado en CI (trunk/main) |
| RNF-15 | Linting | Checkstyle (100 chars, §10) + ESLint en CI y pre-commit |
| RNF-16 | Pre-commit hooks | `.githooks/commit-msg` + `.githooks/pre-commit` |
| RNF-18 | Despliegue PaaS | CD automático a Azure App Service |
| RNF-19 | CI/CD | Pipeline completo con GitHub Actions |
| RNF-20 | Control de versiones | Conventional Commits validados en hook y CI |

---

*Documento unificado el 17 de febrero de 2026*
