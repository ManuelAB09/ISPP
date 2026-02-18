# Análisis de Consumo de Créditos Azure for Students

## MeerKatters - Plataforma de Comunidades de Estudio

### Grupo 9 – Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Análisis Técnico / Infraestructura  
**Sprint:** Sprint DP  
**Semana:** Semana 3  
**Estado:** Aprobado  
**Fecha:** 16/02/2026  
**Autor(es):** Raimundo Jiménez Lara, Manuel María Calderón Rodríguez

---

## Resumen Ejecutivo

| Concepto | Valor |
|----------|-------|
| **Crédito Azure for Students** | $100 USD (≈€83.72) |
| **Consumo mensual estimado (elegido)** | ~€23.50/mes |
| **Duración estimada del crédito** | **~3.5 meses** |

> Precios: Linux, West Europe, EUR. 1 USD = 0.8372 EUR (Azure Pricing, febrero 2026).

---

## Crédito Disponible

### Azure for Students
- **Crédito inicial**: $100 USD (≈€83.72 a tasa actual)
- **Validez**: 12 meses desde la activación
- **Renovable**: Sí, cada año académico mientras seas estudiante
- **Sin tarjeta de crédito**: No requiere tarjeta para activar

### Servicios Gratuitos Incluidos
Además del crédito, tienes acceso a servicios gratuitos durante 12 meses:
- 750 horas/mes de VM B1s (Linux)
- 250 GB de Azure Blob Storage
- 5 GB de Azure Files
- Azure DevOps (básico)

---

## Desglose de Costes por Servicio

### 1. Azure App Service (Aplicación Web)

#### Backend (Java/Spring Boot)

| SKU | vCPU | RAM | Precio/Mes | Recomendación |
|-----|------|-----|------------|---------------|
| **F1 (Free)** | Compartido | 1 GB | €0 | Solo pruebas, se duerme |
| **B1** | 1 | 1.75 GB | ~€11 | Desarrollo/Staging |
| **B2** | 2 | 3.5 GB | ~€22 | Si necesitas más RAM |
| **S1** | 1 | 1.75 GB | ~€58 | Demasiado caro |

**Recomendación**: B1 para desarrollo (~€11/mes)

#### Frontend (React/Nginx)

| SKU | Precio/Mes | Recomendación |
|-----|------------|---------------|
| **F1 (Free)** | €0 | Suficiente para frontend estático |
| **B1** | ~€11 | Solo si Free no funciona |

**Recomendación**: F1 Free (€0/mes) - el frontend es estático y ligero

#### Alternativa: Azure Static Web Apps (Frontend)
- **Free tier**: 100 GB bandwidth, custom domain, SSL gratis
- **Precio**: €0/mes
- **Ideal para**: React, Vue, Angular

**Ahorro**: Usar Static Web Apps para frontend = **€0 vs €11**

---

### 2. Azure Database for PostgreSQL

| SKU | vCores | RAM | Storage | Precio/Mes | Recomendación |
|-----|--------|-----|---------|------------|---------------|
| **Burstable B1ms** | 1 | 2 GB | 32 GB | ~€12.50 | Desarrollo |
| **Burstable B2s** | 2 | 4 GB | 32 GB | ~€25 | Si necesitas más |
| **General Purpose D2s** | 2 | 8 GB | 64 GB | ~€109 | Muy caro |

**Coste adicional de storage**: ~€0.10/GB/mes

**Recomendación**: Burstable B1ms (~€12.50/mes)

#### Alternativa más económica: Azure SQL Database
| SKU | DTUs | Storage | Precio/Mes |
|-----|------|---------|------------|
| **Basic** | 5 | 2 GB | ~$5 | 
| **S0** | 10 | 250 GB | ~$15 |

**Nota**: Si puedes usar SQL Server en lugar de PostgreSQL, el tier Basic es más barato.

---

### 3. Servidor de Correos (SMTP)

#### Opción A: Azure Communication Services (Email)
| Concepto | Precio |
|----------|--------|
| Primeros 1,000 emails/mes | **GRATIS** |
| Emails adicionales | $0.00025/email |

