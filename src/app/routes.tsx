import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, FlatList, TouchableOpacity, ActivityIndicator, Alert } from 'react-native';
import * as Location from 'expo-location';
import { router } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';
import { api } from '../services/api';

export default function RoutesScreen() {
  const [safeZones, setSafeZones] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchNearbyZones();
  }, []);

  const fetchNearbyZones = async () => {
    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        Alert.alert('Permiso denegado', 'Se requiere acceso al GPS para buscar zonas seguras.');
        setLoading(false);
        return;
      }
      const location = await Location.getCurrentPositionAsync({ accuracy: Location.LocationAccuracy.High });
      const { latitude, longitude } = location.coords;

      const response = await api.get(`/safe-zones/nearby?lat=${latitude}&lng=${longitude}&radius=20000`);
      setSafeZones(response.data);
    } catch (error) {
      console.error('Error al obtener zonas seguras:', error);
      Alert.alert('Error', 'No se pudieron cargar las zonas seguras desde el servidor backend.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#003d9b" />
        <Text style={styles.loadingText}>Buscando refugios cercanos...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Selecciona Zona de Evacuación</Text>
      <FlatList
        data={safeZones}
        keyExtractor={(item) => item.id || item.name}
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.card}
            onPress={() => router.push({
              pathname: '/route-detail',
              params: { lat: item.latitude, lng: item.longitude, name: item.name }
            })}
          >
            <MaterialIcons name="shield" size={26} color="#003d9b" />
            <View style={styles.info}>
              <Text style={styles.cardTitle}>{item.name}</Text>
              <Text style={styles.cardDesc}>{item.description || 'Punto de encuentro oficial'}</Text>
            </View>
            <MaterialIcons name="chevron-right" size={24} color="#5d5e61" />
          </TouchableOpacity>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 16, backgroundColor: '#f8f9fb', paddingTop: 60 },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#f8f9fb' },
  loadingText: { marginTop: 10, color: '#434654' },
  title: { fontSize: 20, fontWeight: '700', marginBottom: 16, color: '#191c1e' },
  card: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', padding: 16, borderRadius: 12, marginBottom: 12, elevation: 2 },
  info: { flex: 1, marginLeft: 12 },
  cardTitle: { fontSize: 16, fontWeight: '700', color: '#191c1e' },
  cardDesc: { fontSize: 13, color: '#5d5e61' }
});