# Configuración de Secretos y Variables en GitHub

## MeerKatters - Plataforma de Comunidades de Estudio

### Grupo 9 – Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Guía Técnica / DevOps  
**Sprint:** Sprint DP  
**Semana:** Semana 3  
**Estado:** Aprobado  
**Fecha:** 17/02/2026  
**Autor(es):** Raimundo Jiménez Lara, Manuel María Calderón Rodríguez

---

## Resumen Rápido (Configuración Optimizada - 2 Web Apps)

Esta configuración usa **2 Web Apps en un mismo App Service Plan B1** (~€11/mes, staging + producción, ambas con Always On) y **Static Web Apps gratis** para el frontend. B1 no soporta deployment slots, por lo que se usan 2 Web Apps independientes dentro del mismo plan.

### Secretos (Settings → Secrets and variables → Actions → Secrets)

| Nombre | Valor | Obligatorio |
|--------|-------|-------------|
| `AZURE_CREDENTIALS` | JSON de Service Principal (único) | Sí |
| `AZURE_STATIC_WEB_APPS_TOKEN_STAGING` | Token de Static Web App staging | Sí |
| `AZURE_STATIC_WEB_APPS_TOKEN_PRODUCTION` | Token de Static Web App producción | Sí |
| `SONAR_TOKEN` | Token de SonarQube | Opcional |
| `SONAR_HOST_URL` | URL del servidor SonarQube | Opcional |

### Variables (Settings → Secrets and variables → Actions → Variables)

| Nombre | Valor de Ejemplo | Obligatorio |
|--------|------------------|-------------|
| `AZURE_BACKEND_APP` | `meerkatters-backend` | Sí |
| `AZURE_BACKEND_APP_STAGING` | `meerkatters-backend-staging` | Sí |
| `STAGING_API_URL` | `https://meerkatters-backend-staging.azurewebsites.net` | Sí |
| `PRODUCTION_API_URL` | `https://meerkatters-backend.azurewebsites.net` | Sí |

---

## Paso a Paso para Configurar Secretos

### 1. Acceder a la Configuración de Secretos

1. Ve a tu repositorio en GitHub
2. Haz clic en **Settings**
3. En el menú lateral, busca **Secrets and variables** → **Actions**
4. Verás dos pestañas: **Secrets** y **Variables**

### 2. Crear el Secreto de Azure (ÚNICO)

#### AZURE_CREDENTIALS

1. En la pestaña **Secrets**, haz clic en **New repository secret**
2. **Name**: `AZURE_CREDENTIALS`
3. **Secret**: Pega el JSON del Service Principal (ver sección "Obtener Credenciales de Azure")
4. Haz clic en **Add secret**

> **Nota**: Con la configuración optimizada solo necesitas UN Service Principal porque todo está en el mismo Resource Group.

### 3. Obtener Tokens de Static Web Apps

#### AZURE_STATIC_WEB_APPS_TOKEN_STAGING

1. Ve al Portal de Azure → tu Static Web App de staging
2. En el menú lateral, haz clic en **Manage deployment token**
3. Copia el token
4. En GitHub, crea secreto: `AZURE_STATIC_WEB_APPS_TOKEN_STAGING`

#### AZURE_STATIC_WEB_APPS_TOKEN_PRODUCTION

Repite el proceso para producción con nombre: `AZURE_STATIC_WEB_APPS_TOKEN_PRODUCTION`

### 4. Crear Variables

1. Cambia a la pestaña **Variables**
2. Haz clic en **New repository variable**
3. Crea cada variable:

| Name | Value |
|------|-------|
| `AZURE_BACKEND_APP` | Nombre de tu Web App de producción (ej: `meerkatters-backend`) |
| `AZURE_BACKEND_APP_STAGING` | Nombre de tu Web App de staging (ej: `meerkatters-backend-staging`) |
| `STAGING_API_URL` | URL de staging (ej: `https://meerkatters-backend-staging.azurewebsites.net`) |
| `PRODUCTION_API_URL` | URL de producción (ej: `https://meerkatters-backend.azurewebsites.net`) |

---

## Obtener Credenciales de Azure

### Prerrequisitos

