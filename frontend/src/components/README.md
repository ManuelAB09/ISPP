# Componentes React

## Descripción

Esta carpeta contiene todos los **componentes reutilizables** de la aplicación. Los componentes son piezas de interfaz de usuario que pueden ser utilizadas en múltiples lugares de la aplicación, promoviendo la reutilización de código y manteniendo una estructura organizada y modular.

## Convenciones de Nomenclatura

### ⚠️ Importante

- **Nombre**: Los componentes deben comenzar con **mayúscula** (PascalCase).
- **Extensión**: Los componentes deben tener la extensión **`.jsx`** para diferenciarlos de las screens y otros archivos JavaScript.

### Ejemplos Correctos

✅ `Button.jsx`  
✅ `NavigationBar.jsx`  
✅ `UserCard.jsx`  
✅ `LoadingSpinner.jsx`  

### Ejemplos Incorrectos

❌ `button.jsx` (debe empezar con mayúscula)  
❌ `Button.js` (debe usar extensión .jsx)  
❌ `navigationbar.jsx` (debe usar PascalCase)  

## Organización de Componentes

###  Carpetas por Componente (Componentes Complejos)

```
components/
  ├── Button/
  │   ├── Button.jsx
  │   ├── Button.css
  │   └── Button.test.js
  ├── UserCard/
  │   ├── UserCard.jsx
  │   ├── UserCard.css
  │   └── UserCard.test.js
  └── NavigationBar/
      ├── NavigationBar.jsx
      ├── NavigationBar.css
      └── components/
          ├── NavItem.jsx
          └── NavDropdown.jsx
```


## Ejemplo de Estructura Completa

```
components/
  ├── index.js                    # Exportación centralizada
  ├── Button.jsx                  # Componente simple
  ├── Input.jsx                   # Componente simple
  ├── Badge.jsx                   # Componente simple
  ├── UserCard/                   # Componente complejo
  │   ├── UserCard.jsx
  │   ├── UserCard.css
  │   └── UserCard.test.js
  ├── NavigationBar/              # Componente complejo con sub-componentes
  │   ├── NavigationBar.jsx
  │   ├── NavigationBar.css
  │   └── components/
  │       ├── NavItem.jsx
  │       └── NavDropdown.jsx
  └── README.md                   # Este archivo
```