**Estimación**: 
- 500 emails/mes = $0
- 5,000 emails/mes = ~$1

**Recomendación**: Muy económico para proyectos pequeños

#### Opción B: SendGrid (vía Azure Marketplace)
| Plan | Emails/Mes | Precio/Mes |
|------|------------|------------|
| **Free** | 100/día (3,000/mes) | $0 |
| **Essentials** | 40,000 | ~$15 |

**Recomendación**: Plan Free de SendGrid = $0

#### Opción C: SMTP Propio (VM)
| Concepto | Precio/Mes |
|----------|------------|
| VM B1s | ~$8 |
| IP Pública | ~$3 |
| **Total** | ~$11 |

**Recomendación**: No recomendado - problemas de reputación IP y deliverability

---

### 4. Otros Servicios Potenciales

#### Azure Blob Storage (Archivos/Imágenes)
| Tier | Precio/GB/Mes | Recomendación |
|------|---------------|---------------|
| Hot | $0.018 | Acceso frecuente |
| Cool | $0.01 | Acceso ocasional |

**Estimación** (10 GB): ~$0.20/mes

#### Azure Key Vault (Secretos)
- **Primeras 10,000 operaciones**: GRATIS
- **Precio por secreto**: ~$0.03/mes por secreto

**Estimación**: ~$0.50/mes

#### Azure Monitor (Logs)
- **5 GB de logs/mes**: GRATIS
- **Retención 90 días**: GRATIS

**Estimación**: $0 (si no excedes 5 GB)

---

## Escenarios de Coste

### Escenario 1: Mínimo Viable (Solo Desarrollo)

| Servicio | SKU | Coste/Mes |
|----------|-----|-----------|
| App Service Backend | B1 | €11 |
| App Service Frontend | F1 (Free) | €0 |
| PostgreSQL | B1ms | €12.50 |
| Email (SendGrid Free) | Free | €0 |
| Storage (5 GB) | Hot | €0.10 |
| **TOTAL** | | **~€23.60/mes** |

**Duración del crédito**: ~3.5 meses (€83.72 / €23.60)

---

### Escenario 2: Recomendado (Desarrollo + Staging)

| Servicio | SKU | Coste/Mes |
|----------|-----|-----------|
| App Service Backend | B1 | €11 |
| Static Web App Frontend | Free | €0 |
| PostgreSQL | B1ms | €12.50 |
| Azure Communication Services | Pay-as-you-go | €0.85 |
| Blob Storage (10 GB) | Hot | €0.17 |
| Key Vault | Standard | €0.42 |
| **TOTAL** | | **~€25/mes** |

**Duración del crédito**: ~3.3 meses

---

### Escenario 3: Completo (Dev + Staging + "Prod")

| Servicio | SKU | Coste/Mes |
|----------|-----|-----------|
| App Service Backend (Staging) | B1 | €11 |
| App Service Backend (Prod) | B1 | €11 |
| Static Web App (ambos) | Free | €0 |
| PostgreSQL (Staging) | B1ms | €12.50 |
| PostgreSQL (Prod) | B1ms | €12.50 |
| Azure Communication Services | Pay-as-you-go | €1.70 |
| Blob Storage (20 GB) | Hot | €0.34 |
| Key Vault | Standard | €0.85 |
| **TOTAL** | | **~€50/mes** |

**Duración del crédito**: ~1.7 meses

---

## Estrategias de Ahorro

### 1. Apagar Recursos Fuera de Horario

```powershell
# Script para apagar staging (ahorrar ~50% del coste de App Service)
# Añadir a Azure Automation o ejecutar manualmente

# Apagar a las 20:00
az webapp stop --name meerkattersd-backend-staging --resource-group rg-staging

# Encender a las 08:00
az webapp start --name meerkattersd-backend-staging --resource-group rg-staging
```

**Ahorro potencial**: Si apagas 12h/día = ~€5.50/mes en App Service

### 2. Usar Static Web Apps para Frontend

En lugar de App Service para el frontend:

