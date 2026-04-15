# MeerKatters – Seguimiento de Errores Sprint 2

---

## 🔐 Autenticación

### Botones de registro con Google y LinkedIn no funcionan (no implementado)
- ¿Lo solucionaste? **sin info  **
- ¿En caso de que no, por qué? 
- 🔗 Issue: *(sin enlace)*

---

### Los términos de servicio y política de privacidad no muestran contenido al clicar el enlace
- ¿Lo solucionaste? **Sí.** Julio García Barrena añadió las pantallas legales con contenido real en `frontend/src/screens/legal/Terms.js` (línea 5) y `frontend/src/screens/legal/Privacy.js` (línea 5), y el registro enlaza a ellas desde `frontend/src/screens/auth/Register.js` (líneas 367-368).
- ¿En caso de que no, por qué? —

---

### Si no se aceptan los términos y condiciones no aparece ningún aviso claro al intentar registrarse
- ¿Lo solucionaste? **Sí.** Julio García Barrena añadió la validación `if (!formData.acceptTerms)` en `frontend/src/screens/auth/Register.js` (líneas 83-85), y Álvaro añadió un aviso visible junto a la casilla en `frontend/src/screens/auth/Register.js` (líneas 357 y 371-372).
- ¿En caso de que no, por qué? —

---

### Al volver desde términos de servicio el formulario de registro se reinicia (pérdida de datos introducidos)
- ¿Lo solucionaste? **Sin info**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### No hay límite de caracteres claro en la contraseña al registrarse; la restricción de máximo 128 caracteres no se indica antes de escribir; contraseñas muy largas o con caracteres multibyte provocan error 500
- ¿Lo solucionaste? **Parcialmente resuelto.** Manuel Jesús añadió validaciones de longitud en el formulario de registro junto con otras validaciones generales, incluyendo `.trim()` en contraseñas.
- ¿En caso de que no, por qué? La indicación visual del límite antes de escribir aún puede no estar completamente implementada.
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/257

---

### No se puede elegir rol al registrarse: se crea siempre como estudiante automáticamente

- ¿Lo solucionaste?*
  Nora y Juan propusieron una solución alternativa:
  - Añadir un botón en el perfil que permita convertirse en profesor.
  - Incluir un *switch* durante el registro para que el usuario pueda elegir si quiere registrarse como profesor.

- ¿En caso de que no, por qué?  
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/251

---

### Se permiten correos temporales (sin validación de dominio universitario)
- ¿Lo solucionaste? **No resuelto.** Laura lo arregló inicialmente pero hubo que revertir el cambio.
- ¿En caso de que no, por qué? La validación de dominio universitario entraba en conflicto con la autenticación mediante Google, que también usa el mismo campo de email. Se decidió quitarla para no romper ese flujo.
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/448

---

### No se envía correo de confirmación al registrarse (no verifica que el correo pertenezca al usuario)
- ¿Lo solucionaste? **Sin info**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Se permite repetir el mismo nombre de usuario
- ¿Lo solucionaste? **Sin info**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Validación del campo email demasiado laxa en el registro
- ¿Lo solucionaste? **Sí.** Manuel Jesús añadió validación de regex para el email en el formulario de registro (`frontend/src/screens/auth/Register.js`).
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/257

---

### El campo email del formulario de registro rechaza caracteres Unicode / no-ASCII (ej. caracteres chinos), impidiendo registrar correos con dichos caracteres aunque sean técnicamente válidos
- ¿Lo solucionaste? **Sin info**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### No se especifican las condiciones de una contraseña válida antes de rellenar el formulario de registro
- ¿Lo solucionaste? **Si** Esto se arreglo poniendo las mismas validaciones en frontend y backend
- ¿En caso de que no, por qué? 

---

### Los iconos de los campos de correo y contraseña se superponen con el placeholder y con el texto al escribir (problema de padding en login)
- ¿Lo solucionaste? **Si** Se quitaron los iconos de correo y contraseña para que no superpongan el place holder
- ¿En caso de que no, por qué? 
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/249

---

### Botón de recuperar contraseña no implementado (redirige a otra pantalla sin realizar ninguna acción)
- ¿Lo solucionaste? **Sí.** Se implementó el flujo completo de recuperación con `frontend/src/screens/auth/ForgotPassword.js` (línea 7), `frontend/src/screens/auth/ResetPassword.js` (línea 10), las rutas en `frontend/src/App.js` (líneas 145-146) y los endpoints `POST /api/v1/auth/password/forgot` y `POST /api/v1/auth/password/reset` en `backend/src/main/java/es/us/meerkat/backend/controller/users/AuthController.java` (líneas 188 y 204).
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/418

---

### Los botones "Notas compartidas" y "Grupos de estudio" aparecen en la pantalla de login/inicio pero no tienen funcionalidad
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint. Funcionalidades no implementadas.
- 🔗 Issue: *(sin enlace)*

---

### El flujo de autenticación con Google es confuso: si el correo ya existe exige vinculación manual desde ajustes en vez de unificar cuentas en el momento
- ¿Lo solucionaste? **No resuelto.** Fran revisó el flujo y concluyó que no había error real en los despliegues.
- ¿En caso de que no, por qué? Fran comprobó que en producción y pre-producción el inicio de sesión con Google funcionaba correctamente. El problema reportado era posiblemente debido a una configuración local incorrecta de algunos desarrolladores (variables de entorno desactualizadas).
- 🔗 Issue: *No se creo, ya que no se encontró el error.*

