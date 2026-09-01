import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  TextInput,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useLocation } from '../../hooks/useLocation';
import { useEmergency } from '../../context/EmergencyContext';
import { Card } from '../../components/common/Card';

export default function RoutesTab() {
  const router = useRouter();
  const { location } = useLocation();
  const { emergencyMode, currentEmergency } = useEmergency();
  const [destination, setDestination] = useState('');
  const [isSearching, setIsSearching] = useState(false);

  const handleSearchRoute = () => {
    if (!destination.trim()) {
      alert('Ingresa un destino');
      return;
    }
    
    setIsSearching(true);
    // Simular búsqueda de ruta
    setTimeout(() => {
      setIsSearching(false);
      router.push('/route-detail');
    }, 1500);
  };

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>🚶 Rutas de Evacuación</Text>
        <Text style={styles.subtitle}>
          Encuentra la ruta más segura hacia tu destino
        </Text>
      </View>

      {emergencyMode && currentEmergency && (
        <View style={styles.emergencyBanner}>
          <Text style={styles.emergencyText}>🚨 EMERGENCIA ACTIVA</Text>
          <Text style={styles.emergencyTitle}>{currentEmergency.title}</Text>
        </View>
      )}

      <Card style={styles.searchCard}>
        <Text style={styles.label}>📍 Ubicación actual</Text>
        {location ? (
          <Text style={styles.locationText}>
            {location.coords.latitude.toFixed(6)}, {location.coords.longitude.toFixed(6)}
          </Text>
        ) : (
          <Text style={styles.locationText}>Obteniendo ubicación...</Text>
        )}

        <Text style={styles.label}>🎯 Destino</Text>
        <TextInput
          style={styles.input}
          placeholder="Ej: Plaza de Armas, Zona Segura..."
          value={destination}
          onChangeText={setDestination}
          editable={!isSearching}
        />

        <TouchableOpacity
          style={[styles.searchButton, isSearching && styles.searchButtonDisabled]}
          onPress={handleSearchRoute}
          disabled={isSearching}
        >
          <Text style={styles.searchButtonText}>
            {isSearching ? 'Buscando ruta...' : '🔍 Buscar Ruta'}
          </Text>
        </TouchableOpacity>
      </Card>

      <View style={styles.quickActions}>
        <Text style={styles.sectionTitle}>Acciones Rápidas</Text>
        
        <TouchableOpacity
          style={styles.actionButton}
          onPress={() => router.push('/meeting-points')}
        >
          <Text style={styles.actionIcon}>📍</Text>
          <View>
            <Text style={styles.actionTitle}>Puntos de Encuentro</Text>
            <Text style={styles.actionSubtext}>Ver zonas seguras cercanas</Text>
          </View>
          <Text style={styles.actionArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.actionButton}
          onPress={() => router.push('/report-form')}
        >
          <Text style={styles.actionIcon}>⚠️</Text>
          <View>
            <Text style={styles.actionTitle}>Reportar Incidente</Text>
            <Text style={styles.actionSubtext}>Bloqueos, escombros, etc.</Text>
          </View>
          <Text style={styles.actionArrow}>›</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.tips}>
        <Text style={styles.tipsTitle}>💡 Consejos de Evacuación</Text>
        <Text style={styles.tipItem}>• Mantén la calma y sigue las indicaciones</Text>
        <Text style={styles.tipItem}>• Evita áreas con peligro visible</Text>
        <Text style={styles.tipItem}>• Ayuda a personas con movilidad reducida</Text>
        <Text style={styles.tipItem}>• Mantente informado por canales oficiales</Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
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
  emergencyBanner: {
    backgroundColor: '#D32F2F',
    margin: 16,
    padding: 16,
    borderRadius: 12,
  },
  emergencyText: {
    color: '#FFFFFF',
    fontWeight: 'bold',
    fontSize: 16,
  },
  emergencyTitle: {
    color: '#FFEBEE',
    fontSize: 14,
    marginTop: 4,
  },
  searchCard: {
    margin: 16,
    padding: 16,
  },
  label: {
    fontSize: 14,
    fontWeight: '500',
    color: '#333',
    marginTop: 12,
    marginBottom: 4,
  },
  locationText: {
    fontSize: 14,
    color: '#666',
    backgroundColor: '#F5F5F5',
    padding: 10,
    borderRadius: 8,
    fontFamily: 'monospace',
  },
  input: {
    borderWidth: 1,
    borderColor: '#DDD',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    backgroundColor: '#FAFAFA',
  },
  searchButton: {
    backgroundColor: '#1A237E',
    borderRadius: 8,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 16,
  },
  searchButtonDisabled: {
    backgroundColor: '#B0BEC5',
  },
  searchButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
  quickActions: {
    paddingHorizontal: 16,
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
  actionTitle: {
    fontSize: 15,
    fontWeight: '500',
    color: '#333',
  },
  actionSubtext: {
    fontSize: 12,
    color: '#666',
  },
  actionArrow: {
    fontSize: 20,
    color: '#CCC',
    marginLeft: 'auto',
  },
  tips: {
    backgroundColor: '#FFFFFF',
    margin: 16,
    padding: 16,
    borderRadius: 12,
    marginBottom: 24,
  },
  tipsTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 12,
  },
  tipItem: {
    fontSize: 14,
    color: '#555',
    marginVertical: 4,
    paddingLeft: 4,
  },
});