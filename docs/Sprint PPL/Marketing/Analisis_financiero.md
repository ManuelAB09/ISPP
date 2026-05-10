# Análisis financiero y validación del modelo de negocio (MeerKatters)

## Grupo 9 – Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Análisis financiero (escenarios, monetización y recuperación)  
**Sprint:** Sprint X  
**Semana:** Semana X  
**Estado:** En revisión  
**Fecha:** 05/05/2026  
**Autor(es):** Grupo 9 (Turno de tarde)

---

## Índice
1. [Contexto y objetivo](#contexto-y-objetivo)
2. [Cómo se monetiza MeerKatters](#cómo-se-monetiza-meerkatters)
3. [Qué nos dicen los escenarios](#qué-nos-dicen-los-escenarios)
4. [Recuperación de la inversión y punto de equilibrio](#recuperación-de-la-inversión-y-punto-de-equilibrio)
5. [Riesgos, señales de alerta y palancas de mejora](#riesgos-señales-de-alerta-y-palancas-de-mejora)
6. [Conclusiones](#conclusiones)

---

## Contexto y objetivo
El objetivo de este documento es justificar, con un enfoque realista y defendible, la viabilidad económica de MeerKatters a partir de dos simulaciones: un escenario **optimista** (crecimiento y conversión favorables) y un escenario **pesimista** (crecimiento más lento y monetización menos eficiente).  

La inversión inicial considerada es de **60.000 €**. En los propios datos se menciona que habría que contemplar gastos de marketing, por lo que este análisis se interpreta como una base conservadora sobre la que, en un proyecto real, se añadiría el plan de adquisición con su presupuesto detallado.

---

## Cómo se monetiza MeerKatters
El modelo combina monetización B2C y B2B, lo cual es relevante porque reduce la dependencia de una sola fuente de ingresos:

- **Estudiantes (B2C):** estrategia freemium con upsell a planes de pago.
  - Freemium: 0 € (adquisición y retención).
  - Premium: 1,99 €/mes (entrada “barata”, alto volumen).
  - Pro: 4,99 €/mes (ticket mayor; menor volumen, mayor margen).

- **Profesores:** pago único por **promoción** (19,99 €).  
  Este ingreso es útil como “empuje” al inicio (cash-in inmediato), pero no debería ser el pilar del negocio por su carácter no recurrente.

- **Entidades educativas (B2B):** suscripción mensual de mayor ticket:
  - Academias: 120 €/mes
  - Colegios: 340 €/mes
  - Universidades: 590 €/mes  
  Esta línea suele estabilizar ingresos porque el churn B2B tiende a ser menor si el producto encaja en operación/centro.

- **Comisiones por clase (15%):** ingreso variable ligado a la actividad real del marketplace.  
  En la práctica, esta comisión es la que mejor “escala” cuando el producto consigue liquidez (suficientes estudiantes + profesores activos).

En conjunto, el diseño tiene un patrón típico de producto real: ingresos recurrentes (suscripciones), ingresos de activación (promoción) e ingresos variables (comisión). Esa combinación es positiva porque diversifica el riesgo.

---

## Qué nos dicen los escenarios
Los dos escenarios no solo difieren en el número de usuarios, sino en cómo se comporta el sistema cuando crece: conversión, volumen de alumnos, incorporación de profesores y entrada de entidades.

### Escenario optimista: crecimiento suficiente para acelerar el ROI
En el escenario optimista se observa una progresión rápida de la base de usuarios y, con ella, un incremento fuerte del ingreso neto mensual. Existe mayor conversión a premium/pro, unas estrategias de marketing que funcionan como esperado y nos hacen ganar grandes volúmenes de usuarios, un gran uso de la aplicación y unos profesores capaces de facturar una buena cantidad de dinero al mes. La lectura más importante no es “se gana mucho”, sino que:
- la **pendiente** del ingreso neto crece con el tiempo,
- la combinación B2C + B2B empieza a sostener el negocio,
- y el balance acumulado cruza a positivo relativamente pronto.

Este escenario sugiere que, si la adquisición y la conversión se parecen a las hipótesis optimistas, MeerKatters puede entrar en un ciclo virtuoso: más usuarios → más actividad → más comisión → más atractivo para profesores/centros → más retención. Esto desencadenaría en una recuperación de la inversión en menos de dos años.

### Escenario pesimista: negocio viable, pero exige aguante financiero
En el pesimista, el producto monetiza, pero la velocidad es menor, hay menor conversión a premium/pro, un crecimienot mucho más lento y estancado y un uso menor de nuestra aplicación que lleva incluso a los profesores a facturar incluso la mitad de en el caso optimista. La consecuencia directa es que:
- el balance tarda más en recuperarse,
- y el proyecto necesita más “runway” (tiempo/financiación) para sobrevivir hasta el break-even.

Aun así, el escenario pesimista no es un fracaso: el modelo termina alcanzando rentabilidad acumulada, solo que más tarde. En un entorno real, esto se traduce en que el plan de inversión debería contemplar colchón (y/o fases) para no depender de que “todo vaya perfecto” en los primeros meses.

---

## Recuperación de la inversión y punto de equilibrio
El indicador clave que se desprende de los datos es el **mes de recuperación (payback)**, es decir, cuándo el balance acumulado pasa de negativo a positivo.

- **Optimista:** la inversión se recupera aproximadamente en el **mes 16**.  
  Lectura realista: el producto encuentra tracción suficiente en el año 1 para compensar inversión durante el año 2 temprano.

- **Pesimista:** la recuperación llega aproximadamente en el **mes 28**.  
  Lectura realista: el producto necesita más de 2 años para devolver la inversión inicial, lo cual es compatible con proyectos reales (especialmente si hay componente marketplace + B2B), pero exige una estrategia de costes y adquisición más cuidadosa.

### Qué significa esto en un proyecto “de verdad”
- Un payback de 16 meses es razonable si hay capacidad de escalar adquisición sin disparar costes.
- Un payback de 28 meses obliga a:
  - controlar burn rate,
  - priorizar retención y recurrencia (B2B + suscripciones),
  - y evitar depender de ingresos no recurrentes.

En ambos escenarios, el punto de equilibrio no es “mágico”: llega cuando el producto combina bien tres cosas:
1) conversión de estudiantes a planes de pago,  
2) crecimiento de centros (alto ticket),  
3) aumento de actividad (comisiones).

---

## Riesgos, señales de alerta y palancas de mejora

### Riesgos principales
1. **Conversión B2C menor de lo esperado:** si Premium/Pro convierten por debajo del supuesto, el MRR se debilita.
2. **Marketplace sin liquidez suficiente:** sin suficientes clases, la comisión del 15% no despega.
3. **B2B más lento (ciclos de venta):** academias/colegios/universidades pueden requerir negociación y periodos de prueba.

## Mediddas de contingencia
### PROBLEMA: BAJA CONVERSIÓN B2C
1. Pivotaje hacia el modelo B2B (Licenciamiento Corporativo): Desviar el foco comercial de la venta unitaria (B2C) hacia la venta de licencias por volumen a instituciones y empresas. Esto permite obtener ingresos recurrentes de alto impacto y un acceso masivo a usuarios finales con un menor costo de adquisición individual.
2. edefinir los límites del plan gratuito para aumentar el valor percibido de la versión Premium. Al mover funciones de alta demanda al plan de pago, se genera una necesidad real de conversión para los usuarios activos.
3. Implementación de Ofertas de Retención (Win-back): (Nueva) Crear disparadores automáticos para usuarios que intentan cancelar o que llevan tiempo inactivos, ofreciendo descuentos temporales o periodos de prueba extendidos.

### PROBLEMA: CAMPAÑAS NO EFICIENTES
1. Auditar y suspender de inmediato la inversión en canales con un CAC (Costo de Adquisición) superior al LTV (Valor de Vida del Cliente). Los recursos liberados se reasignarán a los canales con mayor tasa de conversión probada.
2. Concentrar el presupuesto y los esfuerzos creativos en un máximo de 2 o 3 canales clave para dominar el nicho, evitando la dispersión del mensaje y del presupuesto.
3. mplementar un sistema de recompensas donde los usuarios actuales actúen como embajadores. Otorgar beneficios (como meses de suscripción gratuita o funciones exclusivas) por cada conversión exitosa o flujo de crecimiento de la app referida, convirtiendo la base de usuarios en un motor de crecimiento orgánico.
---

## Conclusiones
1. El modelo de ingresos es coherente con un producto real: combina recurrencia (suscripciones), B2B (alto ticket) y variable escalable (comisiones).
2. La viabilidad depende principalmente de la **velocidad de crecimiento y conversión**:
   - con hipótesis favorables, el proyecto recupera la inversión en torno al **mes 16**;
   - con hipótesis conservadoras, se recupera en torno al **mes 28**, requiriendo más margen financiero.

---