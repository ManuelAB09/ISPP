# Análisis de Consumo de Créditos Azure for Students

## MeerKatters - Plataforma de Comunidades de Estudio

### Grupo D – Turno de tarde

![Logo App](../../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Documento:** Análisis Técnico / Infraestructura  
**Sprint:** Sprint 0  
**Semana:** Semana 3  
**Estado:** Aprobado  
**Fecha:** 16/02/2026  
**Autor(es):** Raimundo Jiménez Lara, Manuel María Calderón Rodríguez

---

## Índice

- [Resumen Ejecutivo](#resumen-ejecutivo)
- [Crédito Disponible](#crédito-disponible)
  - [Azure for Students](#azure-for-students)
  - [Servicios Gratuitos Incluidos](#servicios-gratuitos-incluidos)
- [Desglose de Costes por Servicio](#desglose-de-costes-por-servicio)
  - [Azure App Service (Aplicación Web)](#1-azure-app-service-aplicación-web)
    - [Backend (Java/Spring Boot)](#backend-javaspring-boot)
    - [Frontend (React/Nginx)](#frontend-reactnginx)
    - [Alternativa: Azure Static Web Apps (Frontend)](#alternativa-azure-static-web-apps-frontend)
  - [Azure Database for PostgreSQL](#2-azure-database-for-postgresql)
    - [Alternativa más económica: Azure SQL Database](#alternativa-más-económica-azure-sql-database)
  - [Servidor de Correos (SMTP)](#3-servidor-de-correos-smtp)
    - [Opción A: Azure Communication Services (Email)](#opción-a-azure-communication-services-email)
    - [Opción B: SendGrid (vía Azure Marketplace)](#opción-b-sendgrid-vía-azure-marketplace)
    - [Opción C: SMTP Propio (VM)](#opción-c-smtp-propio-vm)
  - [Otros Servicios Potenciales](#4-otros-servicios-potenciales)
    - [Azure Blob Storage (Archivos/Imágenes)](#azure-blob-storage-archivosimágenes)
    - [Azure Key Vault (Secretos)](#azure-key-vault-secretos)
    - [Azure Monitor (Logs)](#azure-monitor-logs)
- [Escenarios de Coste](#escenarios-de-coste)
  - [Escenario 1: Mínimo Viable (Solo Desarrollo)](#escenario-1-mínimo-viable-solo-desarrollo)
  - [Escenario 2: Recomendado (Desarrollo + Staging)](#escenario-2-recomendado-desarrollo--staging)
  - [Escenario 3: Completo (Dev + Staging + "Prod")](#escenario-3-completo-dev--staging--prod)
- [Estrategias de Ahorro](#estrategias-de-ahorro)
  - [Apagar Recursos Fuera de Horario](#1-apagar-recursos-fuera-de-horario)
  - [Usar Static Web Apps para Frontend](#2-usar-static-web-apps-para-frontend)
  - [Compartir Base de Datos](#3-compartir-base-de-datos)
  - [Usar Azure SQL Basic en lugar de PostgreSQL](#4-usar-azure-sql-basic-en-lugar-de-postgresql)
  - [Eliminar Recursos No Utilizados](#5-eliminar-recursos-no-utilizados)
- [Calculadora de Duración](#calculadora-de-duración)
  - [Fórmula](#fórmula)
  - [Ejemplos con $100 de crédito](#ejemplos-con-100-de-crédito)
- [Configuración Recomendada para Estudiantes](#configuración-recomendada-para-estudiantes)
  - [Arquitectura Optimizada](#arquitectura-optimizada)
- [Monitoreo de Gastos](#monitoreo-de-gastos)
  - [Configurar Alertas de Presupuesto](#1-configurar-alertas-de-presupuesto)
  - [Ver Gastos Actuales](#2-ver-gastos-actuales)
  - [Portal de Azure](#3-portal-de-azure)
- [Plan de Contingencia](#plan-de-contingencia)
  - [Si te quedas sin crédito:](#si-te-quedas-sin-crédito)
- [Resumen de Costes Mensuales](#resumen-de-costes-mensuales)
  - [Configuración Mínima Recomendada](#configuración-mínima-recomendada)
  - [Duración Estimada con $100](#duración-estimada-con-100)
- [Decisión Final del Equipo](#decisión-final-del-equipo)
  - [Configuración Elegida](#configuración-elegida)
  - [Justificación de las Decisiones](#justificación-de-las-decisiones)
  - [Arquitectura Final](#arquitectura-final)
- [Checklist de Optimización](#checklist-de-optimización)

---

## Resumen Ejecutivo

| Concepto | Valor |
|----------|-------|
| **Crédito Azure for Students** | $100 USD |
| **Consumo mensual estimado (mínimo)** | $25-35 USD |
| **Consumo mensual estimado (recomendado)** | $45-60 USD |
| **Duración estimada del crédito** | **2-4 meses** |

---

## Crédito Disponible

### Azure for Students
- **Crédito inicial**: $100 USD
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
| **F1 (Free)** | Compartido | 1 GB | $0 | ❌ Solo pruebas, se duerme |
| **B1** | 1 | 1.75 GB | ~$13 | ✅ Desarrollo/Staging |
| **B2** | 2 | 3.5 GB | ~$26 | ⚠️ Si necesitas más RAM |
| **S1** | 1 | 1.75 GB | ~$70 | ❌ Demasiado caro |

**Recomendación**: B1 para desarrollo (~$13/mes)

#### Frontend (React/Nginx)

| SKU | Precio/Mes | Recomendación |
|-----|------------|---------------|
| **F1 (Free)** | $0 | ✅ Suficiente para frontend estático |
| **B1** | ~$13 | ⚠️ Solo si Free no funciona |

**Recomendación**: F1 Free ($0/mes) - el frontend es estático y ligero

#### Alternativa: Azure Static Web Apps (Frontend)
- **Free tier**: 100 GB bandwidth, custom domain, SSL gratis
- **Precio**: $0/mes
- **Ideal para**: React, Vue, Angular

**💡 Ahorro**: Usar Static Web Apps para frontend = **$0 vs $13**

---

### 2. Azure Database for PostgreSQL

| SKU | vCores | RAM | Storage | Precio/Mes | Recomendación |
|-----|--------|-----|---------|------------|---------------|
| **Burstable B1ms** | 1 | 2 GB | 32 GB | ~$15 | ✅ Desarrollo |
| **Burstable B2s** | 2 | 4 GB | 32 GB | ~$30 | ⚠️ Si necesitas más |
| **General Purpose D2s** | 2 | 8 GB | 64 GB | ~$130 | ❌ Muy caro |

**Coste adicional de storage**: ~$0.115/GB/mes

**Recomendación**: Burstable B1ms (~$15/mes)

#### Alternativa más económica: Azure SQL Database
| SKU | DTUs | Storage | Precio/Mes |
|-----|------|---------|------------|
| **Basic** | 5 | 2 GB | ~$5 | 
| **S0** | 10 | 250 GB | ~$15 |

**💡 Nota**: Si puedes usar SQL Server en lugar de PostgreSQL, el tier Basic es más barato.

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

**Recomendación**: ✅ Muy económico para proyectos pequeños

#### Opción B: SendGrid (vía Azure Marketplace)
| Plan | Emails/Mes | Precio/Mes |
|------|------------|------------|
| **Free** | 100/día (3,000/mes) | $0 |
| **Essentials** | 40,000 | ~$15 |

**Recomendación**: ✅ Plan Free de SendGrid = $0

#### Opción C: SMTP Propio (VM)
| Concepto | Precio/Mes |
|----------|------------|
| VM B1s | ~$8 |
| IP Pública | ~$3 |
| **Total** | ~$11 |

**Recomendación**: ❌ No recomendado - problemas de reputación IP y deliverability

---

### 4. Otros Servicios Potenciales

#### Azure Blob Storage (Archivos/Imágenes)
| Tier | Precio/GB/Mes | Recomendación |
|------|---------------|---------------|
| Hot | $0.018 | ✅ Acceso frecuente |
| Cool | $0.01 | ⚠️ Acceso ocasional |

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
| App Service Backend | B1 | $13 |
| App Service Frontend | F1 (Free) | $0 |
| PostgreSQL | B1ms | $15 |
| Email (SendGrid Free) | Free | $0 |
| Storage (5 GB) | Hot | $0.10 |
| **TOTAL** | | **~$28/mes** |

**Duración del crédito**: ~3.5 meses

---

### Escenario 2: Recomendado (Desarrollo + Staging)

| Servicio | SKU | Coste/Mes |
|----------|-----|-----------|
| App Service Backend | B1 | $13 |
| Static Web App Frontend | Free | $0 |
| PostgreSQL | B1ms | $15 |
| Azure Communication Services | Pay-as-you-go | $1 |
| Blob Storage (10 GB) | Hot | $0.20 |
| Key Vault | Standard | $0.50 |
| **TOTAL** | | **~$30/mes** |

**Duración del crédito**: ~3.3 meses

---

### Escenario 3: Completo (Dev + Staging + "Prod")

| Servicio | SKU | Coste/Mes |
|----------|-----|-----------|
| App Service Backend (Staging) | B1 | $13 |
| App Service Backend (Prod) | B1 | $13 |
| Static Web App (ambos) | Free | $0 |
| PostgreSQL (Staging) | B1ms | $15 |
| PostgreSQL (Prod) | B1ms | $15 |
| Azure Communication Services | Pay-as-you-go | $2 |
| Blob Storage (20 GB) | Hot | $0.40 |
| Key Vault | Standard | $1 |
| **TOTAL** | | **~$59/mes** |

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

**Ahorro potencial**: Si apagas 12h/día = ~$6.50/mes en App Service

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

**Ahorro**: $13/mes por entorno

### 3. Compartir Base de Datos

Usar una sola instancia de PostgreSQL con múltiples bases de datos:

```sql
-- En lugar de 2 servidores, usar 2 bases de datos en 1 servidor
CREATE DATABASE meerkattersd_staging;
CREATE DATABASE meerkattersd_production;
```

**Ahorro**: $15/mes

### 4. Usar Azure SQL Basic en lugar de PostgreSQL

Si puedes adaptar la aplicación:

| PostgreSQL B1ms | Azure SQL Basic |
|-----------------|-----------------|
| $15/mes | $5/mes |

**Ahorro**: $10/mes

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

### Ejemplos con $100 de crédito

| Coste Mensual | Duración |
|---------------|----------|
| $25 | 4 meses |
| $30 | 3.3 meses |
| $40 | 2.5 meses |
| $50 | 2 meses |
| $60 | 1.7 meses |

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
│  └──────────────────────────────────────────────────────┘   │
│                            │                                 │
│                            ▼                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              App Service B1 (~$13)                    │   │
│  │                 Backend Spring Boot                   │   │
│  │           (compartido staging/prod)                   │   │
│  └──────────────────────────────────────────────────────┘   │
│                            │                                 │
│                            ▼                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         PostgreSQL Flexible B1ms (~$15)               │   │
│  │              2 bases de datos:                        │   │
│  │         meerkattersd_dev / meerkattersd_prod          │   │
│  └──────────────────────────────────────────────────────┘   │
│                            │                                 │
│                            ▼                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │             SendGrid Free (3000 emails)               │   │
│  │                    o                                  │   │
│  │      Azure Communication Services (1000 free)         │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│                    COSTE TOTAL: ~$28/mes                     │
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
| Backend | App Service B1 | $13 |
| Frontend | Static Web Apps Free | $0 |
| Base de Datos | PostgreSQL B1ms | $15 |
| Email | SendGrid Free | $0 |
| Storage | Incluido en App Service | $0 |
| **TOTAL** | | **$28/mes** |

### Duración Estimada con $100

| Configuración | Coste/Mes | Duración |
|---------------|-----------|----------|
| Mínima | $28 | **3.5 meses** |
| Con 2 entornos | $43 | **2.3 meses** |
| Completa | $59 | **1.7 meses** |

---

## Decisión Final del Equipo

### Configuración Elegida

Tras analizar las opciones y considerando los 6 despliegues adicionales para demos de sprint, el equipo ha decidido la siguiente arquitectura:

| Componente | Decisión | Plataforma | Coste |
|------------|----------|------------|-------|
| **Backend** | Docker + Deployment Slots | Azure App Service B1 + Slot | **$13/mes** |
| **Frontend** | Docker → Static Web App | Azure Static Web Apps | **$0** |
| **Base de Datos** | 1 servidor, 2 schemas | Azure PostgreSQL B1ms | $15/mes |
| **Demos Sprint** | Docker containers | Render Free × 6 | **$0** |
| **Email** | SendGrid Free | Azure Marketplace | $0 |

### Justificación de las Decisiones

#### 1. Método de Despliegue: Docker + Deployment Slots
- ✅ **Elegido Docker** para mantener consistencia entre entornos local/staging/prod
- ✅ **Elegido Deployment Slots** para staging y producción en 1 solo App Service
- Aprovecha los Dockerfiles ya configurados en el proyecto
- Mayor control sobre el entorno de ejecución
- **Ventajas de Slots**: Zero-downtime deployments, rollback instantáneo, warm-up antes de swap
- **Ahorro**: $13/mes vs tener 2 App Services separados

#### 2. Frontend: Azure Static Web Apps
- ✅ **Elegido Static Web Apps** en lugar de App Service
- **Ahorro**: $26/mes (staging + prod)
- Perfecto para SPA React, incluye CDN y SSL gratis

#### 3. Base de Datos: Servidor Compartido
- ✅ **Elegido 1 servidor PostgreSQL** con 2 schemas (staging + prod)
- **Ahorro**: $15/mes vs tener 2 servidores separados
- Suficiente para fase de desarrollo

#### 4. Demos de Sprint: Render Free
- ✅ **Elegido Render Free** para los 6 despliegues de estado de sprint
- **Ahorro**: ~$78/mes vs Azure App Service
- Hibernan cuando no se usan (perfecto para demos puntuales)
- Cold start de ~30s aceptable para demos

### Arquitectura Final

```
┌─────────────────────────────────────────────────────────────────────┐
│                         AZURE ($100 créditos)                        │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │              Azure Static Web Apps (GRATIS)                     │ │
│  │         ┌─────────────────┐  ┌─────────────────┐               │ │
│  │         │ Frontend        │  │ Frontend        │               │ │
│  │         │ Staging         │  │ Producción      │               │ │
│  │         └─────────────────┘  └─────────────────┘               │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌────────────────────────────────────────┐  ┌─────────────────────┐ │
│  │         App Service B1 (~$13/mes)       │  │ PostgreSQL B1ms     │ │
│  │  ┌─────────────┐  ┌─────────────────┐   │  │ (1 servidor)        │ │
│  │  │   Slot:     │  │   Slot:         │   │  │ - schema staging    │ │
│  │  │  Staging    │◄─┤   Production    │   │  │ - schema production │ │
│  │  │  (Docker)   │  │   (Docker)      │   │  │ ~$15/mes            │ │
│  │  └─────────────┘  └─────────────────┘   │  └─────────────────────┘ │
│  │          ↑ swap ↓                       │                          │
│  │    Deploy → Staging → Test → Swap       │                          │
│  └────────────────────────────────────────┘                          │
│                                                                      │
│                    TOTAL AZURE: ~$28/mes                             │
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
│  → TOTAL RENDER: $0/mes                                             │
└─────────────────────────────────────────────────────────────────────┘
```

### Coste Mensual Final

| Servicio | Coste/Mes |
|----------|-----------|
| App Service B1 + Staging Slot | $13 |
| Static Web Apps | $0 |
| PostgreSQL B1ms (compartido) | $15 |
| Render (demos) | $0 |
| **TOTAL** | **~$28/mes** |

### Duración Estimada del Crédito

| Crédito | Coste Mensual | Duración |
|---------|---------------|----------|
| $100 | $28 | **~3.5 meses** |

> **Nota**: Gracias al uso de Deployment Slots ahorramos $13/mes y ganamos workflow profesional de despliegue.

---

## Checklist de Optimización

- [x] Usar Static Web Apps para frontend (ahorro: $26/mes)
- [x] Compartir PostgreSQL entre entornos (ahorro: $15/mes)
- [x] Usar Render Free para demos de sprint (ahorro: ~$78/mes)
- [x] Usar SendGrid Free para emails (ahorro: $15/mes)
- [x] Usar Deployment Slots en lugar de 2 App Services (ahorro: $13/mes)
- [ ] Configurar alertas de presupuesto
- [ ] Revisar gastos semanalmente en Cost Management

---

*Análisis generado el 16 de febrero de 2026*  
*Decisión final documentada el 16 de febrero de 2026*  
*Precios aproximados basados en región West Europe, pueden variar*
