import * as SQLite from 'expo-sqlite';

let db: SQLite.SQLiteDatabase;

export const initDatabase = async () => {
  try {
    db = await SQLite.openDatabaseAsync('evacuapp.db');
    
    await db.execAsync(`
      CREATE TABLE IF NOT EXISTS safe_zones (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        description TEXT,
        latitude REAL NOT NULL,
        longitude REAL NOT NULL,
        emergency_id TEXT,
        created_at TEXT NOT NULL
      );
      
      CREATE TABLE IF NOT EXISTS pending_incidents (
        id TEXT PRIMARY KEY,
        type TEXT NOT NULL,
        severity TEXT NOT NULL,
        description TEXT,
        latitude REAL NOT NULL,
        longitude REAL NOT NULL,
        emergency_id TEXT,
        created_at TEXT NOT NULL,
        retry_count INTEGER DEFAULT 0
      );
      
      CREATE TABLE IF NOT EXISTS sync_queue (
        id TEXT PRIMARY KEY,
        entity_type TEXT NOT NULL,
        entity_id TEXT NOT NULL,
        operation TEXT NOT NULL,
        data TEXT NOT NULL,
        status TEXT DEFAULT 'PENDING',
        retry_count INTEGER DEFAULT 0,
        created_at TEXT NOT NULL,
        synced_at TEXT
      );
    `);
    
    console.log('✅ Database initialized successfully');
  } catch (error) {
    console.error('❌ Error initializing database:', error);
  }
};

export const database = {
  saveSafeZones: async (zones: any[]) => {
    try {
      for (const zone of zones) {
        await db.runAsync(
          `INSERT OR REPLACE INTO safe_zones 
           (id, name, description, latitude, longitude, emergency_id, created_at) 
           VALUES (?, ?, ?, ?, ?, ?, ?)`,
          [zone.id, zone.name, zone.description || '', zone.location.coordinates[1], zone.location.coordinates[0], zone.emergencyId || '', new Date().toISOString()]
        );
      }
    } catch (error) {
      console.error('Error saving safe zones:', error);
    }
  },
  
  getSafeZones: async () => {
    try {
      const result = await db.getAllAsync('SELECT * FROM safe_zones ORDER BY name');
      return result;
    } catch (error) {
      console.error('Error getting safe zones:', error);
      return [];
    }
  },
  
  getPendingCount: async () => {
    try {
      const result = await db.getAllAsync('SELECT COUNT(*) as count FROM sync_queue WHERE status = "PENDING"');
      return result[0]?.count || 0;
    } catch (error) {
      console.error('Error getting pending count:', error);
      return 0;
    }
  },
};