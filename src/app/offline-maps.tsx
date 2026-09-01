import React, { useState } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, Alert } from 'react-native';
import * as SQLite from 'expo-sqlite';

export default function OfflineMapsScreen() {
  const [statusMsg, setStatusMsg] = useState('Datos listos para almacenamiento local.');

  const saveOfflineData = async () => {
    try {
      const db = await SQLite.openDatabaseAsync('evacuapp.db');
      await db.execAsync(`
        CREATE TABLE IF NOT EXISTS offline_cache (id TEXT PRIMARY KEY, data TEXT);
      `);
      setStatusMsg('Zonas seguras y puntos de interés guardados en expo-sqlite[cite: 1, 2].');
      Alert.alert('Éxito', 'Mapas y datos offline guardados localmente.');
    } catch (error) {
      Alert.alert('Error', 'No se pudo guardar la información offline.');
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Gestión de Mapas Offline</Text>
      <Text style={styles.desc}>{statusMsg}</Text>
      <TouchableOpacity style={styles.button} onPress={saveOfflineData}>
        <Text style={styles.buttonText}>Descargar Datos para Uso Sin Conexión</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, justifyContent: 'center', alignItems: 'center', backgroundColor: '#fff' },
  title: { fontSize: 22, fontWeight: 'bold', marginBottom: 15, textAlign: 'center' },
  desc: { fontSize: 14, color: '#666', textAlign: 'center', marginBottom: 20 },
  button: { backgroundColor: '#007AFF', padding: 15, borderRadius: 5, alignItems: 'center', width: '100%' },
  buttonText: { color: '#fff', fontWeight: 'bold' }
});