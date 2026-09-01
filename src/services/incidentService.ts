export const incidentService = {
  async sendReport(reportData: { type: string; severity: string; latitude: number; longitude: number; description: string }) {
    if (USE_MOCK) {
      console.log('[MOCK] Reporte enviado al modelo de confianza:', reportData);
      return { 
        id: 'mock-incident-01', 
        status: 'PENDING', 
        message: 'Reporte recibido y pendiente de validación comunitaria.' 
      };
    }
    const response = await api.post('/incidents', reportData);
    return response.data;
  }
};