---

### Falta doble confirmación de contraseña en el registro
- ¿Lo solucionaste? **Si** Se duplicó el campo de entrada de contraseña y se creó una validación que confirme que los valores de ambos campos coinciden
- ¿En caso de que no, por qué? 
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/249

---

### Hay textos en "spanglish" en la interfaz de login/registro
- ¿Lo solucionaste? Se unifico todos los textos y dejarlos completamente en español.
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.

---

### El botón de cerrar sesión está demasiado escondido dentro de la configuración (difícil de encontrar)
- ¿Lo solucionaste? **Sí.** Juan Moreno movió la acción de cerrar sesión al encabezado visible del perfil en `frontend/src/screens/myProfile/Profile.js` (línea 493).
- ¿En caso de que no, por qué? —

---

## 👤 Perfil de usuario

### No hay acceso visible al perfil (solo accesible escribiendo /perfil en la URL)
- ¿Lo solucionaste? **Sí.** Manuel Artero añadió acceso directo al perfil desde la cabecera mediante `Link to="/perfil"` en `frontend/src/components/Header/Header.jsx` (línea 94).
- ¿En caso de que no, por qué? —

---

### La cuenta desapareció al día siguiente de crearla (bug crítico)
- ¿Lo solucionaste? se desestimó porque se interpretó que había sido al limpiar la base de datos
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint. Bug no replicado consistentemente.
- 🔗 Issue: Issue no creada por ser imposible de replicar.

---

### No se puede eliminar una cuenta que ha sufrido un error previo de contraseña (error 500)
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Al editar el perfil con textos demasiado largos aparece error 500 en vez de mensaje de validación (ej. descripción personal > ~500 caracteres)
- ¿Lo solucionaste? **Parcialmente resuelto.** Manuel Jesús añadió validaciones de longitud en varios campos del formulario de edición de perfil (`EditProfile.js`), añadiendo universidad y grado como obligatorios.
- ¿En caso de que no, por qué? Pueden quedar campos sin cubrir.
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/257

---

### Cuando el usuario no tiene foto de perfil aparece un icono de imagen rota en vez de la imagen por defecto
- ¿Lo solucionaste? **Sí.** Álvaro añadió un `DEFAULT_PROFILE_AVATAR` y `onError` de fallback en `frontend/src/screens/myProfile/Profile.js` (líneas 19, 426 y 429), evitando que el perfil muestre imágenes rotas.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/419

---

### El icono de acceso al perfil (navbar/cabecera) no muestra la imagen del usuario en ninguna página antes de que se haya subido una foto; aparece como imagen no cargada
- ¿Lo solucionaste? **Sí.** Manuel Artero dejó la cabecera usando `DEFAULT_PROFILE_AVATAR` y un `onError` explícito en `frontend/src/components/Header/Header.jsx` (líneas 11, 94, 97 y 100), de modo que el icono del perfil siempre tenga imagen de respaldo.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/419

---

### La foto de perfil no carga correctamente en algunas partes de la app (ej. chat); el icono de acceso al perfil tampoco muestra la imagen
- ¿Lo solucionaste? **Sí.** JosemaMG reforzó los fallbacks de avatar en chat con `DEFAULT_PROFILE_AVATAR` y manejadores `handleProfileImageError` / `handleProfileAvatarError` en `frontend/src/screens/chat/Chats.js` (líneas 18, 328-330) y `frontend/src/screens/chat/CommunityChat.js` (líneas 20, 95-97 y 871-873). Además, la cabecera del perfil usa el mismo fallback en `frontend/src/components/Header/Header.jsx` (línea 100).
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/419

---

### Al eliminar la foto subida no se vuelve a la imagen por defecto: aparece texto con el nombre de usuario o icono de error; el cambio tampoco se persiste sin recargar la página
- ¿Lo solucionaste? **Si** Alvaro: Cuando un usuario no tenía foto de perfil, el componente intentaba mostrar una imagen con URL null/undefined, lo que provocaba un icono de error o que se cayera al fallback de texto plano con el nombre completo. Añadí lógica de fallback que muestra una imagen por defecto (/default-profile.png) cuando ProfilePicture es nulo
- ¿En caso de que no, por qué?

---

### Al volver a editar el perfil por segunda vez después de haber subido una imagen, el botón de eliminar dicha imagen no funciona (no responde o no aparece disponible)
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### El mensaje de error al subir una imagen no válida aparece al final de la ventana emergente, fuera de la zona visible sin hacer scroll
- ¿Lo solucionaste? **Sí.** Alvaro movió el mensaje de error de imagen junto al bloque de subida en `frontend/src/screens/myProfile/EditProfile.js` (líneas 457-458), y las validaciones de formato y tamaño están en las líneas 262 y 268.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/419

---

### Los campos "Universidad", "Grado" y "Ubicación" aceptan cualquier texto libre sin validación (deberían usar desplegable o autocompletado con valores reales)
- ¿Lo solucionaste? **Parcialmente resuelto.** Manuel Jesús añadió universidad y grado como campos obligatorios en EditProfile, pero sin validar que correspondan a valores reales (sin desplegable ni autocompletado).
- ¿En caso de que no, por qué? Implementar un desplegable con universidades/grados reales requiere una fuente de datos externa o una lista estática extensa que no se abordó en este sprint.
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/257


---

