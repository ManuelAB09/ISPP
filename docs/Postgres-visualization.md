# Visualizar la base de datos PostgreSQL
![Logo App](../.../images/logoapp.jpeg)

---

**Proyecto:** MeerKatters  
**Autor:** Arquitectura 

---

## Índice

1. [Introducción](#1-introducción)
2. [Requisitos previos](#2-requisitos-previos)
   1. [Instalación de PostgreSQL](#21-instalación-de-postgresql)
   2. [Arranque automático de PostgreSQL](#22-arranque-automático-de-postgresql)
3. [Instalación de pgAdmin (GUI recomendada)](#3-instalación-de-pgadmin-gui-recomendada)
4. [Usar línea de comandos `psql`](#4-usar-línea-de-comandos-psql)
5. [Alternativas web ligeras (opcional)](#5-alternativas-web-ligeras-opcional)
6. [Consejos de seguridad](#6-consejos-de-seguridad)
---
## 1. Introduccíon

Este documento explica cómo instalar las herramientas necesarias para conectarse y visualizar el contenido de la base de datos PostgreSQL que utiliza el backend del proyecto.

## 2. Requisitos previos

- Windows (o cualquier otro sistema en el que se ejecuta el proyecto).
- PostgreSQL ya instalado y en ejecución (ver instrucciones de instalación en la siguiente sección).
- Usuario y contraseña de PostgreSQL (normalmente `postgres` + la contraseña elegida durante la instalación).

## 2.1 Instalación de PostgreSQL

1. Descarga el instalador para tu plataforma desde https://www.postgresql.org/download/.
2. Elige la versión recomendada (por ejemplo PostgreSQL 18) y ejecuta el programa descargado.
3. Durante el asistente acepta las opciones por defecto en cada pantalla; lo único que
   deberás proporcionar es la contraseña para el usuario `postgres` apuntala por si te hace falta después.
   Y de regional por Español (internacional)
4. Termina la instalación y, cuando se te pregunte, permite que el servidor se inicie.
5. Abre una terminal nueva y comprueba que `psql` está en el PATH ejecutando:
   ```powershell
   psql --version
   ```
   Deberías ver algo como `psql (PostgreSQL) 18.x`; si el comando no se encuentra,
   añade manualmente la carpeta `bin` al PATH como se explica a continuación.

Si `psql` sigue sin estar accesible, añade `C:\Program Files\PostgreSQL\18\bin`
al PATH siguiendo estos pasos:

1. Abre **Este equipo → Propiedades → Configuración avanzada del sistema → Variables de entorno**.
2. En **Variables del sistema** edita `Path` y añade una entrada nueva con la ruta anterior.
3. Acepta y cierra; reinicia la terminal.
4. Vuelve a ejecutar psql --version

Con la base instalada ya puedes seguir a la sección 2 para instalar pgAdmin o la
herramienta que prefieras.

---

## 2.2 Arranque automático de PostgreSQL

Para evitar que el backend falle al iniciar (el error `Connection refused`) asegúrate de que el servicio de
PostgreSQL arranca automáticamente al iniciar Windows. Hay dos opciones:



2. **Usar el script de arranque del repositorio**
   Ejecuta `start-backend.ps1` desde la raíz del proyecto. Este script comprueba si el servidor está
   corriendo, lo inicia si es necesario y luego lanza `./mvnw spring-boot:run`.
   ```powershell
   # desde la carpeta del proyecto
   .\start-backend.ps1
   ```

Ahora cualquiera que quiera levantar el backend solo necesita `.\start-backend.ps1` o tener el
servicio en automático; no se requiere abrir `services.msc` cada vez.


---


## 3. Instalación de pgAdmin (GUI recomendada)

1. Descarga pgAdmin desde https://www.pgadmin.org/download/.
2. Elige el instalador adecuado para tu plataforma (Windows, macOS, Linux).
3. Ejecuta el instalador y sigue los pasos del asistente. Asegúrate de aceptar la instalación de los componentes predeterminados y, si te solicita, define una contraseña maestra (solo para abrir pgAdmin).
4. Abre pgAdmin cuando termine la instalación.
5. Crea un nuevo servidor en pgAdmin:
   - Clic derecho sobre **Servers** → **Create** → **Server...**
   - En **General**, pon un nombre descriptivo (por ejemplo `Local Postgres`).
   - En **Connection** completa:
     - **Hostname/address**: `localhost`
     - **Port**: `5432`
     - **Maintenance database**: `postgres`
     - **Username**: `postgres`
     - **Password**: *la contraseña de instalación*
   - Haz clic en **Save**.
6. Después de conectar, expande `Servers → <tu servidor> → Databases → meerkatters` y navega por los esquemas y tablas.

## 4. Usar línea de comandos `psql`

Si prefieres la consola textual, el cliente `psql` viene con PostgreSQL.

1. Asegúrate de que `psql` está en el `PATH`. Si no aparece al ejecutar `psql --version`, añádelo manualmente:  
   a. Navega con el Explorador a `C:\Program Files\PostgreSQL\18\bin` y copia la ruta.  
   b. Ve a **Este equipo → Propiedades → Configuración avanzada del sistema → Variables de entorno**.  
   c. En **Variables del sistema** edita `Path`, pulsa **Nuevo** y pega la ruta copiada.  
   d. Acepta y cierra todas las ventanas; vuelve a abrir la terminal para aplicar el cambio.  
   Ahora `psql` se ejecutará desde cualquier ubicación.  
   *(si prefieres no modificar `PATH`, usa la ruta completa como se muestra a continuación).*  
2. Ejecuta:
   ```powershell
   & 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -U postgres -W
   ```
   Introduce la contraseña cuando se solicite.
3. Comandos útiles dentro de `psql`:
   - `\l` – listar bases de datos
   - `\c meerkatters` – cambiar a la base de la aplicación
   - `\dt` – listar tablas
   - SQL estándar (`SELECT * FROM usuario;`, etc.)
   - `\q` para salir.

## 5. Alternativas web ligeras (opcional)

Si no quieres instalar pgAdmin, existen herramientas web ejecutables en un contenedor Docker:

```powershell
docker run --rm -p 8081:8080 \
    -e ADMINER_DEFAULT_DB_DRIVER=pgsql \
    harrydb/adminer
```

Luego navega a `http://localhost:8081` y usa las mismas credenciales para conectarte.

## 6. Consejos de seguridad

- Nunca subas archivos de log o contraseñas a Git.
- Usa variables de entorno (`SPRING_DATASOURCE_*`) para configurar la aplicación.
- Considera restringir el acceso a PostgreSQL con Firewall si el servidor es público.

## 7. Posibles fallos de instalación

### Error: "La inicialización del clúster de la base de datos falló"

Si durante la instalación de PostgreSQL aparece el mensaje:

> *Problema al ejecutar el paso post instalación. La instalación no pudo finalizar correctamente.*
> *La inicialización del clúster de la base de datos falló.*

Esto suele ocurrir cuando se selecciona un idioma regional incompatible durante la instalación. Para solucionarlo:

1. Desinstala PostgreSQL.
2. Vuelve a ejecutar el instalador.
3. Cuando el asistente te pregunte por el **Locale** (idioma/región), selecciona **Default locale** en lugar de "Español" u otro idioma.
4. Continúa con el resto de la instalación normalmente.

### El script de inicio no crea la base de datos o el usuario

Si al ejecutar `start-backend.ps1` el backend falla porque no existe la base de datos o el usuario, puedes crearlos manualmente desde `psql`:

```sql
CREATE USER meerkatters_user WITH PASSWORD 'meerkatters_password';
CREATE DATABASE meerkatters OWNER meerkatters_user;
GRANT ALL PRIVILEGES ON DATABASE meerkatters TO meerkatters_user;
```

Para ejecutar estos comandos:

1. Abre una terminal y conéctate como superusuario:
   ```powershell
   psql -U postgres -W
   ```
2. Introduce la contraseña del usuario `postgres`.
3. Pega los tres comandos SQL anteriores y pulsa Enter.
4. Verifica con `\l` que la base `meerkatters` aparece en el listado.
5. Sal con `\q` y vuelve a lanzar `.\start-backend.ps1`.

---

Con estas herramientas podrás inspeccionar, consultar y depurar el contenido de la base PostgreSQL usada por el backend.