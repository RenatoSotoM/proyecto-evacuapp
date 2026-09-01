import React, { createContext, useContext, useState, useEffect } from 'react';
import { apiClient } from '../api/client';
import { Emergency } from '../types';

interface EmergencyContextType {
  emergencyMode: boolean;
  currentEmergency: Emergency | null;
  isLoading: boolean;
  checkEmergencyStatus: () => Promise<void>;
  activateEmergency: (emergency: Emergency) => void;
  deactivateEmergency: () => void;
}

const EmergencyContext = createContext<EmergencyContextType | undefined>(undefined);

export const EmergencyProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [emergencyMode, setEmergencyMode] = useState(false);
  const [currentEmergency, setCurrentEmergency] = useState<Emergency | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const checkEmergencyStatus = async () => {
    try {
      setIsLoading(true);
      const response = await apiClient.getAxiosInstance().get('/emergencies/active');
      const data = response.data;
      
      if (data && data.status === 'ACTIVE') {
        setEmergencyMode(true);
        setCurrentEmergency(data);
      } else {
        setEmergencyMode(false);
        setCurrentEmergency(null);
      }
    } catch (error) {
      console.error('Error checking emergency status:', error);
      setEmergencyMode(false);
      setCurrentEmergency(null);
    } finally {
      setIsLoading(false);
    }
  };

  const activateEmergency = (emergency: Emergency) => {
    setEmergencyMode(true);
    setCurrentEmergency(emergency);
  };

  const deactivateEmergency = () => {
    setEmergencyMode(false);
    setCurrentEmergency(null);
  };

  useEffect(() => {
    checkEmergencyStatus();
  }, []);

  return (
    <EmergencyContext.Provider
      value={{
        emergencyMode,
        currentEmergency,
        isLoading,
        checkEmergencyStatus,
        activateEmergency,
        deactivateEmergency,
      }}
    >
      {children}
    </EmergencyContext.Provider>
  );
};

export const useEmergency = () => {
  const context = useContext(EmergencyContext);
  if (context === undefined) {
    throw new Error('useEmergency must be used within an EmergencyProvider');
  }
  return context;
};