### El rol "profesor" no explica qué implica ni la interfaz refleja diferencias visibles entre ambos tipos de perfil
- ¿Lo solucionaste? **Sí.** Nora añadió un modal explicativo que aparece cuando el usuario se convierte en profesor, y diferenció visualmente el perfil del tutor cambiando el color de la cabecera a azul marino (frente al blanco del estudiante).
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/443

---

### Faltan campos relevantes en el perfil: nivel de estudios, base formativa, selección de rol (profesor/alumno)
- ¿Lo solucionaste? **Parcialmente resuelto.** Mario añadió `nivelEstudios` y `baseFormativa` al formulario y a la vista de perfil en `frontend/src/screens/myProfile/EditProfile.js` (líneas 577-608) y `frontend/src/screens/myProfile/Profile.js` (líneas 336-337 y 526-532). El cambio de rol a profesor también quedó visible desde el perfil en `frontend/src/screens/myProfile/Profile.js` (líneas 681-694), pero no hay un selector general profesor/alumno dentro del formulario de edición.
- ¿En caso de que no, por qué? La parte confirmable en código cubre los nuevos campos y el paso a tutor, pero no un selector de rol simétrico dentro del formulario de perfil.

---

### La configuración del perfil no se puede guardar (los cambios de ajustes no se persisten tras cerrar)
- ¿Lo solucionaste? **Si**. las propiedades no estaban en backend solo esta mockeado, asi que se creo la parte de backend
- ¿En caso de que no, por qué? 

---

### La sección de configuración está mal estructurada (ej. botón de cerrar sesión colocado en zona de peligro cuando no es una acción de riesgo)
- ¿Lo solucionaste? *Si** Se realizo un reajuste de componentes
- ¿En caso de que no, por qué? 

---

### Hay un vacío visual en la parte derecha del perfil (problema de diseño/layout)
- ¿Lo solucionaste? **Sí.** Nora cambió la estructura del componente de perfil para eliminar el espacio en blanco inconsistente.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/443

---

### No existe forma de buscar otros usuarios / No se puede acceder al perfil de otros usuarios desde chats, comunidades o asistentes de un evento
- ¿Lo solucionaste? **Sí.** Alejandro Ruíz añadió en el menú grande una nueva pestaña llamada **Alumnos** que funciona como buscador dinámico de otros alumnos, permitiendo además entrar en el perfil de cada usuario encontrado para consultar su información. Adicionalmente, Mario amplió el acceso al perfil desde otros contextos: los asistentes de la vista de detalle de evento son ahora botones clicables que navegan a `/perfil/{id}` (`frontend/src/screens/evento/DetalleEvento.js`), se añadió un botón "Ver perfil" en el header del chat privado (`frontend/src/screens/chat/Chats.js`) y en la lista de conversaciones del tutor (`frontend/src/screens/teacherProfile/TutorConversaciones.js`).
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/435 / https://github.com/ManuelAB09/ISPP/issues/434

---

### Al ver el perfil de alguien sin foto, las iniciales se sustituyen por su nombre completo en texto plano pequeño
- ¿Lo solucionaste? **Si** Cuando un usuario no tenía foto de perfil, el componente intentaba mostrar una imagen con URL null/undefined, lo que provocaba un icono de error o que se cayera al fallback de texto plano con el nombre completo. Añadí lógica de fallback que muestra una imagen por defecto (/default-profile.png) cuando ProfilePicture es nulo (Alvaro)
- ¿En caso de que no, por qué? 

---

### En el perfil ajeno, el apartado "mis comunidades" no se corresponde al perfil consultado sino al del usuario propio
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Inconsistencia en validación de contraseña al cambiarla: al registrar exige mayúscula, minúscula, etc.; al cambiarla solo pide 6-8 caracteres; el error de longitud indica "cannot be more than 72 bytes" (no comprensible); cambiar contraseña a 6-7 caracteres provoca error 500
- ¿Lo solucionaste? **Parcialmente resuelto.** Manuel Jesús añadió en settings que la nueva contraseña deba ser distinta a la anterior, con `.trim()` adicionales en las validaciones. Los demás aspectos (coherencia de requisitos, mensaje de error técnico) no se han abordado completamente.
- ¿En caso de que no, por qué? El mensaje técnico "72 bytes" viene del backend y requeriría ser interceptado y traducido en el frontend.
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/257

---

### Se permite cambiar la contraseña por la misma que ya tenías sin mostrar ningún aviso
- ¿Lo solucionaste? **Sí.** Manuel Jesús añadió en settings la validación de que la nueva contraseña sea distinta a la contraseña actual.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/257

---

### Notificaciones Push y doble factor de autenticación no tienen respuesta del sistema al activarse (no implementados); muchas categorías de notificación importantes vienen desactivadas por defecto
- ¿Lo solucionaste? **Parcialmente resuelto.** Cynthia arregló la conectividad de las notificaciones push del chat: antes solo aparecían dentro de las comunidades, y se modificó la lógica para que también salgan siempre que el usuario esté fuera del chat.
- ¿En caso de que no, por qué? La parte relativa al doble factor de autenticación y la configuración por defecto de muchas categorías de notificación no queda confirmada como resuelta con esta intervención.

---

## 💬 Chat

### El chat está demasiado pegado a la barra de tareas (la barra tapa la zona de escritura; se "soluciona" en pantalla completa pero es un bug de diseño responsive)
- ¿Lo solucionaste? **Sí.** Laura solucionó este y el resto de problemas de responsive, revisando todas las pantallas en pantalla completa, reducida y formato móvil.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/448

