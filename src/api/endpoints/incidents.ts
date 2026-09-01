import { apiClient } from '../client';
import { Incident, CreateIncidentRequest } from '../../types';

export const incidentsApi = {
  create: (data: CreateIncidentRequest) => 
    apiClient.getAxiosInstance().post<Incident>('/incidents', data),
  
  list: (emergencyId?: string) => {
    const url = emergencyId ? `/incidents?emergencyId=${emergencyId}` : '/incidents';
    return apiClient.getAxiosInstance().get<Incident[]>(url);
  },
};