# Opciones del comando Selenium

Este repositorio expone un alias en Maven Wrapper para los tests de aceptacion con Selenium:

- Windows (PowerShell/CMD): .\\mvnw selenium
- Linux/macOS: ./mvnw selenium

Importante: ejecuta el comando desde la carpeta backend.

## Que hace el alias

El alias expande internamente a:

- -Dtest=AcceptanceE2ETest
- -DrunE2E=true
- test

Por eso puedes anadir flags extra despues de selenium.

## Flags soportadas

1. -DonlyCases=PA-01,PA-34
Ejecuta solo los casos de aceptacion indicados.

2. -Dheadless=true|false
Controla el modo del navegador.
- true: sin ventana visible (recomendado en CI).
- false: con ventana visible (util para depurar en local).

3. -DuiBaseUrl=http://localhost:3000
Define la URL base del frontend para Selenium.
Usalo si tu frontend corre en otro host o puerto.

4. -DforceVisualNavigation=true|false
Controla la navegacion visual auxiliar entre flujos.
- true: mantiene la navegacion visual.
- false: reduce transiciones visuales y suele mejorar estabilidad/tiempo en CI.

5. -DvisualStepDelayMs=1200
Delay en milisegundos entre pasos de navegacion visual.
Util cuando usas headless=false y quieres observar transiciones.

6. -DcaseDelayMs=0
Delay en milisegundos entre casos de aceptacion.
Util para depuracion de tiempos o para bajar carga puntual.

## Ejemplos habituales

- Ejecutar todos los casos (por defecto):
  - .\\mvnw selenium

- Ejecutar solo casos concretos:
  - .\\mvnw selenium "-DonlyCases=PA-01,PA-34"

- Ejecutar con navegador visible:
  - .\\mvnw selenium -Dheadless=false

- Usar una URL de frontend distinta:
  - .\\mvnw selenium -DuiBaseUrl=http://localhost:4173

- Modo estable tipo CI:
  - .\\mvnw selenium -Dheadless=true -DforceVisualNavigation=false

## Recomendaciones para CI

Antes de ejecutar Selenium en CI, asegurate de que:

1. El frontend esta levantado y accesible en uiBaseUrl.
2. Chrome esta instalado en el runner.
3. El comando va en modo headless.
4. Los reportes de Selenium se suben aunque haya fallo.
