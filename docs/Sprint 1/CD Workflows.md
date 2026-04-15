# Despliegue Continuo – Workflows por Sprint y Azure

### Grupo 9 – Turno de tarde

![Logo App](../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Autor:** Arquitectura 

---

## Índice

1. [Introducción](#1-introducción)
2. [Ramas de despliegue en Azure](#2-ramas-de-despliegue-en-azure)
3. [Workflows para ramas de sprint (Render)](#3-workflows-para-ramas-de-sprint-render)
   1. [Funcionamiento general](#31-funcionamiento-general)
   2. [Congelación de ramas](#32-congelación-de-ramas)
   3. [Neon Database](#33-neon-database)
4. [Consideraciones operativas](#4-consideraciones-operativas)
---
## 1. Introduccíon
Este documento complementa la sección de CI/CD principal explicando cómo se
configuran los *workflows* específicos que gestionan el despliegue automático
de las distintas ramas de desarrollo.

La estrategia general se basa en dos ejes:

1. **Azure**: dos entornos permanentes (staging y producción) enlazados a las
   ramas `trunk` y `main` respectivamente. Estos despliegues se usan para
   integrar continuamente los cambios del día a día y validar el comportamiento
   de la aplicación en un entorno real.
2. **Render (sprints)**: tres entornos efímeros independientes, uno por cada
   sprint (`sprint1`, `sprint2`, `sprint3`), que albergan las funcionalidades
   desarrolladas durante ese sprint y permanecen congelados una vez finalizado.

---

## 2. Ramas de despliegue en Azure

Las dos principales ramas productivas son:

* `trunk` → despliegue automático en **Web App Staging (Azure)**
* `main`  → despliegue automático en **Web App Producción (Azure)**

Ambas ramas comparten la misma base de código y el pipeline remoto se ejecuta
cada vez que se hace push. El archivo asociado a cada rama se encuentra en
`.github/workflows/`:

* `CD_staging.yml` – se dispara con `push` a `trunk`
* `CD_production.yml` – se dispara con `push` a `main`

Durante la construcción del pipeline se realiza:

1. Build de la imagen Docker del backend y push a GHCR (`:staging` o `:latest`).
2. Login a Azure con el secreto `AZURE_CREDENTIALS`.
3. Ejecución de `az webapp deploy` sobre la Web App correspondiente.
4. Compilación del frontend React y despliegue en la Static Web App asociada.

Estos entornos se mantienen siempre activos y se actualizan con cada commit
aceptado en su rama correspondiente. Son ideales para las pruebas continuas
de la aplicación mientras se desarrolla.

## 3. Workflows para ramas de sprint (Render)

Para los tres primeros sprints se han preparado flujos de despliegue separados
en Render. Cada sprint tiene una rama dedicada y un *workflow* propio:

* `sprint1` → `.github/workflows/CD_sprint1.yml`
* `sprint2` → `.github/workflows/CD_sprint2.yml`
* `sprint3` → `.github/workflows/CD_sprint3.yml`

### Funcionamiento general

1. El *trigger* es `push` a la rama correspondiente (`sprintN`).
2. Se construye la imagen Docker del backend y se publica en GHCR con etiqueta
   `:sprintN`.
3. Se actualizan los contenedores en el servicio de Render (tanto backend como
   frontends) mediante la API de Render o el CLI (`render deploy service`).
4. Se aplica cualquier variable de entorno necesaria (p.ej. `REACT_APP_API_URL`
   apuntando al backend de Render).
5. Se despliega el frontend compilado a la instancia de Render.

### Congelación de ramas

Una vez finalizado el sprint correspondiente (entregado en la demo), la rama
` sprintN` se marca como **protegida y congelada**. No se permite:

* Nuevos commits directos.
* Fusión de pull requests.

Esto asegura que el entorno de Render refleja exactamente el estado de la
aplicación al cierre del sprint y sirve como referencia histórica o para
regresiones. Si fuera necesario reactivar el entorno o rehacer el despliegue,
los scripts `CD_sprintN.yml` siguen disponibles y se puede forzar un push a la
rama congelada.

### Neon Database

Cada despliegue de sprint utiliza una base de datos **Neon** independiente:

* `sprint1` → Neon #1
* `sprint2` → Neon #2
* `sprint3` → Neon #3

Estas instancias se crean al principio del sprint y se destruyen tras el
final. Los workflows incluyen pasos para ejecutar migraciones (`Flyway`/`Liquibase`)
contra la base correspondiente.

## 4. Consideraciones operativas

* Los workflows de sprint no ejecutan SonarQube ni análisis de cobertura; su
  objetivo es únicamente el despliegue funcional.
* GitHub Actions tiene límites de ejecución; la política de congelación ayuda a
  evitar ejecuciones innecesarias sobre ramas antiguas.
* Si en algún momento se decide reutilizar el entorno de Render para un sprint
  posterior, basta con crear una rama nueva (`sprint4`) y copiar el workflow.

---

Este documento se puede actualizar con ejemplos de YAML y comandos de Render en
caso de que el equipo necesite profundizar en la configuración técnica.