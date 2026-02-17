# Guía de Despliegue en Azure

## MeerKatters - Plataforma de Comunidades de Estudio

### Grupo D – Turno de tarde

---

**Proyecto:** MeerKatters
**Documento:** Guía Técnica / Arquitectura
**Sprint:** Sprint 0
**Semana:** Semana 3
**Estado:** Aprobado
**Fecha:** 17/02/2026
**Autor(es):** Raimundo Jiménez Lara, Manuel María Calderón Rodríguez

---

## Resumen de la Arquitectura

```
┌─────────────────────────────────────────────────────────────────────┐
│                              AZURE                                  │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │              Azure Static Web Apps (GRATIS)                    │ │
│  │         Frontend Staging    │    Frontend Producción           │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │              App Service Plan B1 (~€11/mes)                    │ │
│  │     1 vCPU  │  1.75 GB RAM  │  Always On  │  Linux             │ │
│  │                                                                │ │
│  │  ┌─────────────────────┐  ┌─────────────────────────────────┐  │ │
│  │  │  Web App: Staging   │  │  Web App: Producción            │  │ │
│  │  │  (Docker)           │  │  (Docker)                       │  │ │
│  │  │  Always On: Si      │  │  Always On: Si                  │  │ │
│  │  └─────────────────────┘  └─────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │              PostgreSQL Flexible B1ms (~€12.50/mes)            │ │
│  │         schema staging    │    schema production               │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│                    TOTAL: ~€23.50/mes                               │
└─────────────────────────────────────────────────────────────────────┘
```

### ¿Por qué esta arquitectura?

| Decisión | Razón |
|----------|-------|
| **1 plan B1 con 2 Web Apps** | Staging y producción comparten CPU/RAM. Máximo ahorro (~€11/mes total) |
| **Always On en ambas** | Disponible en B1. Evita cold starts (~20-30s) de Spring Boot |
| **No deployment slots** | B1 no los soporta (requiere Standard S1 ~€58/mes, inviable) |
| **1 PostgreSQL con 2 schemas** | Ahorro de ~€12.50/mes vs 2 servidores |
| **Static Web Apps gratis** | Perfecto para SPA React, incluye CDN + SSL |

### Consideraciones de recursos compartidos

B1 tiene 1 vCPU + 1.75 GB RAM. Con 2 Web Apps de Spring Boot (~300-500 MB cada una):
- ~1 GB ocupado por ambas apps
- ~0.75 GB libre para SO y picos
- Suficiente para un proyecto universitario con poco tráfico

Si se necesita más capacidad, se puede escalar a **B2** (€22/mes, 2 vCPU, 3.5 GB RAM) manteniendo la misma estructura (1 plan, 2 apps).

### Tabla de Costes

| Servicio | Tier | Coste/Mes |
|----------|------|-----------|
| App Service Plan B1 (2 Web Apps) | Basic | ~€11 |
| Static Web Apps × 2 | Free | €0 |
| PostgreSQL Flexible B1ms | Burstable | ~€12.50 |
| **TOTAL** | | **~€23.50/mes** |

> Precios: Linux, West Europe, EUR. B1 = €11.002/mes (fuente: Azure Pricing Calculator, febrero 2026).

### Duración Estimada del Crédito

| Crédito | Coste Mensual | Duración |
|---------|---------------|----------|
| $100 (≈€83.72) | ~€23.50 | **~3.5 meses** |

---

## Paso 1: Prerrequisitos

### 1.1 Activar Azure for Students

