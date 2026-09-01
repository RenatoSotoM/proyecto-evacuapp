import { database } from './database';
import { apiClient } from '../../api/client';

export const syncQueue = {
  // Agregar incidente a la cola
  addIncident: async (incident: any) => {
    try {
      // Guardar en pending_incidents
      await database.savePendingIncident(incident);
      
      // Agregar a cola de sincronización
      await database.addToSyncQueue('incident', incident.id || Date.now().toString(), 'CREATE', incident);
      
      console.log('✅ Incidente agregado a cola de sincronización');
    } catch (error) {
      console.error('❌ Error agregando incidente a cola:', error);
    }
  },
  
  // Obtener número de items pendientes
  getPendingCount: async () => {
    return await database.getPendingCount();
  },
  
  // Procesar cola de sincronización
  processSyncQueue: async () => {
    try {
      const items = await database.getSyncQueue();
      
      if (items.length === 0) {
        console.log('📭 No hay items pendientes de sincronizar');
        return;
      }
      
      console.log(`🔄 Procesando ${items.length} items pendientes...`);
      
      for (const item of items) {
        try {
          const data = JSON.parse(item.data);
          
          // Intentar sincronizar con el backend
          switch (item.entity_type) {
            case 'incident':
              await apiClient.getAxiosInstance().post('/incidents', data);
              break;
            // Agregar más casos según necesites
          }
          
          // Marcar como sincronizado
          await database.markSynced(item.id);
          await database.deletePendingIncident(item.entity_id);
          
          console.log(`✅ Item ${item.id} sincronizado correctamente`);
          
        } catch (error: any) {
          console.error(`❌ Error sincronizando item ${item.id}:`, error.message);
          
          // Incrementar retry_count
          await database.runAsync(
            'UPDATE sync_queue SET retry_count = retry_count + 1 WHERE id = ?',
            [item.id]
          );
        }
      }
      
      console.log('✅ Procesamiento de cola completado');
      
    } catch (error) {
      console.error('❌ Error procesando cola de sincronización:', error);
    }
  },
  
  // Programar sincronización automática
  startAutoSync: (intervalMs: number = 30000) => {
    // Sincronizar cada 30 segundos
    const interval = setInterval(() => {
      syncQueue.processSyncQueue();
    }, intervalMs);
    
    return interval;
  },
  
  // Detener sincronización automática
  stopAutoSync: (interval: NodeJS.Timeout) => {
    clearInterval(interval);
  },
};