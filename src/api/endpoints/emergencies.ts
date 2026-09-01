import { apiClient } from '../client';
import { Emergency, CreateEmergencyRequest } from '../../types';

export const emergenciesApi = {
  getActive: () => 
    apiClient.getAxiosInstance().get<Emergency>('/emergencies/active'),
  
  list: () => 
    apiClient.getAxiosInstance().get<Emergency[]>('/emergencies'),
  
  create: (data: CreateEmergencyRequest) => 
    apiClient.getAxiosInstance().post<Emergency>('/emergencies', data),
};