---

### El botón "Abrir grande" tiene mala usabilidad/diseño
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### El indicador "en línea" aparece dividido en dos líneas
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### La foto de perfil no se muestra correctamente en el chat si el usuario no tiene imagen
- ¿Lo solucionaste? **Si** Alvaro: Cuando un usuario no tenía foto de perfil, el componente intentaba mostrar una imagen con URL null/undefined, lo que provocaba un icono de error o que se cayera al fallback de texto plano con el nombre completo. Añadí lógica de fallback que muestra una imagen por defecto (/default-profile.png) cuando ProfilePicture es nulo
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.

---

### Al eliminar un mensaje en el chat, el resto de usuarios deben recargar la página para verlo eliminado (no es automático)
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint. Requiere mejora del sistema de sincronización en tiempo real (WebSocket/polling).
- 🔗 Issue: *(sin enlace)*

---

### Al enviar textos demasiado largos, el recuadro del mensaje se expande en horizontal a lo largo de la página (no se pueden ver los dos extremos del chat a la vez)
- ¿Lo solucionaste? **Sí.** Laura arregló este problema como parte de la revisión general de responsive, corrigiendo que los mensajes muy largos o archivos con nombres largos se salían de los cuadros en los chats.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/448

---

### No se puede navegar entre chat de comunidades y chats personales sin perder el contexto (hay que volver a entrar en la pantalla de chats)
- ¿Lo solucionaste? **Sí.** Manuel Nuño añadió pestañas y selector móvil para alternar entre comunidades y conversaciones privadas en `frontend/src/screens/chat/Chats.js` (líneas 91, 397-420, 448-458 y 632-644), manteniendo el estado en `activeTab`.
- ¿En caso de que no, por qué? —

---

### Al abrir el chat hay que reducir el zoom al 75% para que se pueda ver correctamente
- ¿Lo solucionaste? **Sí.** Laura solucionó este problema como parte de la revisión general de responsive.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/448

---

## 🏘️ Comunidades

### Error al crear comunidades: subir imagen desde el ordenador provoca error 500; al fallar la creación cuenta como intento del plan gratuito sin haberse creado la comunidad
- ¿Lo solucionaste? **No resuelto completamente.** Fran probó en local y en los despliegues y no reprodujo el fallo, por lo que lo dio por cerrado. El contador de intentos al fallar tampoco se menciona como solucionado.
- ¿En caso de que no, por qué? El error no era reproducible en los entornos de prueba del desarrollador asignado en el momento de la revisión.
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/437

---

### Se puede subir cualquier tipo de archivo como imagen al crear/editar comunidad (vídeos, documentos, etc.) sin restricción de formato ni indicación de requisitos de imagen
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Si se intenta crear una comunidad con el nombre vacío no aparece mensaje de error adecuado (se queda en "creando..." o no responde)
- ¿Lo solucionaste? **Sí.** Alejandro Ruíz comprobó que el formulario no permite crear la comunidad mientras el campo nombre esté vacío, por lo que la acción queda bloqueada antes del envío. Además, si el nombre tiene menos de 3 caracteres, sí se muestra correctamente un mensaje de error indicando que debe introducirse un nombre mayor de 3 caracteres.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/435

---

### La funcionalidad de guardar borrador de comunidad no está implementada
- ¿Lo solucionaste? **Sí.** Nora implementó `handleGuardarBorrador()` en `CrearComunidad.js` (línea 134), que guarda el borrador en `localStorage` bajo `crearComunidadDraft`, muestra mensaje de éxito y redirige a `/mis-borradores`. Al reabrir el formulario, el borrador se recarga desde `localStorage` (línea 161).
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/427

---

### Falta posibilidad de crear comunidad directamente desde la vista de comunidades
- ¿Lo solucionaste? **Sí.** Nora añadió en `Comunidades.js` (línea 56) un botón "Crear comunidad" visible para usuarios autenticados que navega directamente a `/crear-comunidad`.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/427

---

### Los filtros de comunidades no son funcionales; solo se puede buscar por título; el icono de lupa no hace nada; no hay palabras clave ni filtro por categorías
- ¿Lo solucionaste? **Sí.** Cynthia amplió los filtros de búsqueda de comunidades, añadiendo nuevas opciones para filtrar por nombre, descripción, tipo de plan, institución y categorías, completando así una búsqueda que antes era muy limitada.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/428

---

### Introducir demasiados caracteres en la barra de búsqueda de comunidades provoca error
- ¿Lo solucionaste? **Sí.** Julio implementó un límite de caracteres en la barra de búsqueda de comunidades.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/431

---

### El botón "Editar" en la sección de comunidades creadas no está implementado: redirige a una pantalla en blanco
- ¿Lo solucionaste? Se conectó el botón a la pantalla de edición correspondiente.
- ¿En caso de que no, por qué? 

---

### La edición de comunidad solo permite modificar título y descripción; faltan aforo, categorías y otros atributos
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### No aparece ningún distintivo visible en las comunidades creadas que indique si son públicas o privadas
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### No se indica en ningún sitio de "Explorar comunidades" cómo puede un usuario acceder a una comunidad privada
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### No se pueden establecer reglas de acceso para comunidades privadas (solo se puede definir público/privado durante la creación)
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### El mensaje de confirmación al eliminar una comunidad no tiene la estética de la aplicación
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Error "Conflicto de datos al guardar. Revisa que la franja no se solape con otra existente." al eliminar comunidad en casos concretos (sin causa aparente clara)
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint. La causa raíz del error no fue identificada.
- 🔗 Issue: *(sin enlace)*