```yaml
# En tu workflow de CD, cambiar a Static Web Apps
- name: Deploy to Azure Static Web Apps
  uses: Azure/static-web-apps-deploy@v1
  with:
    azure_static_web_apps_api_token: ${{ secrets.AZURE_STATIC_WEB_APPS_API_TOKEN }}
    action: "upload"
    app_location: "/frontend"
    output_location: "build"
```

**Ahorro**: €11/mes por entorno

### 3. Compartir Base de Datos

Usar una sola instancia de PostgreSQL con múltiples bases de datos:

```sql
-- En lugar de 2 servidores, usar 2 bases de datos en 1 servidor
CREATE DATABASE meerkattersd_staging;
CREATE DATABASE meerkattersd_production;
```

**Ahorro**: €12.50/mes

Si puedes adaptar la aplicación:

| PostgreSQL B1ms | Azure SQL Basic |
|-----------------|-----------------|
| €12.50/mes | ~€4/mes |

**Ahorro**: ~€8.50/mes

### 5. Eliminar Recursos No Utilizados

```powershell
# Ver recursos y sus costes
az consumption usage list --top 10

# Eliminar grupos de recursos completos cuando no se necesiten
az group delete --name rg-meerkattersd-staging --yes
```

---

## Calculadora de Duración

### Fórmula
```
Duración (meses) = Crédito Disponible / Coste Mensual
```

### Ejemplos con €83.72 de crédito ($100 USD)

| Coste Mensual | Duración |
|---------------|----------|
| €20 | 4.2 meses |
| €23.50 | 3.5 meses |
| €25 | 3.3 meses |
| €40 | 2.1 meses |
| €50 | 1.7 meses |

---

## Configuración Recomendada para Estudiantes

### Arquitectura Optimizada

```
┌─────────────────────────────────────────────────────────────┐
│                    CONFIGURACIÓN ESTUDIANTES                 │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │            Azure Static Web Apps (FREE)               │   │
│  │                    Frontend React                     │   │
│  │              staging + production                     │   │
│  │                       €0/mes                          │   │
│  └──────────────────────────────────────────────────────┘   │
│                            │                                 │
│                            ▼                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │        App Service Plan B1 (~€11/mes)                 │   │
│  │  ┌─────────────────┐  ┌─────────────────────────┐    │   │
│  │  │ Web App Staging │  │ Web App Producción      │    │   │
│  │  │ (Docker)        │  │ (Docker)                │    │   │
│  │  │ Always On: Si    │  │ Always On: Si            │    │   │
│  │  └─────────────────┘  └─────────────────────────┘    │   │
│  └──────────────────────────────────────────────────────┘   │
│                            │                                 │
│                            ▼                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         PostgreSQL Flexible B1ms (~€12.50)            │   │
│  │              2 schemas:                               │   │
│  │         meerkattersd_staging / meerkattersd_prod      │   │
│  └──────────────────────────────────────────────────────┘   │
│                            │                                 │
│                            ▼                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │             SendGrid Free (3000 emails)               │   │
│  │                    o                                  │   │
│  │      Azure Communication Services (1000 free)         │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│                    COSTE TOTAL: ~€23.50/mes                  │
│                    DURACIÓN: ~3.5 meses                      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Monitoreo de Gastos

### 1. Configurar Alertas de Presupuesto

```powershell
# Crear alerta cuando llegues al 50% del crédito
az consumption budget create `
  --budget-name "AlertaEstudiante" `
  --amount 100 `
  --category Cost `
  --time-grain Monthly `
  --start-date "2026-02-01" `
  --end-date "2027-02-01"
```

### 2. Ver Gastos Actuales

```powershell
# Ver consumo del mes actual
az consumption usage list --query "[].{Servicio:instanceName, Coste:pretaxCost}" --output table

# Ver resumen por servicio
az cost management query `
  --type Usage `
  --timeframe MonthToDate `
  --dataset-grouping name=ServiceName type=Dimension
```

### 3. Portal de Azure

1. Ve a **Cost Management + Billing**
2. Selecciona tu suscripción de estudiante
3. Ve a **Cost analysis** para ver gráficos de consumo

---

## Plan de Contingencia

### Si te quedas sin crédito:

1. **Solicitar más crédito**: Contacta a tu universidad, algunos tienen acuerdos con Microsoft para crédito adicional.

