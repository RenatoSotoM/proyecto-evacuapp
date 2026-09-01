import { apiClient } from '../client';
import { SafeZone } from '../../types';

export const safeZonesApi = {
  list: () => 
    apiClient.getAxiosInstance().get<SafeZone[]>('/safe-zones'),
  
  getNearby: (lat: number, lng: number, radius: number = 5000) => 
    apiClient.getAxiosInstance().get<SafeZone[]>(`/safe-zones/nearby?lat=${lat}&lng=${lng}&radius=${radius}`),
};