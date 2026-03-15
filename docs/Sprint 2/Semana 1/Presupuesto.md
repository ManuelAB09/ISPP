# Documentación del Presupuesto del Proyecto MeerKatters

### Grupo 9 – Turno de tarde

---

**Proyecto:** MeerKatters  
**Documento:** Gestión  
**Sprint:** Sprint 2  
**Estado:** En revisión  
**Fecha:** 10/03/2026  

---

## Índice

1. [Introducción](#1-introducción)
2. [Estructura del equipo y planificación de horas](#2-estructura-del-equipo-y-planificación-de-horas)
3. [Coste de personal](#3-coste-de-personal)
4. [Coste empresa (overhead)](#4-coste-empresa-overhead)
5. [Costes indirectos](#5-costes-indirectos)
6. [Costes de licencias – Microsoft Azure](#6-costes-de-licencias--microsoft-azure)
7. [Coste material – Amortización de equipos](#7-coste-material--amortización-de-equipos)
8. [Presupuesto total](#8-presupuesto-total)

---

## 1. Introducción

El presente documento recoge la justificación y el desglose del presupuesto estimado para el desarrollo del proyecto **MeerKatters**, una aplicación orientada a facilitar comunidades de estudio y organización de sesiones presenciales entre estudiantes.

---

## 2. Estructura del equipo y planificación de horas

El proyecto se divide en cuatro sprints: un Sprint de Desarrollo Previo (Sprint DP) de **3 semanas** de duración, y tres sprints de desarrollo (Sprint 1, 2 y 3) de **2 semanas** cada uno. En total, el proyecto abarca **9 semanas**.

Para cada semana se estiman **10 horas por persona**, lo que resulta en el siguiente cómputo de horas planificadas:

| Sprint    | Semanas | Horas/persona | Horas planificadas | Horas + Desviación (15%) |
|-----------|---------|---------------|--------------------|--------------------------|
| Sprint DP | 3       | 30            | 600                | 690                      |
| Sprint 1  | 2       | 20            | 400                | 460                      |
| Sprint 2  | 2       | 20            | 400                | 460                      |
| Sprint 3  | 2       | 20            | 400                | 460                      |
| **TOTAL** |         |               | **1.800**          | **2.070**                |

A las horas planificadas se aplica una **desviación del 15%** para cubrir posibles imprevistos y retrasos, elevando el total a **2.070 horas** efectivas estimadas.

---

## 3. Coste de personal

Las tarifas por hora bruto de cada rol han sido definidas en base a los perfiles del equipo recogidos en el estudio de mercado. A continuación se detalla el desglose por sprint.

### Sprint DP

El equipo del Sprint DP está compuesto por **20 personas** distribuidas en los siguientes roles:

| Rol           | Precio/hora bruto | Coste estimado/persona | Nº personas | Total estimado bruto |
|---------------|:-----------------:|:-----------------------:|:-----------:|:--------------------:|
| Dirección     | 16 €              | 552 €                   | 2           | 1.104 €  |
| Backend       | 14 €              | 483 €                   | 6           | 2.898 €  |
| Frontend      | 14 €              | 483 €                   | 6           | 2.898 €  |
| Arquitectura  | 22 €              | 759 €                   | 2           | 1.518 €              |
| RRSS          | 10 €              | 345 €                   | 2           | 690 €                |
| Marketing     | 13 €              | 448,5 €                 | 2           | 897 €                |
| **TOTAL**     |                   |                         |             | **10.005 €**          |

> **Nota:** El coste estimado por persona en el Sprint DP se obtienen dividiendo las horas con desviación entre los 20 integranes del equipo, multiplicando luego.

### Sprints 1, 2 y 3

Para los sprints de desarrollo, la distribución de roles varía ligeramente respecto al Sprint DP. Cada sprint tiene una duración de **2 semanas**:

| Rol           | Precio/hora bruto | Precio estimado/persona | Nº personas | Total estimado bruto |
|---------------|:-----------------:|:-----------------------:|:-----------:|:--------------------:|
| Dirección     | 16 €              | 368 €                   | 2           | 736 €                |
| Desarrollo    | 14 €              | 322 €                   | 12          | 3.864 €              |
| Arquitectura  | 22 €              | 506 €                   | 3           | 1.518 €              |
| RRSS          | 10 €              | 230 €                   | 3           | 690 €                |
| **TOTAL**     |                   |                         |             | **6.808 €**          |

El coste bruto de personal por cada uno de los tres sprints de desarrollo es, por tanto, de **6.808 €**.

El **coste bruto total de personal** del proyecto asciende a:

> 10.005 € (Sprint DP) + 6.808 € × 3 (Sprints 1, 2 y 3) = **30.429 € brutos**

---

## 4. Coste empresa (overhead)

Sobre el coste bruto de personal se aplica un **incremento del 30%** para cubrir las cargas sociales y el overhead empresarial (Seguridad Social a cargo de la empresa, seguros, gestión, etc.). Este porcentaje es habitual en el sector tecnológico para pasar del coste salarial bruto al coste real para la empresa.

| Sprint    | Estimado bruto | Coste empresa (+30%) |
|-----------|:--------------:|:--------------------:|
| Sprint DP | 10.005 €       | 13.006,5 €           |
| Sprint 1  | 6.808 €        | 8.850,4 €            |
| Sprint 2  | 6.808 €        | 8.850,4 €            |
| Sprint 3  | 6.808 €        | 8.850,4 €            |
| **TOTAL** | **30.429 €**   | **39.557,7 €**       |

El **coste total de personal para la empresa** asciende a **39.557,7 €**.

---

## 5. Costes indirectos

Los costes indirectos se estiman como un **10% del coste de personal para la empresa**, representando gastos generales de estructura no directamente imputables al proyecto (alquiler de espacios, consumo eléctrico, materiales de oficina, etc.).

> Costes indirectos = 39.557,7 € × 10% = **3.955,77 €**

---

## 6. Costes de licencias – Microsoft Azure

Para el despliegue e infraestructura del proyecto se utilizará **Microsoft Azure**. El coste mensual estimado de la plataforma es de **23,50 €/mes**, lo que equivale a **5,875 €/semana**.

El coste de Azure se distribuye por sprint según su duración:

| Sprint    | Semanas | Coste Azure/sprint |
|-----------|---------|--------------------|
| Sprint DP | 3       | —*                 |
| Sprint 1  | 2       | 17,625 €           |
| Sprint 2  | 2       | 11,75 €            |
| Sprint 3  | 2       | 11,75 €            |
| **TOTAL** |         | **41,125 €**       |

> \* Durante el Sprint DP no se contempla coste de Azure ya que en esa fase aún no se ha iniciado el despliegue activo de la infraestructura.

El coste total en concepto de licencias de Azure para el proyecto es de **41,125 €**.

---

## 7. Coste material – Amortización de equipos

Tal y como se recoge en el **Estudio de Mercado** ([Semana 3, Sprint DP](../docs/Sprint%20DP/Semana%203/Estudio%20de%20Mercado.md)), el equipo de trabajo utilizará sus **ordenadores personales** para el desarrollo del proyecto. Se ha estimado que el precio medio de los equipos es de **800 €**, con un **valor residual de 100 €** tras **3 años de vida útil**.

La amortización anual se calcula mediante el método lineal:

> **Amortización anual por equipo** = (Valor inicial − Valor residual) / Vida útil  
> = (800 € − 100 €) / 3 años = **233,33 €/año por equipo**

El equipo está compuesto por **20 personas** (20 equipos). La amortización anual total para todos los equipos del equipo es, por tanto:

Para obtener la amortización por sprint, se prorratea en función del número de semanas de cada sprint sobre las 52 semanas del año:

| Sprint    | Semanas | Amort. anual/equipo | Amort./sprint/equipo | Amort. total del equipo/sprint |
|-----------|---------|---------------------|:--------------------:|:------------------------------:|
| Sprint DP | 3       | 233,33 €            | 13,4615 €            | 269,23 €                       |
| Sprint 1  | 2       | 233,33 €            | 8,9742 €             | 179,48 €                       |
| Sprint 2  | 2       | 233,33 €            | 8,9742 €             | 179,48 €                       |
| Sprint 3  | 2       | 233,33 €            | 8,9742 €             | 179,48 €                       |
| **TOTAL** |         |                     |                      | **807,68 €**                   |

> **Fórmula aplicada:** Amort./sprint = (Amort. anual / 52 semanas) × Nº semanas del sprint × 20 equipos

El coste total en concepto de amortización de material informático asciende a **807,68 €**.

---

## 8. Presupuesto total

Una vez sumados todos los componentes del presupuesto, el **presupuesto total del proyecto** queda como sigue:

| Concepto                         | Importe       |
|----------------------------------|:-------------:|
| Coste de personal para la empresa| 39.557,7 €   |
| Costes indirectos (10%)          | 3.955,77 €    |
| Costes de licencias (Azure)      | 41,125 €      |
| Coste material (amortización)    | 807,68 €      |
| **PRESUPUESTO TOTAL**            | **44.362,28 €** |

El **presupuesto total estimado del proyecto MeerKatters asciende a 44.362,28 €**, incluyendo todos los costes de personal, indirectos, infraestructura cloud y amortización de equipos informáticos.

---