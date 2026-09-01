import { api } from '../services/api';

export const checkEmergencyStatus = async () => {
  try {
    const response = await api.get('/emergencies/active');
    return response.data?.status === 'ACTIVE';
  } catch (error) {
    console.error('Error al verificar estado de emergencia', error);
    return false;
  }
};