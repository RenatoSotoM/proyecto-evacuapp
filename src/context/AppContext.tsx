import React, { useEffect } from 'react';
import { AuthProvider } from './AuthContext';
import { EmergencyProvider } from './EmergencyContext';
import { initDatabase } from '../services/storage/database';

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  useEffect(() => {
    // Inicializar base de datos al arrancar la app
    initDatabase();
  }, []);

  return (
    <AuthProvider>
      <EmergencyProvider>
        {children}
      </EmergencyProvider>
    </AuthProvider>
  );
};

// Exportar hooks para fácil acceso
export { useAuth } from './AuthContext';
export { useEmergency } from './EmergencyContext';