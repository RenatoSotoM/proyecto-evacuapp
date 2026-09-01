import { api } from './api';

export interface MobilityProfilePayload {
  mobilityType: 'PEATON' | 'VEHICULO' | 'MOVILIDAD_REDUCIDA';
  requiresAccessibleRoute: boolean;
  travelsWithMinors: boolean;
  companionCount: number;
}

export const updateMobilityProfile = async (payload: MobilityProfilePayload) => {
  const response = await api.put('/users/me/mobility-profile', payload);
  return response.data;
};