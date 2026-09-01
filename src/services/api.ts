import axios from 'axios';
import * as SecureStore from 'expo-secure-store';
import { ENV } from '../config/config';

export const api = axios.create({
  baseURL: ENV.API_BASE_URL,
  timeout: 5000, // Timeout corto para que no se quede pegado buscando el backend
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor de solicitudes (Token)
api.interceptors.request.use(
  async (config) => {
    try {
      const token = await SecureStore.getItemAsync('user_token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    } catch (error) {
      console.error('Error al recuperar el token', error);
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Interceptor de respuestas para simular datos si el backend no responde
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url || '';
    console.warn(`[MOCK MODE] Backend no disponible para: ${url}. Usando datos simulados.`);

    // Datos simulados según la ruta consultada
    if (url.includes('/incidents')) {
      return Promise.resolve({
        data: [
          { id: '1', type: 'BLOQUEO_VIAL', severity: 'HIGH', status: 'PENDING', description: 'Escombros en avenida principal', createdAt: new Date().toISOString() },
          { id: '2', type: 'INUNDACION', severity: 'MEDIUM', status: 'VERIFIED', description: 'Acumulación de agua en esquina', createdAt: new Date().toISOString() }
        ]
      });
    }

    if (url.includes('/emergencies/active')) {
      return Promise.resolve({
        data: { status: 'ACTIVE', description: 'Simulacro o alerta de prueba general en el sistema.' }
      });
    }

    if (url.includes('/safe-zones/nearby')) {
      return Promise.resolve({
        data: [
          { id: '1', name: 'Plaza de Armas (Zona Segura)', capacity: 500, latitude: -33.4489, longitude: -70.6693 },
          { id: '2', name: 'Parque Deportivo Municipal', capacity: 1200, latitude: -33.4500, longitude: -70.6700 }
        ]
      });
    }

    if (url.includes('/points-of-interest')) {
      return Promise.resolve({
        data: [
          { id: '1', name: 'Hospital de Emergencia Base', type: 'HOSPITAL' },
          { id: '2', name: 'Comisaría Central', type: 'POLICIA' }
        ]
      });
    }

    // Respuesta genérica para cualquier otro endpoint
    return Promise.resolve({ data: { success: true, message: 'Mock response success' } });
  }
);