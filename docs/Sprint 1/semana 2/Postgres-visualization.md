# Visualizar la base de datos PostgreSQL

Este documento explica cómo instalar las herramientas necesarias para conectarse y visualizar el contenido de la base de datos PostgreSQL que utiliza el backend del proyecto.

## 1. Requisitos previos

- Windows (o cualquier otro sistema en el que se ejecuta el proyecto).
- PostgreSQL ya instalado y en ejecución (ver instrucciones de instalación en la siguiente sección).
- Usuario y contraseña de PostgreSQL (normalmente `postgres` + la contraseña elegida durante la instalación).

## 1.1 Instalación de PostgreSQL

1. Descarga el instalador para tu plataforma desde https://www.postgresql.org/download/.
2. Elige la versión recomendada (por ejemplo PostgreSQL 18) y descarga el instalador gráfico para Windows.
3. Ejecuta el instalador y sigue los pasos del asistente:
   - Selecciona la carpeta de destino (deja la predeterminada).
   - Define la contraseña del superusuario `postgres`; recuerda este valor.
   - Deja el puerto por defecto (`5432`) y la codificación UTF‑8.
   - Marca la casilla para añadir `psql` al PATH si te lo ofrece.
4. Espera a que terminen todos los componentes (servidor, pgAdmin opcional, StackBuilder).
5. Comprueba que el servidor está corriendo abriendo `Services` (buscar "PostgreSQL"), o ejecuta:
   ```powershell
   & 'C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe' status -D 'C:\Program Files\PostgreSQL\18\data'
   ```
6. Si `psql` no está en el PATH, úsalo con la ruta completa como en la sección siguiente.

Con la base instalada ya puedes seguir a la sección 2 para instalar pgAdmin o la herramienta que prefieras.

---

## 1.2 Arranque automático de PostgreSQL

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


## 2. Instalación de pgAdmin (GUI recomendada)

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

## 3. Usar línea de comandos `psql`

Si prefieres la consola textual, el cliente `psql` viene con PostgreSQL.

1. Asegúrate de que `psql` está en el `PATH`. Si no, su ruta típica es:
   `C:\Program Files\PostgreSQL\<versión>\bin\psql.exe`.
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

## 4. Alternativas web ligeras (opcional)

Si no quieres instalar pgAdmin, existen herramientas web ejecutables en un contenedor Docker:

```powershell
docker run --rm -p 8081:8080 \
    -e ADMINER_DEFAULT_DB_DRIVER=pgsql \
    harrydb/adminer
```

Luego navega a `http://localhost:8081` y usa las mismas credenciales para conectarte.

## 5. Consejos de seguridad

- Nunca subas archivos de log o contraseñas a Git.
- Usa variables de entorno (`SPRING_DATASOURCE_*`) para configurar la aplicación.
- Considera restringir el acceso a PostgreSQL con Firewall si el servidor es público.

---

Con estas herramientas podrás inspeccionar, consultar y depurar el contenido de la base PostgreSQL usada por el backend.