# Título del Documento

### Grupo 9 – Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Desarrollo  
**Sprint:** Sprint 1  
**Semana:** Semana 1  
**Estado:** Aprobado  
**Fecha:** 04/03/2026  
**Autor(es):** Macarena Pereira Campos

---

## Índice
1. [Introducción](#1-introduccíon)
2. [Líneas de Ingresos](#2-líneas-de-Ingresos)
3. [Estrategia de Precios](#3-Estrategia-de-Precios)
4. [Presupuesto del Proyecto y Estado Actual](#4-Presupuesto-del-Proyecto-y-Estado-Actual)
5. [Rentabilidad](#5-Rentabilidad)

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
    - *Otras funcionalidades futuras que no formen parte del MVP*
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
    - *Otras funcionalidades futuras que no formen parte del MVP*


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

Por tanto, el presupuesto total estimado del proyecto asciende a **72.142,25 €**, incluyendo tanto los costes de desarrollo como los costes de infraestructura.

---

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

- Amortización de equipos: correspondiente a un mes del uso de hardware por parte del equipo, lo que equivale a **390 €**
- Costes de infraestructura: quedan en **0 €** debido a que aun no se ha tenido que usar creditos de ninguna licencia

En total llevamos un gasto de **10.328 €** de los 18.035€ presupuestados para el primer Sprint, Lo que nos mantiene muy por debajo del presupuesto.

## 5. Rentabilidad
Para asegurar la viabilidad financiera de MeerKatters, es necesario calcular cuántos usuarios de pago se necesitan tanto para mantener la operatividad diaria de la plataforma como para amortizar la inversión inicial de desarrollo.

--- 

### 5.1. Punto de Equilibrio Operativo (Mensual)
El punto de equilibrio operativo indica el nivel de ingresos mínimos necesarios para mantener la aplicación "viva" mes a mes, sin contar el pago de la deuda de desarrollo.

Nuestro principal coste fijo recurrente es la infraestructura cloud en Microsoft Azure, estimada en 23,50 € mensuales. Teniendo en cuenta nuestra estrategia de precios, alcanzar este punto es altamente factible incluso en la fase más temprana de lanzamiento. Para cubrir exclusivamente estos gastos de servidor, necesitaríamos lograr uno de los siguientes escenarios básicos:

- Escenario A (Solo Profesores): Convertir a 4 profesores al Plan Premium (4 usuarios × 5,99 € = 23,96 €).
- Escenario B (Solo Alumnos): Convertir a 8 alumnos al Plan Premium (8 usuarios × 2,99 € = 23,92 €).
- Escenario C (Basado en usuarios piloto): Actualmente contamos con una base validada de 54 usuarios piloto (29 alumnos y 25 profesores). Si logramos una tasa de conversión inicial muy conservadora del 10% sobre esta lista (apenas 3 alumnos y 2 profesores pagando la suscripción), generaríamos 20,95 € mensuales, cubriendo de manera casi inmediata los costes de mantenimiento base.

---

### 5.2. Cálculo de Recuperación de la Inversión (ROI) basado en la Fase Piloto
Más allá del mantenimiento mensual, el proyecto tiene un presupuesto total estimado de 72.142,25 €, que incluye todo el valor devengado por el desarrollo, licencias y amortización de equipos. Para proyectar la recuperación de esta inversión, vamos a basarnos estrictamente en nuestra tracción actual: los 54 usuarios piloto confirmados (29 alumnos y 25 profesores).

Estimamos una tasa de conversión conservadora del 30% hacia nuestros planes Premium. El 70% restante se mantendrá en los planes básicos gratuitos. Además, para calcular los ingresos variables, establecemos el supuesto de que un profesor activo factura una media de 50 € mensuales en clases a través de nuestra pasarela.

Bajo este escenario, el cálculo de ingresos mensuales sería el siguiente:

---

#### Ingresos por Suscripciones (Fijos):
Alumnos Premium: 30% de 29 = ~9 alumnos × 2,99 € = 26,91 €

Profesores Premium: 30% de 25 = ~7 profesores × 5,99 € = 41,93 €

Subtotal Suscripciones: **68,84 €/mes**

---

#### Ingresos por Comisiones (Variables):
Profesores Premium (10% comisión): 7 profesores × 50 € de facturación media × 10% = 35,00 €

Profesores Básicos (15% comisión): El 70% restante (18 profesores) × 50 € de facturación media × 15% = 135,00 €

Subtotal Comisiones: **170,00 €/mes**

---

#### Análisis del Tiempo de Recuperación:
Sumando ambas líneas de negocio, nuestros usuarios piloto generarían unos ingresos totales de 238,84 € al mes. Si a esto le restamos nuestro coste fijo de infraestructura en Azure (23,50 € mensuales), obtenemos un beneficio neto de 215,34 € al mes.

Si dividimos el presupuesto total de desarrollo (72.142,25 €) entre este beneficio neto (215,34 €), el tiempo de recuperación de la inversión con solamente los usuarios piloto actuales sería de aproximadamente **335 meses**.

---

#### Conclusión Estratégica:
Este análisis permite extraer dos ideas clave sobre el modelo de negocio de MeerKatters.

En primer lugar, el peso real de los ingresos no recae en las suscripciones, sino en las comisiones generadas por el volumen de transacciones de los profesores. Esto confirma que la decisión de ofrecer un acceso básico sin coste es coherente con la estrategia económica del proyecto, ya que el verdadero potencial de ingresos se encuentra en la actividad dentro de la plataforma y no en la barrera de entrada.

En segundo lugar, se evidencia la necesidad de escalar. Aunque la base actual de usuarios piloto permite cubrir con holgura los costes operativos, el volumen todavía es insuficiente para amortizar en un plazo razonable la inversión en desarrollo. Para situar el retorno de la inversión en un estándar habitual dentro de la industria del software (por ejemplo, en torno a los 24 meses), sería necesario aumentar mucho la base inicial de usuarios.

 