---

### Cuando se recibe una solicitud de comunidad privada, al dar en aceptar o rechazar el sistema no hace nada; hay error 500 por consola
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### La notificación de solicitud de acceso a comunidad privada no le llega al administrador (la opción de notificación está desactivada por defecto y no debería ser opcional)
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Las solicitudes de acceso a comunidad privada no aparecen en el apartado de notificaciones
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Límite de caracteres en tags de comunidad: poner demasiados caracteres genera bug visual
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### En algún caso concreto, tras unirse a una comunidad pública sigue apareciendo el botón "Unirse"
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Al abandonar una comunidad el sistema no redirige a ninguna pantalla alternativa (el usuario se queda en la misma vista sin retroalimentación clara sobre adónde ir)
- ¿Lo solucionaste? **Sí.** Alejandro Ruíz corrigió la redirección al abandonar una comunidad.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/435

---

### La acción de transferir administrador requiere recargar la página para verse actualizada
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### No se pueden moderar cuestionarios ni anuncios (solo se pueden moderar eventos)
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Una vez concedido el rol de administrador a un miembro no se puede revocar; además un administrador puede expulsar a otros administradores, lo que permite "adueñarse" de la comunidad
- ¿Lo solucionaste? **Parcialmente resuelto.** Se añadió una protección en `backend/src/main/java/es/us/meerkat/backend/service/MemberService.java` y `frontend/src/screens/comunidades/CommunityDetail.js` para impedir que se expulse al único administrador de la comunidad. La revocación de rol de administrador ya concedido sigue sin implementarse.
- ¿En caso de que no, por qué? — 

---

### La gestión de roles en comunidades es demasiado limitada: solo existen los roles administrador y miembro; no hay roles intermedios (ej. moderador) pese a que el sistema los contempla en la especificación
- ¿Lo solucionaste? **Parcialmente resuelto.** Se añadió soporte para el rol **docente/profesor** dentro de la gestión de comunidades `backend/.../controller/CommunityController.java`, `frontend/src/screens/comunidades/CommunityDetail.js` y `frontend/src/utils/communityRoles.js`. Ahora un miembro puede tener simultáneamente rol de profesor y de administrador. No se implementaron roles intermedios tipo moderador.
- ¿En caso de que no, por qué? El sistema no contemplaba el rol docente en la gestión de comunidades; se priorizó este caso sobre roles intermedios genéricos.

---

### Al poner a un miembro como administrador del chat y que ese miembro intente gestionar el chat (guardar cambios, expulsar miembros, quitar admin a otros) el sistema devuelve "No tienes permisos para gestionar este grupo" pese a tener rol de administrador asignado
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Los botones "Notas compartidas" y "Grupos de estudio" son visibles en comunidades pero no tienen ninguna funcionalidad
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint. Funcionalidades no implementadas.
- 🔗 Issue: *(sin enlace)*

---

## 📅 Eventos

### No está claro cómo crear eventos ni existe opción de crear eventos independientes fuera de una comunidad
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Aunque se seleccione privado, los eventos se crean siempre como públicos
- ¿Lo solucionaste? **Sí.** Cynthia corrigió la creación del evento para que la propiedad de privacidad deje de enviarse siempre como `false` y pase a tomar correctamente el valor introducido por el usuario en el formulario.
- ¿En caso de que no, por qué? —

---

### Se pueden crear eventos con fecha pasada
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Al introducir un dato erróneo en la fecha se produce error 500 en vez de mensaje de validación; tras el error ya no se puede modificar nada
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### El sistema permite introducir una dirección de ubicación inventada o inexistente al crear un evento (ej. coordenadas 0,0 con nombre ficticio)
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Unirse a ciertos eventos pasados produce error 500; en otros eventos pasados el sistema permite unirse sin error cuando no debería
- ¿Lo solucionaste? **No replicable.** Manuel Jesús intentó reproducir el error pero una vez pasada la fecha de fin de los eventos, estos desaparecen de la interfaz y no permiten la unión. No pudo confirmar si el bug sigue presente.
- ¿En caso de que no, por qué? Los eventos pasados ya no aparecen en la interfaz, lo que hace imposible reproducir el fallo tal como fue reportado.
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/438

---

### El sistema permite cancelar asistencia después de que el evento haya terminado
- ¿Lo solucionaste? **Sí.** Nora corrigió esto en el backend en `AsistenciaEventoService.java` (línea 146), añadiendo una comprobación de `fechaFin` o `fechaHora` y lanzando error si el evento ya terminó. En el frontend también se ocultó el botón cuando `isEnded` es verdadero en `DetalleEvento.js` (línea 1208).
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/427

---

### En Inicio > Mis eventos > Historial aparecen eventos a los que el usuario no ha asistido, incluyendo eventos anteriores a la creación de la cuenta
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Guardar cambios al editar un evento produce error interno del servidor (500)
- ¿Lo solucionaste? **Sí.** Juan Antonio corrigió el fallo que se producía cuando la comunidad asociada al evento era nula. Se añadió una validación defensiva antes de ejecutar la lógica de notificaciones, evitando así el error interno. Julio también verificó que al guardar ya no daba error 500.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/439 / https://github.com/ManuelAB09/ISPP/issues/431

---

