import { apiClient } from '../client';
import { User, MobilityProfile } from '../../types';

export const usersApi = {
  getProfile: () => 
    apiClient.getAxiosInstance().get<User>('/users/me'),
  
  updateProfile: (data: Partial<User>) => 
    apiClient.getAxiosInstance().put<User>('/users/me', data),
  
  getMobilityProfile: () => 
    apiClient.getAxiosInstance().get<MobilityProfile>('/users/me/mobility-profile'),
  
  updateMobilityProfile: (data: Partial<MobilityProfile>) => 
    apiClient.getAxiosInstance().put<MobilityProfile>('/users/me/mobility-profile', data),
};