# Título del Documento

## Subtítulo (si aplica)

### Grupo 9 – Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Tipo de documento (Acta / Plan de proyecto / Arquitectura / Normativa / Desarrollo)  
**Sprint:** Sprint X  
**Semana:** Semana X  
**Estado:** Borrador / En revisión / Aprobado  
**Fecha:** DD/MM/YYYY  
**Autor(es):** Nombre(s)

---

## Índice
1. [Introducción](#1-introduccíon)
2. [Líneas de Ingresos](#2-líneas-de-Ingresos)
3. [Estrategia de Precios](#3-Estrategia-de-Precios)
4. [Presupuesto del Proyecto y Estado Actual](#4-Presupuesto-del-Proyecto-y-Estado-Actual)
5. [Rentabilidad](#5-Rentabilidad)
6. [Escenarios financieros](#6-Escenarios-financieros)
7. [Plan de Implementación de Pagos](#7-Plan-de-Implementación-de-Pagos)

---

## 1. Introduccíon
El modelo de negocio de MeerKatters se centra en democratizar el acceso a la educación colaborativa, asegurando al mismo tiempo la sostenibilidad financiera del proyecto a través de un modelo *freemium*. Este modelo se basará en proporcionar a los usuarios de la aplicación las funcionalidades básicas para que puedan realizar la educación colaborativa de forma gratuita, mientras que podrán optar por una versión de pago mediante la cual obtendrán funciones que aportarán valor añadido según el tipo de usuario final. Además, cuando un profesor realice el cobro de una clase a través de nuestro servicio, se le aplicará una comisión por esta. Finalmente, negociaremos con pequeñas academias la venta del modelo premium de nuestro software mediante planes con tarifas reducidas.

El presente documento tiene como objetivo principal definir cómo MeerKatters logrará financiarse, alcanzar la sostenibilidad y generar un margen de beneficio real. Para ello, estudiaremos en detalle qué hitos, métricas y volumen de usuarios harán falta para que el proyecto supere sus costes de desarrollo y mantenimiento, convirtiéndose en un producto rentable.

## 2. Líneas de Ingresos
*Defubur exactamente por dónde entra dinero teniendo en cuenta los dos perfiles de tu lista de usuarios pilotos:*
    *- Monetización orientada a Alumnos: pagos por clases, accesos a comunidades premium...*
    *- Monetización orientada a Profesores/Creadores: comisión que se les retiene, suscripción para destacar su perfil*

## 3. Estrategia de Precios
*Ponerle cifras a las líneas de ingresos basándote en la disposición a pagar.*
    *- Estructura de precios o Tiers (Gratis vs. Premium).*
    *- Justificación de los precios frente a la competencia (referenciando brevemente tu "Análisis de Competencias", por ejemplo, cómo mejoras el "Pase Alumn@" de Superprof).*

## 4. Presupuesto del Proyecto y Estado Actual
*Aquí es donde respondes a la pregunta de cuánto va a costar hacer la app y cuánto lleváis gastado.*

*Presupuesto Total Estimado (4 Sprints): * Debes resumir cuánto cuesta desarrollar MeerKatters. Esto incluye el coste de las horas de trabajo del equipo (aunque seáis estudiantes, poned un precio por hora teórico a vuestro trabajo, ej: 5 desarrolladores x 10h/semana x 4 Sprints x 15€/h), licencias, marketing inicial y la infraestructura de Azure.*

*Gasto Acumulado hasta la fecha (Burn Rate): * Cuánto de ese presupuesto ya os habéis "fundido" hasta el Sprint actual (Sprint 0 / Sprint DP).*

*Nota: Aquí puedes mencionar que de momento los servidores en Azure os están saliendo gratis gracias a los 100$ de crédito de estudiantes, lo que reduce el gasto real inicial.*

## 5. Rentabilidad
*"cuánto dinero necesitamos ganar para superar el presupuesto y ser rentables".*

*Cálculo del Punto de Equilibrio Operativo (Mensual): * Cuánto necesitáis facturar al mes solo para mantener la app viva (cubrir los ~23.50€/mes de Azure + pasarelas de pago + mantenimiento).*

*Ejemplo práctico que le gustará al profesor: "Si cobramos una suscripción de 5€ al mes a los profesores, necesitamos 5 profesores activos al mes para pagar los servidores".*

*Cálculo de Recuperación de la Inversión (ROI): * Teniendo en cuenta el Presupuesto Total (4 Sprints) del apartado anterior, ¿cuántos meses tardaréis en recuperar todo el dinero invertido en desarrollarlo?*

*Ejemplo: "Si el proyecto entero cuesta 3.000€ desarrollarlo, y nuestro beneficio mensual estimado es de 500€, tardaremos 6 meses en ser rentables desde el lanzamiento".*

## 6. Escenarios financieros
*Escenario Realista basado en Pilotos: * Usando tu documento "Lista de Usuarios Pilotos.md", donde tienes a profesores (ej. Paloma, Emma, Carlos) y alumnos.*

*"Si de nuestra lista de pilotos conseguimos que el 30% pague la cuota mensual desde el primer mes, ingresaríamos X€. Esto significa que cubriríamos los costes de Azure y amortizaríamos un Y% del presupuesto de desarrollo".*

## 7. Plan de Implementación de Pagos

*Como es un proyecto de desarrollo, el análisis de monetización debe aterrizarse a nivel técnico y legal.*
    *-  Pasarela de pago elegida (Stripe, PayPal, etc.) y sus comisiones.*
    *- Gestión de pagos a terceros (cómo se le pagará a los profesores).*
    *- Fases de despliegue de la monetización (ej. Sprint X: Pagos por clase; Sprint Y: Suscripciones).*

 