# Visualizar la base de datos PostgreSQL

Este documento explica cómo instalar las herramientas necesarias para conectarse y visualizar el contenido de la base de datos PostgreSQL que utiliza el backend del proyecto.

## 1. Requisitos previos

- Windows (o cualquier otro sistema en el que se ejecuta el proyecto).
- PostgreSQL ya instalado y en ejecución (ver instrucciones de instalación en la siguiente sección).
- Usuario y contraseña de PostgreSQL (normalmente `postgres` + la contraseña elegida durante la instalación).

## 1.1 Instalación de PostgreSQL

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