### Al editar la ubicación de un evento no se actualiza al guardar; además redirige a la página anterior que se había abierto (comportamiento sin sentido)
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### En la vista de comunidad, los eventos cancelados siguen mostrando el botón "Apuntarse" (aunque internamente bloquea la operación)
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Si un evento es privado no aparece en el mapa aunque el usuario pertenezca a su comunidad
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### La distinción entre evento público y privado carece de utilidad práctica: los eventos públicos dentro de una comunidad solo los ven los propios miembros (no se publicitan al exterior), por lo que el alcance real de ambas opciones es el mismo
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint. Es una decisión de diseño de producto que requiere discusión.
- 🔗 Issue: *(sin enlace)*

---

### Los asistentes no son notificados al cancelar un evento si la opción de notificación no estaba activada; dicha opción está desmarcada por defecto
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Se permite añadir más asistentes del máximo configurado para el evento
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### La lista de asistentes a un evento no muestra el rol del usuario (si es profesor o alumno), lo que impide distinguir el tipo de asistente
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

###  Ver ubicaciones recomendadas: funcionalidad no visible o localizable en algunos casos; al aumentar el radio o cambiar categoría de búsqueda lanza errores aleatorios ("Error buscando ubicaciones")
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### No hay límite definido para la cantidad de eventos que se pueden crear dentro de una comunidad
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Cualquier miembro de una comunidad (no solo el administrador) puede crear eventos dentro de ella sin restricción de rol ni límite de cantidad
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Al cancelar un evento desaparece completamente; sería preferible que los eventos cancelados se mantuvieran visibles al menos para el creador
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Al intentar cancelar un evento que no es tuyo, en vez de mostrar un mensaje de "no permitido", puede producirse un error de servidor
- ¿Lo solucionaste? **Sí.** Juan Moreno creó `handleResponseStatusException` para centralizar el manejo de este tipo de errores y evitar que la cancelación de un evento ajeno termine en un error interno del servidor, mostrando en su lugar una respuesta controlada al usuario.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/431 / https://github.com/ManuelAB09/ISPP/issues/429

---

### Al guardar borrador de evento no se guardaba realmente
- ¿Lo solucionaste? **Sí.** Nora implementó `handleSaveDraft()` en `CrearEvento.js` (línea 463), que guarda el formulario en `localStorage` dentro de `eventDrafts` junto con `savedAt`. Al volver desde otras vistas, el borrador se recupera mediante `location.state.eventFormDraft` en `CrearEvento.js` (línea 93).
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/427

---

### Falta sección "Tus borradores" para visualizar borradores de comunidades y eventos
- ¿Lo solucionaste? **Sí.** Nora creó la pantalla `MisBorradores.js` (línea 123), que lee `eventDrafts` y `crearComunidadDraft` desde `localStorage`, los separa en pestañas de eventos y comunidades, y permite continuar o eliminar borradores. Se añadió la ruta `/mis-borradores` en `App.js` y el acceso desde el header en `Header.jsx` (línea 130).
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/427

---

## 📁 Contenido (Archivos)

### La subida de archivos solo es posible desde el chat de comunidades; el botón "Subir apuntes" en una comunidad redirige a una página en blanco (no implementado)
- ¿Lo solucionaste? **Sí.** Manuel Nuño eliminó el botón de "Subir apuntes" de todos los sitios donde aparecía, al no existir la funcionalidad correspondiente.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/440

---

### Subir un PDF provoca error sin indicar el motivo; no se especifican los formatos disponibles ni el tamaño máximo permitido
- ¿Lo solucionaste? **Resuelto según las pruebas realizadas.** Julio y Fran probaron la subida de PDF en todos los chats y en los despliegues y dejaba subir PDFs correctamente. No se encontró el error.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/431 / https://github.com/ManuelAB09/ISPP/issues/437

---

### Al eliminar un archivo en el chat, el resto de usuarios deben recargar la página para que desaparezca el mensaje (no es automático)
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint. Requiere mejora del sistema de sincronización en tiempo real.
- 🔗 Issue: *(sin enlace)*

---

### Problema en la subida de archivos desde chat privado (no desde chat de comunidad)
- ¿Lo solucionaste? **Sí.** Juan Antonio reforzó la resolución del destinatario y endureció el manejo de errores en el flujo de mensajes privados, para que el comportamiento fuera consistente también fuera del chat de comunidad.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/439

---

## 💳 Pricing y Pagos

### El diseño de la página de pricing es confuso y los planes no explican claramente las ventajas
- ¿Lo solucionaste? **Sí.** Nora detalló más las ventajas de los planes en las cajas del frontend.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/256

---

### Las ventajas de los planes aumentan justo en la pantalla de pago (poco transparente)
- ¿Lo solucionaste? **Sí.** Nora cohesionó las ventajas para que salieran iguales en todos los sitios del flujo de compra.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/256

---

### El IVA se añade al final del proceso de pago (posible problema con normativa europea)
- ¿Lo solucionaste? **Sí.** Nora corrigió el flujo para incluir el IVA desde el inicio, mostrando siempre el precio final (precio + IVA) desde el primer paso.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/256

---

