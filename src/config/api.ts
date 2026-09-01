import Constants from 'expo-constants';

export const API_BASE_URL = Constants.expoConfig?.extra?.API_BASE_URL || 'http://localhost:3000/api/v1';

export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
    REFRESH: '/auth/refresh',
  },
  USERS: {
    ME: '/users/me',
    MOBILITY_PROFILE: '/users/me/mobility-profile',
  },
  EMERGENCIES: {
    ACTIVE: '/emergencies/active',
    LIST: '/emergencies',
    CREATE: '/emergencies',
  },
  SAFE_ZONES: {
    LIST: '/safe-zones',
    NEARBY: '/safe-zones/nearby',
  },
  INCIDENTS: {
    CREATE: '/incidents',
    LIST: '/incidents',
  },
  POINTS_OF_INTEREST: {
    LIST: '/points-of-interest',
  },
};