1. Ir a [Azure for Students](https://azure.microsoft.com/es-es/free/students/)
2. Iniciar sesión con cuenta universitaria
3. Verificar los $100 (≈€83.72) de crédito en [Cost Management](https://portal.azure.com/#blade/Microsoft_Azure_CostManagement)

### 1.2 Instalar Azure CLI

```bash
# macOS
brew install azure-cli

# Windows (PowerShell como admin)
winget install Microsoft.AzureCLI

# Linux (Ubuntu/Debian)
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
```

### 1.3 Verificar instalación

```bash
az --version
az login
az account show --query "{name:name, id:id}" -o table
```

---

## Paso 2: Crear Resource Group

Un único Resource Group para todos los recursos:

```bash
az group create \
  --name rg-meerkattersd \
  --location westeurope
```

---

## Paso 3: Crear App Service Plan + 2 Web Apps

### 3.1 Crear plan B1 (único)

```bash
az appservice plan create \
  --name plan-meerkattersd \
  --resource-group rg-meerkattersd \
  --sku B1 \
  --is-linux
```

### 3.2 Crear Web App de producción (en el mismo plan)

```bash
az webapp create \
  --name meerkattersd-backend \
  --resource-group rg-meerkattersd \
  --plan plan-meerkattersd \
  --deployment-container-image-name ghcr.io/tu-org/tu-repo/backend:latest

# Habilitar Always On
az webapp config set \
  --name meerkattersd-backend \
  --resource-group rg-meerkattersd \
  --always-on true
```

### 3.3 Crear Web App de staging (en el mismo plan)

```bash
az webapp create \
  --name meerkattersd-backend-staging \
  --resource-group rg-meerkattersd \
  --plan plan-meerkattersd \
  --deployment-container-image-name ghcr.io/tu-org/tu-repo/backend:staging

# Habilitar Always On
az webapp config set \
  --name meerkattersd-backend-staging \
  --resource-group rg-meerkattersd \
  --always-on true
```

> **Nota:** Ambas Web Apps usan `--plan plan-meerkattersd` (el mismo plan B1). No se crea un segundo plan. El coste es €11/mes independientemente de cuántas Web Apps haya dentro.

---

## Paso 4: Crear Static Web Apps (Frontend)

```bash
# Static Web App para staging
az staticwebapp create \
  --name meerkattersd-frontend-staging \
  --resource-group rg-meerkattersd \
  --location westeurope2 \
  --sku Free

# Static Web App para producción
az staticwebapp create \
  --name meerkattersd-frontend-prod \
  --resource-group rg-meerkattersd \
  --location westeurope2 \
  --sku Free
```

---

## Paso 5: Crear PostgreSQL

```bash
# Crear servidor PostgreSQL Flexible B1ms
az postgres flexible-server create \
  --name meerkattersd-db \
  --resource-group rg-meerkattersd \
  --location westeurope \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --storage-size 32 \
  --version 16 \
  --admin-user meerkattersd_admin \
  --admin-password '<CONTRASEÑA_SEGURA>' \
  --yes

# Permitir servicios de Azure
az postgres flexible-server firewall-rule create \
  --resource-group rg-meerkattersd \
  --name meerkattersd-db \
  --rule-name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0

# Crear bases de datos (schemas)
az postgres flexible-server db create \
  --resource-group rg-meerkattersd \
  --server-name meerkattersd-db \
  --database-name meerkattersd_staging

az postgres flexible-server db create \
  --resource-group rg-meerkattersd \
  --server-name meerkattersd-db \
  --database-name meerkattersd_prod
```

---

## Paso 6: Configurar Variables de Entorno

### 6.1 Web App Producción

```bash
az webapp config appsettings set \
  --name meerkattersd-backend \
  --resource-group rg-meerkattersd \
  --settings \
    SPRING_DATASOURCE_URL="jdbc:postgresql://meerkattersd-db.postgres.database.azure.com:5432/meerkattersd_prod" \
    SPRING_DATASOURCE_USERNAME="meerkattersd_admin" \
    SPRING_DATASOURCE_PASSWORD="<CONTRASEÑA>" \
    SPRING_PROFILES_ACTIVE="production" \
    WEBSITES_PORT="8080"
```

### 6.2 Web App Staging

```bash
az webapp config appsettings set \
  --name meerkattersd-backend-staging \
  --resource-group rg-meerkattersd \
  --settings \
    SPRING_DATASOURCE_URL="jdbc:postgresql://meerkattersd-db.postgres.database.azure.com:5432/meerkattersd_staging" \
    SPRING_DATASOURCE_USERNAME="meerkattersd_admin" \
    SPRING_DATASOURCE_PASSWORD="<CONTRASEÑA>" \
    SPRING_PROFILES_ACTIVE="staging" \
    WEBSITES_PORT="8080"
```

---

## Paso 7: Configurar GitHub

### 7.1 Crear Service Principal (único)

```bash
az ad sp create-for-rbac \
  --name "github-actions-meerkattersd" \
  --role contributor \
  --scopes /subscriptions/<TU_SUBSCRIPTION_ID>/resourceGroups/rg-meerkattersd \
  --sdk-auth
```

Copiar el JSON completo resultante.

### 7.2 Configurar Secretos y Variables en GitHub

Ver [Configuración de Secretos GitHub](Configuracion_Secretos_GitHub.md) para detalles.

**Secretos:**

| Nombre | Valor |
|--------|-------|
| `AZURE_CREDENTIALS` | JSON del Service Principal |
| `AZURE_STATIC_WEB_APPS_TOKEN_STAGING` | Token de SWA staging |
| `AZURE_STATIC_WEB_APPS_TOKEN_PRODUCTION` | Token de SWA producción |

**Variables:**

| Nombre | Valor |
|--------|-------|
| `AZURE_BACKEND_APP` | `meerkattersd-backend` |
| `AZURE_BACKEND_APP_STAGING` | `meerkattersd-backend-staging` |
| `STAGING_API_URL` | `https://meerkattersd-backend-staging.azurewebsites.net` |
| `PRODUCTION_API_URL` | `https://meerkattersd-backend.azurewebsites.net` |

---

## Paso 8: Verificar Despliegue

### URLs de verificación

| Entorno | Backend | Frontend |
|---------|---------|----------|
| Staging | `https://meerkattersd-backend-staging.azurewebsites.net/api/health` | URL de Static Web App staging |
| Producción | `https://meerkattersd-backend.azurewebsites.net/api/health` | URL de Static Web App producción |

### Comandos útiles

```bash
# Ver logs de producción
az webapp log tail --name meerkattersd-backend --resource-group rg-meerkattersd

# Ver logs de staging
az webapp log tail --name meerkattersd-backend-staging --resource-group rg-meerkattersd

# Reiniciar Web App
az webapp restart --name meerkattersd-backend --resource-group rg-meerkattersd

# Verificar Always On
az webapp config show \
  --name meerkattersd-backend \
  --resource-group rg-meerkattersd \
  --query alwaysOn

# Ver las Web Apps del plan
az webapp list \
  --resource-group rg-meerkattersd \
  --query "[].{name:name, plan:appServicePlanId}" -o table
```

---

## Escalado

### Si las 2 apps van justas de recursos (B1 → B2)

```bash
az appservice plan update \
  --name plan-meerkattersd \
  --resource-group rg-meerkattersd \
  --sku B2
```

| Plan | vCPU | RAM | Coste/Mes |
|------|------|-----|-----------|
| B1 | 1 | 1.75 GB | ~€11 |
| B2 | 2 | 3.5 GB | ~€22 |
| B3 | 4 | 7 GB | ~€43 |

> Escalar el plan afecta a **todas las Web Apps** dentro de él. No hay downtime.

---

## Troubleshooting

### Error: "Always On is not supported"

Always On solo está disponible desde el tier **Basic (B1)**. No está disponible en Free (F1).

### Uso de RAM alto

Con 2 apps Spring Boot en B1 (1.75 GB), vigilar el consumo:

```bash
az monitor metrics list \
  --resource "/subscriptions/<SUB_ID>/resourceGroups/rg-meerkattersd/providers/Microsoft.Web/serverfarms/plan-meerkattersd" \
  --metric "MemoryPercentage" \
  --interval PT1H
```

Si supera el 80% consistentemente, escalar a B2.

### Créditos consumiéndose rápido

1. Verificar en [Cost Management](https://portal.azure.com/#blade/Microsoft_Azure_CostManagement)
2. Confirmar que solo hay **1 plan B1** (no se crearon planes adicionales por error)
3. Verificar que PostgreSQL es B1ms y no un tier superior

---

## Referencias

- [Documentación CI/CD](CI_CD.md)
- [Análisis de Créditos Azure](Analisis_Creditos_Azure_Estudiantes.md)
- [Configuración de Secretos GitHub](Configuracion_Secretos_GitHub.md)
- [Azure App Service Plans](https://learn.microsoft.com/en-us/azure/app-service/overview-hosting-plans)

---

*Documento actualizado el 17 de febrero de 2026*