- Tener instalado [Azure CLI](https://docs.microsoft.com/cli/azure/install-azure-cli)
- Tener permisos de administrador en la suscripción de Azure

### Paso 1: Iniciar Sesión en Azure

```bash
az login
```

Se abrirá el navegador para autenticarte.

### Paso 2: Obtener el ID de Suscripción

```bash
az account show --query id -o tsv
```

Guarda este ID, lo necesitarás.

### Paso 3: Crear el Resource Group (ÚNICO)

```bash
# Un solo Resource Group para todo (ahorro de costes)
az group create --name rg-meerkatters --location westeurope
```

### Paso 4: Crear Service Principal (ÚNICO)

```bash
# Un solo Service Principal con acceso a todo el Resource Group
az ad sp create-for-rbac \
  --name "github-actions-meerkatters" \
  --role contributor \
  --scopes /subscriptions/<TU_SUBSCRIPTION_ID>/resourceGroups/rg-meerkatters \
  --sdk-auth
```

**Reemplaza** `<TU_SUBSCRIPTION_ID>` con tu ID de suscripción.

El output será algo como:

```json
{
  "clientId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "clientSecret": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "subscriptionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "tenantId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "activeDirectoryEndpointUrl": "https://login.microsoftonline.com",
  "resourceManagerEndpointUrl": "https://management.azure.com/",
  "activeDirectoryGraphResourceId": "https://graph.windows.net/",
  "sqlManagementEndpointUrl": "https://management.core.windows.net:8443/",
  "galleryEndpointUrl": "https://gallery.azure.com/",
  "managementEndpointUrl": "https://management.core.windows.net/"
}
```

**Copia TODO este JSON** y pégalo como valor de `AZURE_CREDENTIALS`.

---

## Configuración de SonarQube (Opcional)

Si quieres habilitar análisis de SonarQube:

### SONAR_TOKEN

1. Inicia sesión en tu servidor SonarQube
2. Ve a **My Account** → **Security** → **Generate Tokens**
3. Crea un token con nombre `github-actions`
4. Copia el token y añádelo como secreto `SONAR_TOKEN`

### SONAR_HOST_URL

La URL de tu servidor SonarQube, ejemplo:
- `https://sonarqube.tudominio.com`
- `https://sonarcloud.io` (si usas SonarCloud)

---

## Verificación

### Comprobar que los secretos están configurados

Los secretos no se pueden ver una vez guardados, pero puedes verificar que existen en:

**Settings → Secrets and variables → Actions → Secrets**

Deberías ver:
- AZURE_CREDENTIALS
- AZURE_STATIC_WEB_APPS_TOKEN_STAGING
- AZURE_STATIC_WEB_APPS_TOKEN_PRODUCTION
- SONAR_TOKEN (opcional)
- SONAR_HOST_URL (opcional)

### Comprobar que las variables están configuradas

Ve a la pestaña **Variables** y verifica:
- AZURE_BACKEND_APP
- AZURE_BACKEND_APP_STAGING
- STAGING_API_URL
- PRODUCTION_API_URL

---

## Troubleshooting

### Error: "AZURE_CREDENTIALS is not set"

El workflow de CD no funcionará hasta que configures el secreto de Azure.

### Error: "azure/login failed"

1. Verifica que el JSON está completo (incluye llaves `{}`)
2. Verifica que el Service Principal tiene permisos de `contributor` en el Resource Group
3. Verifica que el Service Principal no ha expirado

### Error: "App Service not found"

1. Verifica que el nombre en `AZURE_BACKEND_APP` coincide exactamente con el nombre en Azure
2. Los nombres son case-sensitive

### Error: "Staging App Service not found"

1. Verifica que el nombre en `AZURE_BACKEND_APP_STAGING` coincide exactamente con el nombre en Azure
2. Verifica que la Web App de staging existe en el Resource Group correcto
3. Verifica que ambas Web Apps están en el mismo App Service Plan B1

---

## Seguridad

**IMPORTANTE**:

- **NUNCA** compartas los secretos en commits, issues, o PRs
- **NUNCA** escribas los secretos en archivos del repositorio
- Los secretos de GitHub están encriptados y solo son accesibles durante la ejecución de workflows
- Rota los Service Principals periódicamente (recomendado: cada 6-12 meses)

### Rotación de Credenciales

Para rotar las credenciales de un Service Principal:

```bash
# Resetear el secreto
az ad sp credential reset --name "github-actions-meerkatters" --sdk-auth
```

Actualiza el secreto en GitHub con el nuevo JSON.

---

## Migración a Otra Cuenta de Azure

Si te quedas sin créditos y necesitas migrar a la cuenta de otro compañero, el proceso es **muy sencillo**:

### Pasos para Migrar (10-15 minutos)

1. **En la NUEVA cuenta de Azure**, crear los recursos:
   ```bash
   # El nuevo compañero ejecuta esto en su cuenta
   az login
   az group create --name rg-meerkatters --location westeurope
   # ... crear App Service, PostgreSQL, Static Web Apps
   ```

2. **Crear nuevo Service Principal**:
   ```bash
   az ad sp create-for-rbac \
     --name "github-actions-meerkatters" \
     --role contributor \
     --scopes /subscriptions/<NUEVO_SUBSCRIPTION_ID>/resourceGroups/rg-meerkatters \
     --sdk-auth
   ```

3. **Actualizar secretos en GitHub**:
   - Ir a Settings → Secrets → `AZURE_CREDENTIALS`
   - Hacer clic en "Update"
   - Pegar el nuevo JSON

4. **Actualizar tokens de Static Web Apps**:
   - Obtener nuevos tokens de las nuevas Static Web Apps
   - Actualizar `AZURE_STATIC_WEB_APPS_TOKEN_STAGING`
   - Actualizar `AZURE_STATIC_WEB_APPS_TOKEN_PRODUCTION`

5. **Actualizar variables**:
   - Actualizar URLs si cambian los nombres

### ¿Qué NO necesitas cambiar?

- Los workflows (ya están configurados)
- El código de la aplicación
- Las imágenes Docker (siguen en GitHub Container Registry)

### Exportar datos de PostgreSQL (si necesario)

```bash
# Exportar de la cuenta vieja
pg_dump -h viejo-servidor.postgres.database.azure.com -U admin -d meerkatters > backup.sql

# Importar en la cuenta nueva
psql -h nuevo-servidor.postgres.database.azure.com -U admin -d meerkatters < backup.sql
```

---

*Documento actualizado el 16 de febrero de 2026*
