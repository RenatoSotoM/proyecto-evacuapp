import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View, FlatList, ActivityIndicator } from 'react-native';
import { api } from '../services/api';

export default function MeetingPointsScreen() {
  const [pois, setPois] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchPointsOfInterest();
  }, []);

  const fetchPointsOfInterest = async () => {
    try {
      const response = await api.get('/points-of-interest');
      setPois(response.data);
    } catch (error) {
      console.error('Error al cargar puntos de interés', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <ActivityIndicator style={styles.center} size="large" color="#007AFF" />;

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Puntos de Apoyo e Información</Text>
      <FlatList
        data={pois}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <View style={styles.card}>
            <Text style={styles.name}>{item.name}</Text>
            <Text style={styles.type}>Tipo: {item.type}</Text>
            <Text style={styles.info}>Solo informativos, no son destinos de evacuación.</Text>
          </View>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, backgroundColor: '#fff' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  title: { fontSize: 22, fontWeight: 'bold', marginBottom: 20, textAlign: 'center' },
  card: { padding: 15, borderWidth: 1, borderColor: '#ccc', borderRadius: 8, marginBottom: 10 },
  name: { fontSize: 16, fontWeight: 'bold', color: '#333' },
  type: { fontSize: 13, color: '#666', marginVertical: 4 },
  info: { fontSize: 11, color: '#856404', backgroundColor: '#fff3cd', padding: 5, borderRadius: 3 }
});