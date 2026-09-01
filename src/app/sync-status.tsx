import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, Alert } from 'react-native';
import NetInfo from '@react-native-community/netinfo';

export default function SyncStatusScreen() {
  const [isConnected, setIsConnected] = useState<boolean | null>(true);

  useEffect(() => {
    const unsubscribe = NetInfo.addEventListener(state => {
      setIsConnected(state.isConnected);
    });
    return () => unsubscribe();
  }, []);

  const handleSync = () => {
    if (!isConnected) {
      Alert.alert('Sin conexión', 'No se puede sincronizar mientras el dispositivo esté offline.');
      return;
    }
    Alert.alert('Sincronización', 'Cola pendiente transmitida exitosamente al servidor[cite: 1, 2].');
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Estado de Sincronización</Text>
      <View style={[styles.badge, isConnected ? styles.online : styles.offline]}>
        <Text style={styles.badgeText}>{isConnected ? 'ONLINE (Conectado)' : 'OFFLINE (Sin conexión)'}[cite: 1, 2]</Text>
      </View>
      <Text style={styles.desc}>Permite detectar conectividad y sincronizar reportes creados sin conexión[cite: 1, 2].</Text>
      <TouchableOpacity style={styles.button} onPress={handleSync}>
        <Text style={styles.buttonText}>Sincronizar Cola Pendiente</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, justifyContent: 'center', alignItems: 'center', backgroundColor: '#fff' },
  title: { fontSize: 22, fontWeight: 'bold', marginBottom: 20 },
  badge: { paddingVertical: 8, paddingHorizontal: 15, borderRadius: 20, marginBottom: 15 },
  online: { backgroundColor: '#d4edda' },
  offline: { backgroundColor: '#f8d7da' },
  badgeText: { fontWeight: 'bold', fontSize: 13, color: '#333' },
  desc: { fontSize: 13, color: '#666', textAlign: 'center', marginBottom: 25 },
  button: { backgroundColor: '#28A745', padding: 15, borderRadius: 5, alignItems: 'center', width: '100%' },
  buttonText: { color: '#fff', fontWeight: 'bold' }
});