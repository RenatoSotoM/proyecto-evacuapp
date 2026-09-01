import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useLocation } from '../hooks/useLocation';
import { safeZonesApi } from '../api/endpoints/safe-zones';
import { SafeZone } from '../types';

export default function MeetingPointsScreen() {
  const router = useRouter();
  const { location } = useLocation();
  const [zones, setZones] = useState<SafeZone[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (location) {
      loadZones();
    }
  }, [location]);

  const loadZones = async () => {
    if (!location) return;
    
    setIsLoading(true);
    try {
      const response = await safeZonesApi.getNearby(
        location.coords.latitude,
        location.coords.longitude,
        10000 // 10km
      );
      setZones(response.data);
    } catch (error) {
      console.error('Error cargando zonas:', error);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#1A237E" />
      </View>
    );
  }

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>📍 Puntos de Encuentro</Text>
        <Text style={styles.subtitle}>
          Zonas seguras y puntos de reunión cercanos
        </Text>
      </View>

      {zones.length === 0 ? (
        <View style={styles.emptyState}>
          <Text style={styles.emptyIcon}>📍</Text>
          <Text style={styles.emptyText}>No hay puntos de encuentro cercanos</Text>
        </View>
      ) : (
        zones.map((zone) => (
          <TouchableOpacity
            key={zone.id}
            style={styles.zoneCard}
            onPress={() => {
              // Navegar al detalle o abrir en mapa
              router.push({
                pathname: '/(tabs)',
                params: { 
                  lat: zone.location.coordinates[1],
                  lng: zone.location.coordinates[0],
                  name: zone.name,
                }
              });
            }}
          >
            <View style={styles.zoneIcon}>
              <Text style={styles.zoneIconText}>🏠</Text>
            </View>
            <View style={styles.zoneInfo}>
              <Text style={styles.zoneName}>{zone.name}</Text>
              {zone.description && (
                <Text style={styles.zoneDescription}>{zone.description}</Text>
              )}
              {zone.capacity && (
                <Text style={styles.zoneCapacity}>Capacidad: {zone.capacity} personas</Text>
              )}
              {zone.services && zone.services.length > 0 && (
                <View style={styles.servicesContainer}>
                  {zone.services.map((service, index) => (
                    <View key={index} style={styles.serviceTag}>
                      <Text style={styles.serviceTagText}>{service}</Text>
                    </View>
                  ))}
                </View>
              )}
            </View>
            <View style={styles.zoneDistance}>
              <Text style={styles.distanceText}>
                {zone.distance ? `${(zone.distance / 1000).toFixed(1)}km` : '📌'}
              </Text>
            </View>
          </TouchableOpacity>
        ))
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  header: {
    backgroundColor: '#1A237E',
    padding: 20,
    paddingTop: 60,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  subtitle: {
    fontSize: 14,
    color: '#B0BEC5',
    marginTop: 4,
  },
  emptyState: {
    padding: 40,
    alignItems: 'center',
  },
  emptyIcon: {
    fontSize: 48,
    marginBottom: 12,
  },
  emptyText: {
    fontSize: 16,
    color: '#666',
  },
  zoneCard: {
    flexDirection: 'row',
    backgroundColor: '#FFFFFF',
    marginHorizontal: 16,
    marginVertical: 8,
    padding: 16,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
  },
  zoneIcon: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: '#E8F5E9',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  zoneIconText: {
    fontSize: 24,
  },
  zoneInfo: {
    flex: 1,
  },
  zoneName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
  },
  zoneDescription: {
    fontSize: 14,
    color: '#666',
    marginTop: 2,
  },
  zoneCapacity: {
    fontSize: 13,
    color: '#4CAF50',
    marginTop: 4,
  },
  servicesContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginTop: 6,
  },
  serviceTag: {
    backgroundColor: '#E3F2FD',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 10,
    marginRight: 6,
    marginBottom: 4,
  },
  serviceTagText: {
    fontSize: 11,
    color: '#1A237E',
  },
  zoneDistance: {
    justifyContent: 'center',
    paddingLeft: 12,
  },
  distanceText: {
    fontSize: 14,
    fontWeight: '500',
    color: '#1A237E',
  },
});