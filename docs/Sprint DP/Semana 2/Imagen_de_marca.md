
### Reporte de decisiones de diseño
#### 1. Objetivo del trabajo

El objetivo principal de este trabajo ha sido diseñar un sistema de identidad visual coherente y eficiente, basándome en artículos y papers que analizaban el efecto de UI en la psique humana. Mis prioridades han sido:
- claridad visual
    
- reducción de carga cognitiva
    
- consistencia de marca
    
- facilidad de lectura prolongada
Las decisiones tomadas se basan en principios de UX design, especialmente en estudios sobre psicología del color, flow cognitivo y sobrecarga cognitiva, tal como se expone en los artículos consultados.
#### 2. Enfoque general del sistema cromático

En lugar de diseñar una paleta “decorativa”, se ha optado por un sistema funcional de color, donde cada color tiene un rol específico, evitando ambigüedad y reduciendo decisiones innecesarias para el usuario.
Este enfoque responde a una conclusión clave de los artículos analizados:

> El color no debe atraer atención por sí mismo, sino **guiar la atención hacia lo relevante**.

Por ello, el sistema se estructura jerárquicamente en capas visuales, alineadas con cómo el cerebro procesa la información.
#### 3. Jerarquía cromática por capas
El sistema se organiza en cuatro capas visuales claramente diferenciadas:

```
CAPA 1 → Background
CAPA 2 → Contenido (texto)
CAPA 3 → Marca (estructura)
CAPA 4 → Énfasis (atención)
```

#### 4. Uso de los colores de marca

##### 4.1 Color primario (identidad)

**Brand / Primary - \#2D3250**

**Uso**

- Logotipo
    
- Títulos principales (H1, H2, H3)
    
- Elementos de identidad

**Criterios de diseño**

- Saturación media/baja (evita fatiga visual)
    
- Funciona en:
    
    - blanco y negro
        
    - fondos claros y oscuros

**Justificación UX**  
Según la psicología del color aplicada a UI, los colores dominantes deben ser **estables y poco excitantes**, ya que se repiten con frecuencia. Un color demasiado saturado genera cansancio y rompe el flow cognitivo. Por consiguiente, se ha elegido una paleta principal de colores fríos y apagados.

Según un artículo:
1. Blue: Re­flects reliability, instills calmness, common in corporate­ designs.

Este color actúa como **ancla visual de la marca**, no como estímulo emocional.

---

### 4.2 Color secundario (soporte)

**Brand / Secondary - #676F9D**

**Uso**

- Iconografía
    
- Líneas de apoyo

**Regla clave**

- Nunca usarse como foco principal

**Justificación UX**  
El uso de un color secundario desaturado permite introducir variación visual sin añadir carga cognitiva. Según los estudios sobre sobrecarga cognitiva y recuerdo de marca, los elementos de soporte deben “respirar”, no reclamar atención.

[https://uxplanet.org/color-psychology-in-ui-design-more-than-meets-the-eye-72da5051e51e](https://uxplanet.org/color-psychology-in-ui-design-more-than-meets-the-eye-72da5051e51e)  
[https://uxplanet.org/designing-ux-for-focus-and-flow-b58e5f36d1c8](https://uxplanet.org/designing-ux-for-focus-and-flow-b58e5f36d1c8)  
[https://www.logodesign.net/blog/cognitive-overload-brand-recall/](https://www.logodesign.net/blog/cognitive-overload-brand-recall/)

Esta paleta de color es óptima incluso para personas con daltonismo, ya que los colores más comunes a confundir son tonos de verdes y rojos, el único tipo de daltonismo que causaría problemas con nuestra imagen sería la tritanopía.

---

### 4.3 Color acento (acción y emoción)

**Brand / Accent - #F2C18E**

**Uso (muy limitado)**

- Datos importantes
    
- Elementos que deben verse inmediatamente

**Regla estricta**

- Un solo acento por página

**Justificación UX**  
El artículo sobre foco y flow destaca que el exceso de estímulos rompe la continuidad cognitiva. El color acento funciona como un disparador atencional, y su efectividad depende directamente de su escasez.

> Si todo es acento, nada es acento.

---

## 5. Uso de neutros (los verdaderos protagonistas)

### Paleta de neutros final

- Fondo principal - `#F4F4F4`
    
- Cajas / bloques - `#E3E3DE`
    
- Divisores - `#B8B8B2`
    
- Texto principal - `#2B2B2B`

Los neutros ocupan aproximadamente el 60-70 % del diseño.

**Justificación UX**  
Los artículos analizados coinciden en que los sistemas visuales con alto porcentaje de neutros:

- reducen fatiga visual
    
- aumentan comprensión
    
- mejoran recuerdo de marca
    

El fondo neutro actúa como **“silencio visual”**, permitiendo que el contenido sea el protagonista.

---

## 6. Tipografía y texto (prioridad de lectura)

### Texto principal `Text / Main (#2B2B2B)`

**Uso**

- Texto largo
    
- Listas

Se evita el negro puro para reducir contraste excesivo, lo que mejora la lectura prolongada.

### Texto secundario `Text / Secondary`

**Uso**

- Subtítulos
    
- Pies de página

Al ser más claro, este color reduce peso visual sin comprometer legibilidad, alineándose con principios de UX cognitiva.

---

## 7. Combinaciones aprobadas

**Página estándar:**

- Fondo: Background
    
- Títulos: Brand / Primary
    
- Texto: Text / Main
    
- Subtítulos: Text / Secondary
    
- Divisores: UI / Dividers

**Lista elementos:**

- Fondo: Background / Boxes
    
- Texto: Text / Main

**Llamada clave**

- Texto normal: Text / Main
    
- Elemento puntual: Brand / Accent


Estas combinaciones reducen decisiones de diseño y refuerzan consistencia, un factor clave en **brand recall**, terminología referida a la capacidad de una marca para ser recordada y reconocida.

---

## 8. Combinaciones prohibidas

Se prohíbe explícitamente:

- Accent + Secondary juntos
    
- Texto en Accent sobre fondo claro
    
- Usar Primary como fondo grande
    
- Usar Secondary para títulos principales
    

Estas combinaciones incrementan la carga cognitiva y rompen la jerarquía visual.

---

## 9. Implementación en Figma

Se ha utilizado Figma para poder implementar los estilos de texto y colores como plantillas fijas, que pueden ser reutilizadas para cada elemento.

## 10. Font
Se ha elegido Inter como la fuente de la compañía debido a que es open source y reconocible utilizada ampliamente en projectos como Google.
