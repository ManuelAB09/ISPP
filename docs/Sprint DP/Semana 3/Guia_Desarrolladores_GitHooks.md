# Guía para Desarrolladores - Configuración de Git Hooks

## MeerKatters - Plataforma de Comunidades de Estudio

### Grupo 9 – Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Guía Técnica / DevOps  
**Sprint:** Sprint DP  
**Semana:** Semana 2  
**Estado:** Aprobado  
**Fecha:** 14/02/2026  
**Autor(es):** Raimundo Jiménez Lara, Manuel María Calderón Rodríguez

---

## Índice

1. [Introducción](#introducción)
2. [Requisitos Previos](#requisitos-previos)
3. [Activación de Git Hooks](#activación-de-git-hooks)
4. [Descripción de los Hooks](#descripción-de-los-hooks)
5. [Solución de Problemas](#solución-de-problemas)
6. [Desactivación Temporal](#desactivación-temporal)
7. [Resumen de Comandos](#resumen-de-comandos)
8. [Checklist del Nuevo Desarrollador](#checklist-del-nuevo-desarrollador)

---

## Introducción

Este proyecto utiliza **Git Hooks personalizados** para garantizar la calidad del código antes de que llegue al repositorio remoto. Los hooks realizan validaciones automáticas de:

- **Estilo de código**: ESLint (frontend) y Checkstyle (backend)
- **Mensajes de commit**: Formato Conventional Commits

Estos hooks son **obligatorios** para todos los desarrolladores del equipo y deben activarse antes de comenzar a trabajar en el proyecto.

---

## Requisitos Previos

Antes de activar los hooks, asegúrate de tener instalado:

### Para el Frontend (React/JavaScript)

```powershell
# Verificar Node.js (requerido: v18 o superior)
node --version

# Instalar dependencias del frontend
cd frontend
npm install
cd ..
```

### Para el Backend (Java/Spring Boot)

```powershell
# Verificar Java (requerido: JDK 21)
java -version

# Verificar Maven
mvn --version
```

> **Nota**: Si no tienes Maven instalado, puedes usar el Maven Wrapper incluido en el proyecto.

---

## Activación de Git Hooks

### Paso 1: Clonar el Repositorio

Si aún no lo has hecho:

```powershell
git clone <url-del-repositorio>
cd cicdtest-main
```

### Paso 2: Configurar la Ruta de Hooks

Ejecuta el siguiente comando **desde la raíz del proyecto**:

```powershell
git config core.hooksPath .githooks
```

Este comando le indica a Git que use los hooks personalizados ubicados en la carpeta `.githooks/` en lugar de la carpeta predeterminada `.git/hooks/`.

### Paso 3: Verificar la Configuración

Comprueba que la configuración se aplicó correctamente:

```powershell
git config --get core.hooksPath
```

Deberías ver:
```
.githooks
```

### Paso 4: Verificar Permisos (Solo Linux/macOS)

En sistemas Unix, los hooks deben tener permisos de ejecución:

```bash
chmod +x .githooks/pre-commit
chmod +x .githooks/commit-msg
```

> **Nota**: En Windows con Git Bash o PowerShell, esto generalmente no es necesario.

### Paso 5: Instalar Dependencias del Frontend

Los hooks de pre-commit necesitan ESLint instalado:

```powershell
cd frontend
npm install
cd ..
```

---

## Descripción de los Hooks

### Hook: pre-commit

**Archivo**: `.githooks/pre-commit`

**Cuándo se ejecuta**: Antes de cada `git commit`

**Qué hace**:
1. Detecta archivos `.js` y `.jsx` modificados en `frontend/`
2. Ejecuta **ESLint** sobre esos archivos
3. Detecta archivos `.java` modificados en `backend/src/`
4. Ejecuta **Checkstyle** (via Maven) sobre el backend
5. **Bloquea el commit** si hay errores de linting

**Ejemplo de salida exitosa**:
```
Ejecutando comprobaciones pre-commit...
  Comprobando ESLint (frontend)...
  Comprobando Checkstyle (backend)...
[OK] Todas las comprobaciones pre-commit pasaron.
```

**Ejemplo de salida con errores**:
```
Ejecutando comprobaciones pre-commit...
  Comprobando ESLint (frontend)...
  [FALLO] ESLint ha encontrado errores.

/frontend/src/App.js
  10:5  error  'unusedVar' is defined but never used  no-unused-vars

[FALLO] Pre-commit fallido. Corrige los errores antes de hacer commit.
```

---

### Hook: commit-msg

**Archivo**: `.githooks/commit-msg`

**Cuándo se ejecuta**: Después de escribir el mensaje de commit

**Qué valida**:
1. El mensaje no está vacío
2. El formato sigue **Conventional Commits**
3. Hay línea en blanco entre título y cuerpo
4. Breaking changes (`!`) tienen footer `BREAKING CHANGE:`

**Formato requerido**:
```
tipo(scope opcional): descripción

[cuerpo opcional]

[footer opcional]
```

**Tipos permitidos**:
| Tipo | Descripción |
|------|-------------|
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de errores |
| `docs` | Solo documentación |
| `style` | Formato, sin cambio de lógica |
| `refactor` | Refactorización de código |
| `perf` | Mejoras de rendimiento |
| `test` | Añadir o modificar tests |
| `build` | Sistema de build o dependencias |
| `ci` | Configuración de CI |
| `chore` | Tareas de mantenimiento |
| `revert` | Revertir cambios |

**Ejemplos válidos**:
```bash
# Simple
git commit -m "feat: añade página de login"

# Con scope
git commit -m "fix(auth): corrige validación de tokens"

# Con cuerpo
git commit -m "refactor(api): reorganiza estructura de endpoints

Mueve todos los endpoints de usuario a un controlador separado
para mejorar la organización del código."

# Breaking change
git commit -m "feat(api)!: cambia formato de respuesta JSON

BREAKING CHANGE: el campo 'data' ahora es un array en lugar de objeto"
```

**Ejemplos inválidos**:
```bash
# Sin tipo
git commit -m "arregla el bug del login"

# Tipo incorrecto
git commit -m "feature: nueva funcionalidad"

# Sin descripción
git commit -m "fix:"

# Falta espacio después de :
git commit -m "feat:sin espacio"
```

---

## Solución de Problemas

### Error: "ESLint no instalado"

```
[AVISO] ESLint no instalado. Ejecuta 'npm install' en frontend/.
```

**Solución**:
```powershell
cd frontend
npm install
cd ..
```

### Error: "No se encontró backend/pom.xml"

```
[AVISO] No se encontró backend/pom.xml.
```

**Solución**: Verifica que estás ejecutando el comando desde la raíz del proyecto.

### Error: "El mensaje no sigue Conventional Commits"

```
ERROR: El mensaje no sigue Conventional Commits.

  Formato: tipo(scope opcional)!?: descripción
  Tipos:   feat | fix | docs | style | refactor | perf | test | build | ci | chore | revert
```

**Solución**: Reescribe tu mensaje de commit siguiendo el formato correcto:
```powershell
git commit --amend -m "feat: descripción correcta"
```

### Error: "Debe haber una línea en blanco entre el título y el cuerpo"

**Solución**: Usa `git commit` sin `-m` para abrir el editor y añadir la línea en blanco:
```powershell
git commit
```

En el editor:
```
feat: título del commit

Aquí va el cuerpo del mensaje.
```

### Los Hooks No Se Ejecutan

**Verificar configuración**:
```powershell
git config --get core.hooksPath
```

Si no devuelve `.githooks`, ejecuta nuevamente:
```powershell
git config core.hooksPath .githooks
```

**Verificar que los archivos existen**:
```powershell
Get-ChildItem .githooks
```

---

## Desactivación Temporal

> **ADVERTENCIA**: Desactivar los hooks solo debe hacerse en casos excepcionales. Los commits sin validar serán rechazados por el CI en GitHub.

### Saltarse hooks para un commit específico

```powershell
git commit --no-verify -m "mensaje del commit"
```

### Desactivar hooks completamente (no recomendado)

```powershell
git config --unset core.hooksPath
```

### Reactivar hooks

```powershell
git config core.hooksPath .githooks
```

---


## Resumen de Comandos

| Acción | Comando |
|--------|---------|
| Activar hooks | `git config core.hooksPath .githooks` |
| Verificar configuración | `git config --get core.hooksPath` |
| Instalar dependencias frontend | `cd frontend && npm install` |
| Commit con validación | `git commit -m "tipo: mensaje"` |
| Commit sin validación (emergencia) | `git commit --no-verify -m "mensaje"` |
| Desactivar hooks | `git config --unset core.hooksPath` |

---

## Checklist del Nuevo Desarrollador

- [ ] Clonar el repositorio
- [ ] Ejecutar `git config core.hooksPath .githooks`
- [ ] Instalar Node.js (v18+) y npm
- [ ] Ejecutar `cd frontend && npm install`
- [ ] Instalar JDK 21 y Maven (o usar mvnw)
- [ ] Hacer un commit de prueba para verificar que los hooks funcionan
- [ ] Leer la documentación de Conventional Commits

---

## Enlaces Útiles

- [Conventional Commits Specification](https://www.conventionalcommits.org/)
- [ESLint Documentation](https://eslint.org/docs/latest/)
- [Checkstyle Documentation](https://checkstyle.org/)
- [Git Hooks Documentation](https://git-scm.com/docs/githooks)
- [Azure App Service Documentation](https://docs.microsoft.com/azure/app-service/)

---

*Guía actualizada el 16 de febrero de 2026*