2. **Azure for Students Starter**: Si expira tu crédito, puedes usar servicios limitados gratuitamente.

3. **Migrar a alternativas gratuitas**:
   - Railway.app (free tier)
   - Render.com (free tier)
   - Supabase (PostgreSQL gratis hasta 500MB)
   - Vercel (frontend gratis)

4. **GitHub Student Developer Pack**: Incluye:
   - $200 en créditos de Azure (adicionales)
   - DigitalOcean $200
   - Heroku $13/mes durante 2 años

---

## Resumen de Costes Mensuales

### Configuración Mínima Recomendada

| Servicio | Opción | Coste/Mes |
|----------|--------|-----------|
| Backend (staging + prod) | 1 App Service Plan B1, 2 Web Apps, Always On | ~€11 |
| Frontend | Static Web Apps Free × 2 | €0 |
| Base de Datos | PostgreSQL B1ms (1 servidor, 2 schemas) | ~€12.50 |
| Email | SendGrid Free | €0 |
| Storage | Incluido en App Service | €0 |
| **TOTAL** | | **~€23.50/mes** |

### Duración Estimada con €83.72 ($100 USD)

| Configuración | Coste/Mes | Duración |
|---------------|-----------|----------|
| Elegida (1 plan B1, 2 Web Apps) | ~€23.50 | **~3.5 meses** |
| Con B2 si insuficiente | ~€35 | **~2.4 meses** |

---

## Decisión Final del Equipo

### Configuración Elegida

Tras analizar las opciones y considerando que **el tier B1 (Basic) NO soporta deployment slots** (requiere Standard S1 ~€58/mes, inviable con créditos de estudiante), el equipo ha decidido usar **2 Web Apps en un mismo App Service Plan B1**:

| Componente | Decisión | Plataforma | Coste |
|------------|----------|------------|-------|
| **Backend (staging + prod)** | Docker, Always On en ambas | 1 App Service Plan B1 (2 Web Apps) | **~€11/mes** |
| **Frontend** | SPA React | Azure Static Web Apps (Free) × 2 | **€0** |
| **Base de Datos** | 1 servidor, 2 schemas | Azure PostgreSQL B1ms | **~€12.50/mes** |
| **Demos Sprint** | Docker containers | Render Free × 6 | **€0** |
| **Email** | SendGrid Free | Azure Marketplace | €0 |

### Justificación de las Decisiones

#### 1. Método de Despliegue: Docker + 1 Plan B1 con 2 Web Apps
- **Elegido Docker** para mantener consistencia entre entornos local/staging/prod
- **1 plan B1 con 2 Web Apps** (staging + producción), ya que B1 no soporta slots
- **Descartados Deployment Slots**: requieren Standard S1 (~€58/mes), inviable
- **Descartados tiers Free (F1) y Shared (D1)**: NO soportan Docker en Linux
- Aprovecha los Dockerfiles ya configurados en el proyecto
- Mayor control sobre el entorno de ejecución
- **Always On habilitado en ambas Web Apps** (disponible desde B1): sin cold starts
- 2 Spring Boot (~300-500 MB cada uno) caben en 1.75 GB RAM del B1

#### 2. 1 Plan B1 en lugar de 2 planes separados
- **Un único plan B1 (~€11/mes)** aloja staging y producción
- Un App Service Plan es la unidad de facturación (CPU + RAM). Las Web Apps son apps dentro de él.
- Se pueden crear **tantas Web Apps como se quiera** en un plan de pago, sin coste adicional.
- Ambas comparten 1 vCPU + 1.75 GB RAM — suficiente para 2 Spring Boot.
- **Plan de escalado**: si el B1 es insuficiente → escalar a B2 (~€22/mes, 2 vCPU, 3.5 GB) sin cambiar estructura.

#### 3. Frontend: Azure Static Web Apps
- **Elegido Static Web Apps** en lugar de App Service
- **Ahorro**: frente a B1 para frontend (~€11/mes extra)
- Perfecto para SPA React, incluye CDN y SSL gratis

