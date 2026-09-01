import { api } from './api';

// Cambiar a false cuando el backend esté listo para producción
const USE_MOCK = true; 

export const safeZoneService = {
  async getNearbyZones(lat: number, lng: number, radius: number) {
    if (USE_MOCK) {
      // Datos simulados que imitan el contrato exacto de la API de PostGIS
      return [
        {
          id: 'mock-zone-1',
          name: 'Plaza de Armas (San Bernardo)',
          latitude: -33.5951,
          longitude: -70.7022,
          description: 'Zona segura de evacuación simulada'
        }
      ];
    }
    const response = await api.get(`/safe-zones/nearby?lat=${lat}&lng=${lng}&radius=${radius}`);
    return response.data;
  }
};