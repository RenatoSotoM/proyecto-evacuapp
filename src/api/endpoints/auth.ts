import { apiClient } from '../client';
import { LoginRequest, RegisterRequest, AuthResponse } from '../../types';

export const authApi = {
  login: (data: LoginRequest) => 
    apiClient.getAxiosInstance().post<AuthResponse>('/auth/login', data),
  
  register: (data: RegisterRequest) => 
    apiClient.getAxiosInstance().post<AuthResponse>('/auth/register', data),
  
  refresh: () => 
    apiClient.getAxiosInstance().post<AuthResponse>('/auth/refresh'),
};