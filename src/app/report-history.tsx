import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View, FlatList, ActivityIndicator } from 'react-native';
import { api } from '../services/api';

interface IncidentItem {
  id: string;
  type: string;
  severity: string;
  status: 'PENDING' | 'VERIFIED' | 'REJECTED' | 'EXPIRED';
  description?: string;
  createdAt: string;
}

export default function ReportHistoryScreen() {
  const [incidents, setIncidents] = useState<IncidentItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchIncidents();
  }, []);

  const fetchIncidents = async () => {
    try {
      const response = await api.get('/incidents');
      setIncidents(response.data);
    } catch (error) {
      console.error('Error al cargar historial de incidentes', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#007AFF" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Historial de Reportes</Text>
      <FlatList
        data={incidents}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <View style={styles.card}>
            <View style={styles.row}>
              <Text style={styles.type}>{item.type}</Text>
              <Text style={[styles.status, styles[item.status]]}>{item.status}</Text>
            </View>
            <Text style={styles.severity}>Severidad: {item.severity}</Text>
            {item.description ? <Text style={styles.desc}>{item.description}</Text> : null}
            <Text style={styles.date}>{new Date(item.createdAt).toLocaleString()}</Text>
          </View>
        )}
        ListEmptyComponent={<Text style={styles.empty}>No hay reportes registrados.</Text>}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, backgroundColor: '#f8f9fa' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  title: { fontSize: 22, fontWeight: 'bold', marginBottom: 20, textAlign: 'center' },
  card: { backgroundColor: '#fff', padding: 15, borderRadius: 8, marginBottom: 10, borderWidth: 1, borderColor: '#eee' },
  row: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 5 },
  type: { fontWeight: 'bold', fontSize: 16, color: '#333' },
  severity: { fontSize: 13, color: '#666', marginBottom: 5 },
  desc: { fontSize: 14, color: '#444', marginBottom: 8 },
  date: { fontSize: 11, color: '#999', textAlign: 'right' },
  status: { fontSize: 12, fontWeight: 'bold', paddingHorizontal: 6, paddingVertical: 2, borderRadius: 4 },
  PENDING: { backgroundColor: '#fff3cd', color: '#856404' },
  VERIFIED: { backgroundColor: '#d4edda', color: '#155724' },
  REJECTED: { backgroundColor: '#f8d7da', color: '#721c24' },
  EXPIRED: { backgroundColor: '#e2e3e5', color: '#383d41' },
  empty: { textAlign: 'center', color: '#666', marginTop: 40 }
});