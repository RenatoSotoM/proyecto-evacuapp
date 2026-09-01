export const APP_CONSTANTS = {
  // Configuración de mapa
  MAP: {
    DEFAULT_LATITUDE: -33.5951,
    DEFAULT_LONGITUDE: -70.7022,
    DEFAULT_DELTA: 0.03,
    ZOOM_IN: 0.01,
    ZOOM_OUT: 0.05,
  },
  
  // Configuración de ubicación
  LOCATION: {
    UPDATE_INTERVAL: 5000, // 5 segundos
    DISTANCE_FILTER: 10, // 10 metros
  },
  
  // Configuración de sincronización
  SYNC: {
    INTERVAL: 30000, // 30 segundos
    MAX_RETRIES: 3,
  },
  
  // Configuración de emergencia
  EMERGENCY: {
    CHECK_INTERVAL: 10000, // 10 segundos
  },
  
  // Límites
  LIMITS: {
    MAX_REPORT_DESCRIPTION: 500,
    MAX_REPORT_IMAGES: 5,
    MAX_SAFE_ZONES_DISPLAY: 50,
  },
  
  // Estados de incidente
  INCIDENT_STATUS: {
    PENDING: 'PENDING',
    VERIFIED: 'VERIFIED',
    REJECTED: 'REJECTED',
    EXPIRED: 'EXPIRED',
  },
  
  // Colores
  COLORS: {
    PRIMARY: '#1A237E',
    SECONDARY: '#F5F5F5',
    SUCCESS: '#4CAF50',
    WARNING: '#FFC107',
    DANGER: '#D32F2F',
    INFO: '#2196F3',
    DARK: '#333',
    LIGHT: '#FAFAFA',
    WHITE: '#FFFFFF',
    GRAY: '#999',
  },
};