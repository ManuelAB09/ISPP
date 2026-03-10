# Plan de Pruebas

## Sprint S2

### Grupo 9 – Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Entrega  
**Sprint:** Sprint S2  
**Semana:** Semana 1  
**Estado:** Aprobado  
**Fecha:** 10/03/2026  
**Autor(es):** Alejandro Soult Toscano

---

## Índice

1. [Introducción](#1-introducción)  
2. [Plan de Pruebas: Backend](#2-plan-de-pruebas-backend)  
3. [Plan de Pruebas: Frontend](#3-plan-de-pruebas-frontend)  
4. [Plan de Pruebas: Integración Backend + Frontend](#4-plan-de-pruebas-integración-backend--frontend)  
5. [Cobertura mínima](#5-cobertura-mínima)  

---

## 1. Introducción

Este documento describe la planificación de pruebas del proyecto **MeerKatters**, estableciendo la estrategia que seguirá el equipo para garantizar la calidad del software desarrollado a lo largo de los distintos sprints del proyecto.

El objetivo de este plan es definir **cuándo**, **qué** y **cómo** se realizarán las pruebas en las diferentes capas del sistema: backend, frontend e integración completa. De esta forma, se busca detectar errores de manera temprana, validar el correcto funcionamiento de las funcionalidades implementadas y asegurar que el producto final cumple con los requisitos definidos.

La estrategia de pruebas se organiza siguiendo el ciclo de desarrollo iterativo del proyecto, integrando la ejecución de tests dentro del propio flujo de trabajo de cada sprint. Esto permite mantener un control continuo de la calidad del código y evitar la acumulación de errores hacia las fases finales del desarrollo.

Sin embargo, esta organización es flexible, permitiendo su modificación para adaptarse al tiempo y al alcance del sprint.

---

## 2. Plan de Pruebas: Backend

Las pruebas del backend se centran principalmente en **tests unitarios** sobre la lógica de negocio y en **tests de controladores** que validen el comportamiento de las API expuestas.

La planificación se estructura de la siguiente manera:

- **Mitad de Sprint:**  
  Se realizarán **tests unitarios** sobre las funcionalidades implementadas hasta ese momento.  
  Estas pruebas se centrarán principalmente en las capas de **Servicio** y **Repositorio**, verificando la lógica de negocio y el correcto acceso y manipulación de los datos.

- **Final de Sprint:**  
  Se ampliará el conjunto de pruebas incluyendo:
  - **Tests unitarios completos** sobre todo el resto del código desarrollado en el sprint.
  - **Tests de controladores** sobre todo el código, utilizando **mocks** para simular dependencias del sistema.

---

## 3. Plan de Pruebas: Frontend

Las pruebas del frontend se centran en validar el correcto funcionamiento de los componentes de la interfaz y su interacción con los datos proporcionados por el backend.

La estrategia de pruebas consiste en:

- **Final de cada Sprint:**  
  Se realizarán **tests sobre componentes React**, usando **respuestas JSON mockeadas** para simular las respuestas de la API.

---

## 4. Plan de Pruebas: Integración Backend + Frontend

Las pruebas de integración tienen como objetivo validar el funcionamiento completo del sistema cuando **frontend y backend interactúan conjuntamente**.

Estas pruebas se realizarán principalmente en las fases finales del desarrollo:

- **En la segunda mitad del Sprint 3:**
  Se llevarán a cabo **tests de interfaz de usuario (UI)** que simulan el comportamiento real de un usuario utilizando la aplicación. Para ello se empleará la herramienta **Selenium**, que permitirá automatizar interacciones con la interfaz web y comprobar que los flujos principales del sistema funcionan correctamente.

Estas pruebas permitirán validar escenarios completos de uso del sistema, verificando que las funcionalidades implementadas funcionan correctamente en un entorno cercano al de producción.

---

## 5. Cobertura mínima

Como criterio de calidad del proyecto, se establece una **cobertura mínima del 70 % del código** mediante tests automatizados. Esta será monitorizada mediante herramientas de análisis de calidad integradas en el flujo de desarrollo del proyecto, permitiendo identificar fácilmente las áreas del código que requieren mayor nivel de pruebas.