### El campo nombre del titular permite introducir símbolos (ej. @); el número de tarjeta acepta cualquier secuencia de números
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### En "Tu suscripción" / "Mis suscripciones" no aparece el plan contratado individualmente; inconsistencia entre la vista de "Planes" y "Mis suscripciones" al cambiar de plan
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### En "Mis pagos" no aparecen datos de la transacción realizada; hay error 500 en consola
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### No se puede cancelar la suscripción ni la renovación automática (no está implementado o no es localizable)
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Al cancelar la suscripción vuelve inmediatamente al plan gratuito en lugar de mantener el premium hasta el fin del período pagado
- ¿Lo solucionaste? **Sí.** Manuel Nuño corrigió el fallo para que, al cancelar la suscripción, esta se mantenga activa hasta la fecha límite establecida y no se revierta inmediatamente al plan gratuito.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/440

---

### Las validaciones de campos del formulario de suscripción (nombre, email, dominio) se ejecutan al clicar "Continuar el pago" en vez de hacerse en tiempo real antes de ese paso
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### No se puede retroceder después de "Continuar pago" para modificar datos; si se cierra la ventana sin completar y se reintenta, el sistema no permite usar el mismo dominio
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Se puede contratar plan Pro junto con Premium sin cancelar el anterior; no queda claro cómo gestionar plan individual tras plan institucional
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Lógica de pricing con comunidades: si un usuario gratuito se hace premium sus comunidades siguen siendo gratuitas; si se cancela el plan las comunidades premium siguen siéndolo; si un premium traspasa la administración a un gratuito la comunidad sigue siendo premium; si un usuario sale de una comunidad habiendo llegado al límite no puede crear otra
- ¿Lo solucionaste? **Sí.** Fran corrigió en backend el caso de que al hacerse premium las comunidades existentes se actualicen. Juan Moreno amplió `cancelarSuscripcion()` para que, tras persistir la cancelación, consulte las comunidades del usuario con `comunidadRepository.findByCreadorId(usuarioId)` y degrade a `FREE` con `maxMiembros = 30` las que sigan con `tipoPlan == PREMIUM`. Además, Juan Moreno añadió al final de `transferAdmin()` una comprobación que, si la comunidad es `PREMIUM`, consulta la suscripción activa del nuevo administrador mediante `suscripcionService.obtenerMiSuscripcion(newAdminId)` y la degrada a `FREE` con `maxMiembros = 30` si el nuevo admin no tiene plan `PREMIUM` ni `PRO`. Juan Antonio corrigió el desajuste en el límite de comunidades para que el conteo tenga en cuenta tanto al creador como al administrador. Manuel Nuño corrigió el caso en que un usuario que sale de una comunidad y ha llegado al límite no podía crear otra.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/440 / https://github.com/ManuelAB09/ISPP/issues/439 / https://github.com/ManuelAB09/ISPP/issues/437 / https://github.com/ManuelAB09/ISPP/issues/429
---

## 🧑‍🏫 Profesores

### Error al cargar la lista de profesores ("No se pudieron cargar los profesores"); el botón de reintentar no funciona
- ¿Lo solucionaste? **Sí.** Nora y Juan corrigieron el error cambiando la query SQL en producción.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/251

---

### Las validaciones de campos al crear/editar perfil de profesor no son específicas: solo muestra mensaje genérico sin indicar qué campo falla
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### En el perfil de profesor, el avatar por defecto muestra el nombre completo del usuario en texto en lugar de las iniciales, al igual que ocurre con perfiles de otros usuarios sin foto
- ¿Lo solucionaste? **Si** Cuando un usuario no tenía foto de perfil, el componente intentaba mostrar una imagen con URL null/undefined, lo que provocaba un icono de error o que se cayera al fallback de texto plano con el nombre completo. Añadí lógica de fallback que muestra una imagen por defecto (/default-profile.png) cuando ProfilePicture es nulo (Alvaro)
- ¿En caso de que no, por qué? 

---

### La valoración de profesor no está completamente implementada; la fecha aparece en formato americano
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### No se puede valorar a un profesor el mismo día de la sesión contratada; el sistema parece requerir que haya pasado la fecha para permitir valorar
- ¿Lo solucionaste? **Sí.** Manuel Artero cambió la condición de valoración para que dependa de que el evento haya empezado (`isStarted`) y no de que haya terminado, en `frontend/src/screens/evento/DetalleEvento.js` (línea 1327). El cálculo de `isStarted` está en la línea 229.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/415

---

### No hay opción de filtrar para mostrar solo profesores verificados (aparecen primero pero no hay filtro exclusivo)
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Falta contenido o ejemplos de profesores para entender cómo funciona la función docente en la plataforma
- ¿Lo solucionaste? **Sí.** Nora y Juan añadieron ejemplos de profesores en el despliegue para facilitar la comprensión de la funcionalidad.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/251

---

### No se eliminan los mensajes predeterminados al dar a "Contactar" a un profesor (el chat se instancia con mensajes de plantilla visibles)
- ¿Lo solucionaste? **Sí.** Juan Moreno corrigió el problema revisando la lógica del mensaje automático inicial. El saludo predeterminado residía en el estado local `mensajes`, de modo que cuando el usuario lo eliminaba y la lista volvía a quedar vacía (`mensajes.length === 0`), se re-disparaba el mismo `useEffect` y el mensaje se reenviaba indefinidamente. La solución inicial fue introducir `autoStartFiredRef`, una referencia con `useRef` que actúa como guardia de un solo uso. Posteriormente, se optó por un enfoque más simple: **eliminar directamente el envío automático** del mensaje `"¡Hola! Me gustaría contactar contigo."` en `frontend/src/screens/chat/PrivateChat.js`, de modo que el flujo `autoStart` ya solo limpia el parámetro de la URL sin enviar ningún mensaje.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/431 / https://github.com/ManuelAB09/ISPP/issues/429

