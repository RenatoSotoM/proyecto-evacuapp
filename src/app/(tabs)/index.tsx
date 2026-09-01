import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  ScrollView,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useEmergency } from '../../context/EmergencyContext';
import { useLocation } from '../../hooks/useLocation';
import { useAuth } from '../../context/AuthContext';

export default function MapScreen() {
  const router = useRouter();
  const { user } = useAuth();
  const { emergencyMode, currentEmergency, checkEmergencyStatus } = useEmergency();
  const { location, isLoading: locationLoading, getLocation } = useLocation();
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    checkEmergencyStatus();
    getLocation();
    setTimeout(() => setIsLoading(false), 1000);
  }, []);

  if (locationLoading || isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#1A237E" />
        <Text style={styles.loadingText}>Cargando mapa...</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container}>
      {/* Banner de emergencia */}
      {emergencyMode && currentEmergency && (
        <View style={styles.emergencyBanner}>
          <Text style={styles.emergencyText}>🚨 EMERGENCIA ACTIVA</Text>
          <Text style={styles.emergencyTitle}>{currentEmergency.title}</Text>
          <Text style={styles.emergencyDescription}>{currentEmergency.description}</Text>
        </View>
      )}

      {/* Información de ubicación */}
      <View style={styles.infoCard}>
        <Text style={styles.cardTitle}>📍 Tu Ubicación</Text>
        {location ? (
          <>
            <Text style={styles.coordText}>
              Latitud: {location.coords.latitude.toFixed(6)}
            </Text>
            <Text style={styles.coordText}>
              Longitud: {location.coords.longitude.toFixed(6)}
            </Text>
            <Text style={styles.coordText}>
              Precisión: {location.coords.accuracy?.toFixed(0) || 'N/A'} metros
            </Text>
          </>
        ) : (
          <Text style={styles.noLocationText}>Obteniendo ubicación...</Text>
        )}
        <TouchableOpacity style={styles.refreshButton} onPress={getLocation}>
          <Text style={styles.refreshButtonText}>🔄 Actualizar</Text>
        </TouchableOpacity>
      </View>

      {/* Información del usuario */}
      <View style={styles.infoCard}>
        <Text style={styles.cardTitle}>👤 Usuario</Text>
        <Text style={styles.infoText}>Nombre: {user?.name || 'No logueado'}</Text>
        <Text style={styles.infoText}>Email: {user?.email || 'N/A'}</Text>
      </View>

      {/* Acciones rápidas */}
      <View style={styles.actionsContainer}>
        <Text style={styles.sectionTitle}>Acciones Rápidas</Text>
        
        <TouchableOpacity
          style={styles.actionButton}
          onPress={() => router.push('/report-form')}
        >
          <Text style={styles.actionIcon}>⚠️</Text>
          <Text style={styles.actionText}>Reportar Incidente</Text>
          <Text style={styles.actionArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.actionButton}
          onPress={() => router.push('/meeting-points')}
        >
          <Text style={styles.actionIcon}>📍</Text>
          <Text style={styles.actionText}>Puntos de Encuentro</Text>
          <Text style={styles.actionArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.actionButton}
          onPress={() => router.push('/alerts-tab')}
        >
          <Text style={styles.actionIcon}>🔔</Text>
          <Text style={styles.actionText}>Ver Alertas</Text>
          <Text style={styles.actionArrow}>›</Text>
        </TouchableOpacity>
      </View>

      {/* Estado del sistema */}
      <View style={styles.infoCard}>
        <Text style={styles.cardTitle}>📊 Estado del Sistema</Text>
        <Text style={styles.infoText}>
          Modo Emergencia: {emergencyMode ? '🟢 Activado' : '⚪ Inactivo'}
        </Text>
        <Text style={styles.infoText}>
          Ubicación: {location ? '✅ Disponible' : '⏳ Obteniendo...'}
        </Text>
        <Text style={styles.versionText}>EvacuApp v1.0.0</Text>
      </View>
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
    backgroundColor: '#F5F5F5',
  },
  loadingText: {
    marginTop: 12,
    color: '#666',
    fontSize: 16,
  },
  emergencyBanner: {
    backgroundColor: '#D32F2F',
    padding: 16,
    margin: 16,
    borderRadius: 12,
  },
  emergencyText: {
    color: '#FFFFFF',
    fontWeight: 'bold',
    fontSize: 18,
  },
  emergencyTitle: {
    color: '#FFEBEE',
    fontSize: 16,
    fontWeight: '600',
    marginTop: 4,
  },
  emergencyDescription: {
    color: '#FFEBEE',
    fontSize: 14,
    marginTop: 4,
  },
  infoCard: {
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
  cardTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1A237E',
    marginBottom: 12,
  },
  coordText: {
    fontSize: 14,
    color: '#333',
    marginVertical: 2,
    fontFamily: 'monospace',
  },
  noLocationText: {
    fontSize: 14,
    color: '#999',
    fontStyle: 'italic',
  },
  infoText: {
    fontSize: 14,
    color: '#333',
    marginVertical: 4,
  },
  refreshButton: {
    backgroundColor: '#1A237E',
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 8,
    marginTop: 12,
    alignSelf: 'flex-start',
  },
  refreshButtonText: {
    color: '#FFFFFF',
    fontWeight: '600',
    fontSize: 14,
  },
  actionsContainer: {
    marginHorizontal: 16,
    marginVertical: 8,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 12,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    padding: 16,
    borderRadius: 12,
    marginBottom: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
  },
  actionIcon: {
    fontSize: 24,
    marginRight: 12,
  },
  actionText: {
    fontSize: 16,
    fontWeight: '500',
    color: '#333',
    flex: 1,
  },
  actionArrow: {
    fontSize: 20,
    color: '#CCC',
  },
  versionText: {
    fontSize: 12,
    color: '#999',
    textAlign: 'center',
    marginTop: 8,
  },
});