import axios from 'axios';
import * as SecureStore from 'expo-secure-store';

// Importante: Si pruebas en dispositivo físico o emulador, usa tu IP local de red o 10.0.2.2 para Android
export const API_BASE_URL = 'http://10.0.2.2:3000/api/v1';

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// Interceptor para inyectar el token en cada petición protegida
api.interceptors.request.use(async (config) => {
  const token = await SecureStore.getItemAsync('userToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});