---

###  Error al crear cuestionarios ligados a eventos
- ¿Lo solucionaste? **No replicable en el momento de la revisión.** Manuel Jesús realizó diversas pruebas creando cuestionarios y no logró reproducir el error, por lo que es posible que haya sido arreglado anteriormente durante el desarrollo.
- ¿En caso de que no, por qué? No fue posible replicar el fallo.
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/438

---

###  Error al visualizar cuestionarios
- ¿Lo solucionaste? **No replicable en el momento de la revisión.** Manuel Jesús realizó diversas pruebas y no logró reproducir el error, por lo que es posible que haya sido arreglado anteriormente durante el desarrollo.
- ¿En caso de que no, por qué? No fue posible replicar el fallo.
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/438

---
### Búsqueda de alumnos para cuestionarios por email. Hacerlo lista
- ¿Lo solucionaste? **Sí.** Alejandro Ruíz modificó la creación de cuestionarios para que, cuando se elige publicarlos para personas concretas, ya no haya que introducir manualmente los correos separados por comas. En su lugar, se añadió una búsqueda dinámica de alumnos desde la que se pueden seleccionar usuarios y dejarlos añadidos en una lista.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/435

---

### Redirección de botón volver en el preview de cuestionario
- ¿Lo solucionaste? **Sí.** Alejandro Ruíz ajustó la navegación del botón de volver en la pantalla de previsualización del cuestionario para que no regrese simplemente a la vista anterior. Ahora, si el cuestionario se responde dentro de una comunidad, redirige de nuevo a esa comunidad; en los demás casos, vuelve a la pestaña de cuestionarios públicos.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/435

---


## 🗺️ Mapas y ubicación

### El sistema permite introducir una ubicación inventada en el perfil del usuario y usarla para buscar profesores cercanos
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Eventos privados no aparecen en el mapa aunque el usuario pertenezca a la comunidad; algunos eventos aparecen fuera de los parámetros del radio de búsqueda configurado
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### En el mapa de selección de ubicación de evento aparecen demasiados conventos y cementerios al hacer zoom; los filtros de búsqueda por dirección son insuficientes (ej. calles con el mismo nombre en distintas provincias)
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint. Depende de la API de Google Maps y sus parámetros de filtrado.
- 🔗 Issue: *(sin enlace)*

---

## 📹 Videollamadas

### El cronómetro de la sala de videollamada muestra valores incorrectos desde el inicio: con duración configurada en 60 min muestra "Restante: 00:00" y el tiempo activo aparece directamente con 2 horas
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

## 🎨 UI general / Responsive

### Iconos y placeholders se solapan en formularios
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Faltan signos de apertura en español (¿) en la interfaz
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### La página inicial carga a mitad de scroll
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### Algunos headers tienen mal diseño o padding
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---

### El buscador de precios (min-max €) corta palabras
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*


---

### Problemas de responsive: algunos contenidos no se ven si la ventana no está en pantalla completa
- ¿Lo solucionaste? **Sí.** Laura revisó y arregló todos los problemas de responsive encontrados: textos largos que se salían de los cuadros, botones que se solapaban, mensajes y archivos con nombres largos que se salían en los chats, y problemas en formato móvil y ventana reducida.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/448

---

### La landing page se percibe poco diferenciada (sensación de diseño genérico)
- ¿Lo solucionaste? **Sí.** Juan Antonio realizó un rediseño visual y de contenido de la landing para dar más identidad al producto.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/439

---

## ✅ Validación y datos

### No hay validación de países/ubicaciones (se pueden introducir datos absurdos en campos de ubicación, universidad o grado)
- ¿Lo solucionaste? **Parcialmente resuelto.** Manuel Jesús buscó en el código campos relacionados con países sin encontrar ninguno específico. Añadió universidad y grado como campos obligatorios en EditProfile pero sin validar que sean valores reales. No se pudo añadir validación de país al no existir dicho campo en el código.
- ¿En caso de que no, por qué? No existe un campo de país explícito en el código del proyecto que pudiera validarse.
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/257

---

### Falta opción "Otros" en intereses / opciones de preferencias insuficientes
- ¿Lo solucionaste? **Sí.** Manuel Jesús añadió la opción "Otros" en la pantalla de `EditProfile.js`.
- ¿En caso de que no, por qué? —
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/257

---

### Faltan validaciones en formularios en general: campos obligatorios vacíos aceptados, nombres inválidos (números, símbolos), correos mal formados, sin límite de caracteres que provocan error 500 al excederse
- ¿Lo solucionaste? **Parcialmente resuelto.** Manuel Jesús añadió: validación de regex para email en el registro, universidad y grado como obligatorios en EditProfile, y validación de que la nueva contraseña sea distinta a la actual en settings. También añadió `.trim()` en validaciones de contraseñas. El código ya contaba con bastantes validaciones previas, pero quedan campos y formularios sin cubrir completamente.
- ¿En caso de que no, por qué? La cobertura total de validaciones en todos los formularios de la aplicación es una tarea extensa que no se pudo completar en un único sprint.
- 🔗 Issue: https://github.com/ManuelAB09/ISPP/issues/257

---

### No se valida el número de teléfono ni el sitio web al suscribirse a plan de institución
- ¿Lo solucionaste? **No resuelto en este sprint.**
- ¿En caso de que no, por qué? No se asignó a ningún miembro del equipo para este sprint.
- 🔗 Issue: *(sin enlace)*

---
