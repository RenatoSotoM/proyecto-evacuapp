import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View, ActivityIndicator } from 'react-native';
import { api } from '../services/api';

export default function AlertsTabScreen() {
  const [emergency, setEmergency] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkActiveEmergency();
  }, []);

  const checkActiveEmergency = async () => {
    try {
      const response = await api.get('/emergencies/active');
      setEmergency(response.data);
    } catch (error) {
      console.error('Error al consultar emergencia activa', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#FF3B30" />
      </View>
    );
  }

  const isActive = emergency?.status === 'ACTIVE';

  return (
    <View style={[styles.container, isActive ? styles.alertActive : styles.alertNormal]}>
      <Text style={styles.title}>Estado de Alerta Global</Text>
      {isActive ? (
        <View style={styles.cardActive}>
          <Text style={styles.alertTitle}>¡EMERGENCIA ACTIVA!</Text>
          <Text style={styles.text}>Descripción: {emergency.description || 'Sin descripción'}</Text>
          <Text style={styles.text}>Estado: {emergency.status}</Text>
        </View>
      ) : (
        <View style={styles.cardNormal}>
          <Text style={styles.normalTitle}>Sin emergencias activas</Text>
          <Text style={styles.text}>El sistema opera con normalidad en este momento.</Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, justifyContent: 'center', alignItems: 'center' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  alertNormal: { backgroundColor: '#e8f4fd' },
  alertActive: { backgroundColor: '#f8d7da' },
  title: { fontSize: 22, fontWeight: 'bold', marginBottom: 20, color: '#333' },
  cardNormal: { backgroundColor: '#fff', padding: 20, borderRadius: 10, width: '100%', alignItems: 'center', borderWidth: 1, borderColor: '#bee5eb' },
  cardActive: { backgroundColor: '#fff', padding: 20, borderRadius: 10, width: '100%', alignItems: 'center', borderWidth: 1, borderColor: '#f5c6cb' },
  normalTitle: { fontSize: 18, fontWeight: 'bold', color: '#0c5460', marginBottom: 10 },
  alertTitle: { fontSize: 20, fontWeight: 'bold', color: '#721c24', marginBottom: 10 },
  text: { fontSize: 14, color: '#333', textAlign: 'center', marginBottom: 5 }
});