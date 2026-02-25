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
El público objetivo de MeerKatters cuenta con dos tipos de perfiles principales: profesores y alumnos. Cada uno tendrá características y necesidades diferentes y, por tanto, cada uno tendrá también una disposición a pagar distinta. Por ello, analizaremos cada perfil por separado, aplicando un tipo distinto de monetización o funciones premium a cada uno.

Además, se considerará un canal de monetización adicional mediante la venta del software premium con tarifas reducidas o planes especiales a pequeñas academias, lo que permitirá ampliar la adopción y generar ingresos de tipo B2B.

---

### 2.1 Monetización orientada a Alumnos (B2C)
El enfoque para los estudiantes se basa en la mejora de su experiencia de estudio y el acceso a contenido de alto valor. Las líneas de ingresos por parte de los alumnos son:
    - Suscripción Premium (Mensual/Anual): Los alumnos podrán utilizar la aplicación de forma gratuita, sin embargo, ofrecemos un "Pase Premium" que desbloqueará:
        - Acceso a "Comunidades Premium" exclusivas
        - Capacidad de ampliar el tamaño de la comunidad privada
        - Aumento del número de comunidades creadas
        *- Otras funcionalidades futuras que no formen parte del MVP*
    - Pagos transaccionales por clases: Cuando un alumno decide contratar una clase con un profesor a través de nuestro servicio el pago se proceso de forma segura a través de nuestra aplicación, generando un flujo de dinero dentro de la plataforma

---

### 2.2 Monetización orientada a Profesores (B2B/B2C)
Los profesores y academias utilizarán MeerKatters como una herramienta de captación de clientes y gestión de sus alumnos. Para ellos, el valor reside más en la visibilidad y las herramientas de adminsitración, monetizandose en:
    - Comisión por transacción: Por cada clase que un profesor gestione a través de MeerKatters, se retendrá un pequeño porcentaje del cobro en concepto de gestión y uso de la infraestructura de pagos seguros. Esto nos asegura un ingreso variable eque escala con el volumen de uso.
    - Suscripción "Profesor Premium" (Mensual/Anual): Plan de suscripción diseño para educadores que necesiten ir más allá del uso básico. Incluye:
        - Posicionamiento destacado en las búsquedas
        - Verificación de perfil que aportará seguridad a los estudiantes
        - Ampliación del límite de miembros en las comunidades que administran
        - Capacidad de crear múltiples grupos de estudio simultáneos sin restricciones
        *- Otras funcionalidades futuras que no formen parte del MVP*


## 3. Estrategia de Precios
Nuestra estrategia de precios se basa en la sensibilidad económica de nuestro público objetivo (principalmente estudiantes) y en la necesidad de eliminar las fricciones de pago presentes en plataformas competidoras.  

El objetivo es ofrecer una estructura accesible, transparente y fácil de entender, favoreciendo la conversión y la retención.

---

### A. Para Alumnos
Teniendo en cuenta que los estudiantes cuentan con recursos limitados, el precio debe ser lo suficientemente bajo como para percibirse como una compra asumible y justificada por el rendimiento académico.

Tras un análisis del público objetivo mediante encuestas y una comparación con la competencia (especialmente Wuolah, cuyo público y propuesta son similares), se establece el siguiente modelo:

#### Plan Alumno Básico (0 €/mes)
Acceso gratuito a las funcionalidades esenciales de la plataforma.

**Limitaciones:**
- Acceso únicamente a comunidades básicas.
- Tamaño limitado de las comunidades.
- Máximo de 3 comunidades gratuitas activas.

---

#### Plan Alumno Premium (2,99 €/mes o 25,99 €/año)
Incluye funcionalidades ampliadas orientadas a una experiencia más completa:

- Creación de comunidades premium exclusivas.
- Ampliación del tamaño de las comunidades.
- Incremento del número máximo de comunidades creadas (más de 3).

El precio se ha fijado en un rango accesible para facilitar la decisión de compra y minimizar la fricción económica.

---

### B. Para Profesores
El modelo para profesores busca facilitar la entrada en la plataforma sin riesgos iniciales, permitiendo validar su demanda antes de asumir costes fijos.

#### Plan Profesor Básico (0 €/mes + 15% de comisión por clase)

