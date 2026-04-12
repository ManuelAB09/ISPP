import React, { useState, useRef, useEffect } from 'react';
import { LuX, LuSearch } from 'react-icons/lu';
import { usersApi } from '../../api/users.api';
import './StudentSelector.css';

const StudentSelector = ({ selectedStudents, onStudentsChange }) => {
  const [searchInput, setSearchInput] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showResults, setShowResults] = useState(false);
  const searchTimeoutRef = useRef(null);

  // Realiza la búsqueda con debounce
  useEffect(() => {
    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }

    if (searchInput.trim().length === 0) {
      setSearchResults([]);
      setShowResults(false);
      return;
    }

    setIsSearching(true);
    setShowResults(true);

    searchTimeoutRef.current = setTimeout(async () => {
      try {
        const results = await usersApi.searchUsers(searchInput.trim());

        const filteredResults = results.filter((user) => {
          
          if (selectedStudents.some((s) => s.id === user.id)) {
            return false;
          }
          
          if (user.rol === 'ADMIN' || user.rol === 'ADMINISTRADOR') {
            return false;
          }
          
          if (user.email && user.email.toLowerCase().includes('admin')) {
            return false;
          }
          return true;
        });
        setSearchResults(filteredResults);
      } catch (error) {
        console.error('Error searching users:', error);
        setSearchResults([]);
      } finally {
        setIsSearching(false);
      }
    }, 300);

    return () => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
    };
  }, [searchInput, selectedStudents]);

  const handleSelectStudent = (student) => {
    onStudentsChange([...selectedStudents, student]);
    setSearchInput('');
    setSearchResults([]);
    setShowResults(false);
  };

  const handleRemoveStudent = (studentId) => {
    onStudentsChange(selectedStudents.filter((s) => s.id !== studentId));
  };

  const handleClickOutside = () => {
    setShowResults(false);
  };

  return (
    <div className="student-selector-container">
      <div className="search-input-wrapper">
        <LuSearch className="search-icon" size={20} />
        <input
          type="text"
          className="search-input"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          onFocus={() => searchInput.trim() !== '' && setShowResults(true)}
          onBlur={() => setTimeout(handleClickOutside, 100)}
          placeholder="Busca alumnos por nombre o email..."
        />
        {isSearching && <div className="search-loader" />}
      </div>

      {/* Results Dropdown */}
      {showResults && (searchResults.length > 0 || isSearching) && (
        <div className="search-results-dropdown">
          {isSearching ? (
            <div className="result-item loading">Buscando...</div>
          ) : searchResults.length > 0 ? (
            searchResults.map((user) => (
              <div
                key={user.id}
                className="result-item"
                onClick={() => handleSelectStudent(user)}
              >
                <div className="result-avatar">
                  {user.avatarUrl ? (
                    <img src={user.avatarUrl} alt={user.nombre} />
                  ) : (
                    <div className="avatar-placeholder">
                      {user.nombre.charAt(0).toUpperCase()}
                    </div>
                  )}
                </div>
                <div className="result-info">
                  <div className="result-name">{user.nombre}</div>
                  <div className="result-email">{user.email}</div>
                </div>
              </div>
            ))
          ) : (
            <div className="result-item no-results">Sin resultados</div>
          )}
        </div>
      )}

      {/* Selected Students */}
      {selectedStudents.length > 0 && (
        <div className="selected-students-list">
          {selectedStudents.map((student) => (
            <div key={student.id} className="student-chip">
              <div className="chip-avatar">
                {student.avatarUrl ? (
                  <img src={student.avatarUrl} alt={student.nombre} />
                ) : (
                  <div className="avatar-placeholder">
                    {student.nombre.charAt(0).toUpperCase()}
                  </div>
                )}
              </div>
              <div className="chip-info">
                <div className="chip-name">{student.nombre}</div>
                <div className="chip-email">{student.email}</div>
              </div>
              <button
                type="button"
                className="btn-remove-student"
                onClick={() => handleRemoveStudent(student.id)}
              >
                <LuX size={18} />
              </button>
            </div>
          ))}
        </div>
      )}

      {selectedStudents.length === 0 && !showResults && (
        <div className="empty-state">
          <p>Ningún alumno seleccionado</p>
        </div>
      )}
    </div>
  );
};

export default StudentSelector;
