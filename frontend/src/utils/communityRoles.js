export const normalizeCommunityRole = (role) => {
  const value = String(role || '').trim().toUpperCase();

  if (value === 'ADMINISTRADOR') return 'ADMIN';
  if (value === 'ESTUDIANTE' || value === 'ALUMNO') return 'ALUMNO';
  if (value === 'MEMBER' || value === 'MIEMBRO') return 'MIEMBRO';
  if (value === 'TEACHER') return 'PROFESOR';

  return value;
};

export const isAdminRole = (role) => normalizeCommunityRole(role) === 'ADMIN';

export const isTeacherRole = (role) => normalizeCommunityRole(role) === 'PROFESOR';

export const canCreateCommunityEvent = (role) => {
  const normalizedRole = normalizeCommunityRole(role);
  return normalizedRole === 'ADMIN' || normalizedRole === 'PROFESOR';
};

export const getCommunityRoleLabel = (role) => {
  switch (normalizeCommunityRole(role)) {
    case 'ADMIN':
      return 'Administrador';
    case 'PROFESOR':
      return 'Profesor';
    case 'ALUMNO':
      return 'Alumno';
    case 'MIEMBRO':
      return 'Alumno';
    default:
      return 'Invitado';
  }
};

export const getCommunityRoleCapabilities = (role) => {
  switch (normalizeCommunityRole(role)) {
    case 'ADMIN':
      return [
        'Gestionar configuración y miembros de la comunidad',
        'Aprobar solicitudes y moderar contenido',
        'Crear, editar y cancelar eventos',
        'Transferir la administración y eliminar la comunidad',
      ];
    case 'PROFESOR':
      return [
        'Crear y gestionar eventos de su comunidad',
        'Acceder al contenido educativo disponible',
        'Participar en el chat y en la dinámica docente',
        'Consultar la información de la comunidad y sus materiales',
      ];
    case 'ALUMNO':
    case 'MIEMBRO':
      return [
        'Acceder al contenido y materiales publicados',
        'Participar en eventos y en el chat',
        'Consultar la información general de la comunidad',
        'Sin permisos de moderación o gestión',
      ];
    default:
      return [
        'Consultar la información pública de la comunidad',
        'Solicitar acceso o unirse si la comunidad es abierta',
      ];
  }
};