Acceso a las funcionalidades esenciales para impartir clases y gestionar comunidades.

**Incluye:**
- Uso de herramientas core para profesores.

**Limitaciones:**
- Límite en el número de miembros por comunidad.
- Máximo de 3 comunidades administradas.
- Visibilidad estándar en búsquedas.

---

#### Plan Profesor Premium (5,99 €/mes o 59,99 €/año + comisión reducida al 10%)
Orientado a profesores que deseen mayor alcance y profesionalización dentro de la plataforma.

**Incluye:**
- Ampliación del número de comunidades administradas y de su capacidad.
- Verificación del perfil, aumentando la confianza para los alumnos.
- Posicionamiento destacado en resultados de búsqueda.

---

> **Nota:** La comisión correspondiente al método de pago utilizado (por ejemplo, Stripe u otro proveedor) se aplicará de forma independiente a la comisión de gestión de la plataforma.

## 4. Presupuesto del Proyecto y Estado Actual
### 4.1 Presupuesto del proyecto
Para determinar la viabilidad y el punto de equilibrio de MeerKatters, es imprescindible cuantificar el coste total del desarrollo de la plataforma a lo largo de los cuatro Sprints planificados, así como el gasto acumulado hasta la fecha.

El cálculo del presupuesto se ha realizado en el documento [Estudio del Mercado](../../Sprint%20DP/Semana%203/Estudio%20de%20Mercado.md), donde se concluye que el coste base del desarrollo asciende a 72.060 €, a los que deben añadirse los costes derivados de licencias.

En cuanto a las licencias, será necesaria la contratación de servicios en Microsoft Azure. Según el análisis de créditos realizado en el estudio, y considerando una arquitectura optimizada, el coste estimado es de 23,50 € mensuales durante aproximadamente 3,5 meses, lo que supone un total de 82,25 € en concepto de licencias.

Por tanto, el presupuesto total estimado del proyecto asciende a 72.142,25 €, incluyendo tanto los costes de desarrollo como los costes de infraestructura.

### 4.2 Estado Actual
El presupuesto total del proyecto, distribuido entre los cuatro Sprints planificados, asciende aproximadamente a 18.035 € por Sprint.

Actualmente, el equipo se encuentra al inicio del primer Sprint, tras haber completado el Sprint DP. Dado que aún no se han contabilizado todas las horas correspondientes a la primera semana del Sprint 1, el cálculo del *Burn Rate* se realizará tomando como referencia el Sprint DP. Para ello, se consideran exclusivamente las horas reales incurridas por el equipo durante dicho periodo, desglosandolas de la siguiente manera:
- Coste de personal: Durante este Sprint, el equipo ha registrado un total de 397 horas reales de trabajo, además de 311 horas correspondientes a asistencia a clase y estudio. Estas horas se han distribuido de la siguiente manera

| Equipo     | Horas     | Precio/hora | Total € |
|------------|-----------|-------------|---------|
| Líder Frontend | 14    | 30          | 420     |
| Frontend   | 83        | 18          | 1.494   |
| Líder Backend | 27     | 32          | 864     |
| Backend    | 78        | 20          | 1.560   |
| Líder Arquitectura | 38| 40          | 1.520   |
| Arquitectura | 27      | 35          | 945     |
| Líder RRSS | 18        | 25          | 450     |
| RRSS       | 16        | 18          | 288     |
| Líder Marketing | 20   | 30          | 600     |
| Marketing  | 19        | 22          | 418     |
| Total      | 340       |             | 8.559   |

| Miembro directivo  | Horas | Pecio/hora | Total  |
|--------------------|-------|------------|--------| 
| Project Manager    | 34    | 25         | 850    |
| Scrum Master       | 23    | 23         | 529    |
| Total              | 57    |            | 1.379  |

- Amortización de equipos: correspondiente a un mes del uso de hardware por parte del equipo, lo que equivale a 390€
- Costes de infraestructura: quedan en 0€ debido a que aun no se ha tenido que usar creditos de ninguna licencia

En total llevamos un gasto de 10.328 de los 18.035€ presupuestados para el primer Sprint, Lo que nos mantiene muy por debajo del presupuesto.



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

 