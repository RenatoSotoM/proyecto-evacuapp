import { useState, useEffect } from 'react';
import * as Location from 'expo-location';
import { LocationObject } from 'expo-location';

export const useLocation = () => {
  const [location, setLocation] = useState<LocationObject | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const getLocation = async () => {
    setIsLoading(true);
    try {
      // Solicitar permisos
      const { status } = await Location.requestForegroundPermissionsAsync();
      
      if (status !== 'granted') {
        setError('Permiso de ubicación denegado');
        setIsLoading(false);
        return;
      }

      // Obtener ubicación actual
      const currentLocation = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.Balanced,
      });
      
      setLocation(currentLocation);
      setError(null);
    } catch (err) {
      setError('Error al obtener la ubicación');
      console.error('Error obteniendo ubicación:', err);
    } finally {
      setIsLoading(false);
    }
  };

  // Obtener ubicación al montar el componente
  useEffect(() => {
    getLocation();
  }, []);

  return { 
    location, 
    error, 
    isLoading, 
    getLocation 
  };
};