#### 4. Base de Datos: Servidor Compartido
- **Elegido 1 servidor PostgreSQL** con 2 schemas (staging + prod)
- **Ahorro**: ~€12.50/mes vs tener 2 servidores separados
- Suficiente para fase de desarrollo

#### 5. Demos de Sprint: Render Free
- **Elegido Render Free** para los 6 despliegues de estado de sprint
- **Ahorro** significativo vs Azure App Service
- Hibernan cuando no se usan (perfecto para demos puntuales)
- Cold start de ~30s aceptable para demos

### Arquitectura Final

```
┌─────────────────────────────────────────────────────────────────────┐
│                       AZURE (€83.72 créditos)                        │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │              Azure Static Web Apps (GRATIS)                    │  │
│  │         ┌─────────────────┐  ┌─────────────────┐              │  │
│  │         │ Frontend        │  │ Frontend        │              │  │
│  │         │ Staging         │  │ Producción      │              │  │
│  │         └─────────────────┘  └─────────────────┘              │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │              App Service Plan B1 (~€11/mes)                    │  │
│  │     1 vCPU  │  1.75 GB RAM  │  Always On  │  Linux             │  │
│  │                                                                │  │
│  │  ┌─────────────────────┐  ┌─────────────────────────────────┐ │  │
│  │  │  Web App: Staging   │  │  Web App: Producción            │ │  │
│  │  │  (Docker)           │  │  (Docker)                       │ │  │
│  │  │  Always On: Si       │  │  Always On: Si                   │ │  │
│  │  └─────────────────────┘  └─────────────────────────────────┘ │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │              PostgreSQL Flexible B1ms (~€12.50/mes)            │  │
│  │         schema staging    │    schema production              │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                      │
│                    TOTAL AZURE: ~€23.50/mes                          │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                      RENDER (FREE TIER)                              │
│                                                                      │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐           │
│  │ Demo      │ │ Demo      │ │ Demo      │ │ Demo      │ ...       │
│  │ Sprint 1  │ │ Sprint 2  │ │ Sprint 3  │ │ Sprint N  │           │
│  │ (Back+    │ │ (Back+    │ │ (Back+    │ │ (Back+    │           │
│  │  Front)   │ │  Front)   │ │  Front)   │ │  Front)   │           │
│  └───────────┘ └───────────┘ └───────────┘ └───────────┘           │
│                                                                      │
│  → Hibernan tras 15min de inactividad                               │
│  → Cold start ~30s cuando se accede                                 │
│  → TOTAL RENDER: €0/mes                                             │
└─────────────────────────────────────────────────────────────────────┘
```

### Coste Mensual Final

| Servicio | Coste/Mes |
|----------|-----------|
| 1 App Service Plan B1 – 2 Web Apps (Always On) | ~€11 |
| Static Web Apps × 2 | €0 |
| PostgreSQL B1ms (1 servidor, 2 schemas) | ~€12.50 |
| Render (demos) | €0 |
| **TOTAL** | **~€23.50/mes** |

### Duración Estimada del Crédito

| Crédito | Coste Mensual | Duración |
|---------|---------------|----------|
| €83.72 ($100 USD) | ~€23.50 | **~3.5 meses** |

> **Nota**: Al usar **1 solo plan B1** con 2 Web Apps, el coste se reduce significativamente frente a la alternativa de 2 planes separados. Ambas Web Apps tienen Always On, eliminando cold starts. Si se necesita más capacidad, escalar a B2 (~€22/mes, 2 vCPU, 3.5 GB).

---

## Checklist de Optimización

- [x] Usar Static Web Apps para frontend (ahorro vs App Service)
- [x] Compartir PostgreSQL entre entornos (1 servidor, 2 schemas)
- [x] Usar Render Free para demos de sprint
- [x] Usar SendGrid Free para emails
- [x] 1 solo plan B1 con 2 Web Apps (staging + prod) — ahorro vs 2 planes
- [x] Always On en ambas Web Apps (sin cold starts)
- [ ] Configurar alertas de presupuesto
- [ ] Revisar gastos semanalmente en Cost Management

---

*Análisis generado el 16 de febrero de 2026*  
*Decisión final documentada el 16 de febrero de 2026*  
*Precios aproximados basados en región West Europe, pueden variar*
