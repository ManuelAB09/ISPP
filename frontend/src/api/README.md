# API Endpoints

## Descripción

Esta carpeta contiene los archivos de **endpoints** que se utilizan para realizar llamadas a las rutas del backend de la aplicación. Cada funcionalidad del sistema tiene su propio archivo endpoint dedicado, lo que facilita la organización, mantenimiento y escalabilidad del código.

## Estructura y Funcionamiento

- **Propósito**: Centralizar todas las llamadas HTTP al backend en un solo lugar.
- **Tecnología**: Se utiliza **Axios** como librería para realizar las peticiones HTTP (GET, POST, PUT, DELETE, etc.).
- **Organización**: Cada funcionalidad o módulo del sistema tendrá su propio archivo endpoint. Por ejemplo:
  - `userEndpoints.js` - Para gestión de usuarios
  - `communityEndpoints.js` - Para gestión de comunidades
  - `eventEndpoints.js` - Para gestión de eventos
  - etc.

## Ventajas de esta Estructura

- ✅ **Separación de responsabilidades**: La lógica de comunicación con el backend está aislada.
- ✅ **Reutilización**: Los endpoints pueden ser importados y utilizados en cualquier componente.
- ✅ **Mantenibilidad**: Facilita la actualización de URLs o la modificación de la lógica de peticiones.
- ✅ **Testabilidad**: Permite realizar tests unitarios de las funciones de endpoints de forma independiente.

## Ejemplo de Uso

A continuación se muestra un ejemplo de cómo crear un archivo endpoint con Axios:

### Archivo: `userEndpoints.js`

```javascript
import axios from 'axios';

// URL base del backend (puede configurarse en variables de entorno)
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

/**
 * Obtiene todos los usuarios
 * @returns {Promise} - Lista de usuarios
 */
export const getAllUsers = async () => {
  try {
    const response = await axiosInstance.get('/users');
    return response.data;
  } catch (error) {
    console.error('Error al obtener usuarios:', error);
    throw error;
  }
};

/**
 * Obtiene un usuario por ID
 * @param {number} userId - ID del usuario
 * @returns {Promise} - Datos del usuario
 */
export const getUserById = async (userId) => {
  try {
    const response = await axiosInstance.get(`/users/${userId}`);
    return response.data;
  } catch (error) {
    console.error(`Error al obtener usuario ${userId}:`, error);
    throw error;
  }
};

/**
 * Crea un nuevo usuario
 * @param {Object} userData - Datos del usuario a crear
 * @returns {Promise} - Usuario creado
 */
export const createUser = async (userData) => {
  try {
    const response = await axiosInstance.post('/users', userData);
    return response.data;
  } catch (error) {
    console.error('Error al crear usuario:', error);
    throw error;
  }
};

/**
 * Actualiza un usuario existente
 * @param {number} userId - ID del usuario
 * @param {Object} userData - Datos actualizados del usuario
 * @returns {Promise} - Usuario actualizado
 */
export const updateUser = async (userId, userData) => {
  try {
    const response = await axiosInstance.put(`/users/${userId}`, userData);
    return response.data;
  } catch (error) {
    console.error(`Error al actualizar usuario ${userId}:`, error);
    throw error;
  }
};

/**
 * Elimina un usuario
 * @param {number} userId - ID del usuario
 * @returns {Promise} - Confirmación de eliminación
 */
export const deleteUser = async (userId) => {
  try {
    const response = await axiosInstance.delete(`/users/${userId}`);
    return response.data;
  } catch (error) {
    console.error(`Error al eliminar usuario ${userId}:`, error);
    throw error;
  }
};
```

### Uso en un Componente React

```javascript
import React, { useEffect, useState } from 'react';
import { getAllUsers, createUser } from '../api/userEndpoints';

const UserList = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchUsers = async () => {
      try {
        const data = await getAllUsers();
        setUsers(data);
      } catch (error) {
        console.error('Error al cargar usuarios:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchUsers();
  }, []);

  const handleCreateUser = async () => {
    const newUser = {
      name: 'Nuevo Usuario',
      email: 'nuevo@ejemplo.com',
    };

    try {
      const createdUser = await createUser(newUser);
      setUsers([...users, createdUser]);
    } catch (error) {
      console.error('Error al crear usuario:', error);
    }
  };

  if (loading) return <div>Cargando...</div>;

  return (
    <div>
      <h1>Lista de Usuarios</h1>
      <button onClick={handleCreateUser}>Crear Usuario</button>
      <ul>
        {users.map((user) => (
          <li key={user.id}>{user.name}</li>
        ))}
      </ul>
    </div>
  );
};

export default UserList;
```
