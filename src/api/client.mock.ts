// Esto es solo para pruebas SIN backend
// Reemplaza el contenido de client.ts temporalmente

export const apiClient = {
  getAxiosInstance: () => ({
    post: async (url: string, data: any) => {
      console.log('📤 MOCK POST:', url, data);
      // Simular respuestas
      if (url.includes('/auth/login')) {
        return {
          data: {
            accessToken: 'mock-token-123',
            tokenType: 'Bearer',
            user: {
              id: 'mock-user-1',
              name: 'Usuario Prueba',
              email: data.email || 'test@test.com',
              role: 'USER',
            }
          }
        };
      }
      if (url.includes('/auth/register')) {
        return {
          data: {
            accessToken: 'mock-token-123',
            tokenType: 'Bearer',
            user: {
              id: 'mock-user-1',
              name: data.name || 'Usuario',
              email: data.email || 'test@test.com',
              role: 'USER',
            }
          }
        };
      }
      return { data: { success: true } };
    },
    get: async (url: string) => {
      console.log('📤 MOCK GET:', url);
      if (url.includes('/emergencies/active')) {
        return {
          data: {
            id: 'mock-emergency-1',
            type: 'EARTHQUAKE',
            status: 'ACTIVE',
            title: 'Simulación de Sismo',
            description: 'Evacuación preventiva hacia zonas seguras.',
            startedAt: new Date().toISOString(),
          }
        };
      }
      if (url.includes('/safe-zones/nearby')) {
        return {
          data: [
            {
              id: 'zone-1',
              name: 'Plaza de Armas',
              description: 'Zona segura principal',
              location: {
                type: 'Point',
                coordinates: [-70.7022, -33.5951]
              },
              capacity: 500,
              services: ['Agua', 'Primeros Auxilios'],
            }
          ]
        };
      }
      return { data: [] };
    },
    put: async (url: string, data: any) => {
      console.log('📤 MOCK PUT:', url, data);
      return { data: { ...data, updated: true } };
    },
    interceptors: {
      request: { use: () => {}, eject: () => {} },
      response: { use: () => {}, eject: () => {} },
    }
  }),
  setAuthToken: (token: string) => console.log('🔑 Token:', token),
  clearAuthToken: () => console.log('🗑️ Token limpiado'),
};