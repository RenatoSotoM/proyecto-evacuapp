import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
  Switch,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from '../../context/AuthContext';
import { useEmergency } from '../../context/EmergencyContext';
import { usersApi } from '../../api/endpoints/users';
import { MobilityProfile } from '../../types';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Card } from '../../components/common/Card';

export default function ProfileTab() {
  const router = useRouter();
  const { user, logout } = useAuth();
  const { emergencyMode } = useEmergency();
  const [profile, setProfile] = useState<MobilityProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    setIsLoading(true);
    try {
      const response = await usersApi.getMobilityProfile();
      setProfile(response.data);
    } catch (error) {
      Alert.alert('Error', 'No se pudo cargar el perfil');
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = () => {
    Alert.alert(
      'Cerrar Sesión',
      '¿Estás seguro que deseas cerrar sesión?',
      [
        { text: 'Cancelar', style: 'cancel' },
        { 
          text: 'Cerrar Sesión', 
          style: 'destructive',
          onPress: async () => {
            await logout();
            router.replace('/login');
          }
        },
      ]
    );
  };

  const handleUpdateMobility = async (key: keyof MobilityProfile, value: any) => {
    try {
      const updated = { ...profile, [key]: value };
      await usersApi.updateMobilityProfile(updated);
      setProfile(updated);
    } catch (error) {
      Alert.alert('Error', 'No se pudo actualizar el perfil');
    }
  };

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <LoadingSpinner size="large" />
      </View>
    );
  }

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <View style={styles.avatarContainer}>
          <Text style={styles.avatarText}>
            {user?.name?.charAt(0).toUpperCase() || 'U'}
          </Text>
        </View>
        <Text style={styles.userName}>{user?.name}</Text>
        <Text style={styles.userEmail}>{user?.email}</Text>
      </View>

      <Card style={styles.section}>
        <Text style={styles.sectionTitle}>Perfil de Movilidad</Text>
        
        <View style={styles.field}>
          <Text style={styles.fieldLabel}>Tipo de Movilidad</Text>
          <View style={styles.buttonGroup}>
            {(['PEATON', 'VEHICULO', 'MOVILIDAD_REDUCIDA'] as const).map((type) => (
              <TouchableOpacity
                key={type}
                style={[
                  styles.typeButton,
                  profile?.mobilityType === type && styles.typeButtonActive,
                ]}
                onPress={() => handleUpdateMobility('mobilityType', type)}
              >
                <Text style={[
                  styles.typeButtonText,
                  profile?.mobilityType === type && styles.typeButtonTextActive,
                ]}>
                  {type === 'PEATON' ? '🚶 Peatón' :
                   type === 'VEHICULO' ? '🚗 Vehículo' :
                   '♿ Movilidad Reducida'}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <View style={styles.field}>
          <View style={styles.switchRow}>
            <Text style={styles.fieldLabel}>Ruta Accesible</Text>
            <Switch
              value={profile?.requiresAccessibleRoute || false}
              onValueChange={(value) => handleUpdateMobility('requiresAccessibleRoute', value)}
              trackColor={{ false: '#D1D1D6', true: '#1A237E' }}
            />
          </View>
          <Text style={styles.fieldDescription}>
            Priorizar rutas sin escaleras ni pendientes pronunciadas
          </Text>
        </View>

        <View style={styles.field}>
          <View style={styles.switchRow}>
            <Text style={styles.fieldLabel}>Viaja con Menores</Text>
            <Switch
              value={profile?.travelsWithMinors || false}
              onValueChange={(value) => handleUpdateMobility('travelsWithMinors', value)}
              trackColor={{ false: '#D1D1D6', true: '#1A237E' }}
            />
          </View>
        </View>
      </Card>

      <Card style={styles.section}>
        <Text style={styles.sectionTitle}>Preferencias</Text>
        
        <TouchableOpacity
          style={styles.menuItem}
          onPress={() => router.push('/offline-maps')}
        >
          <Text style={styles.menuItemText}>📥 Mapas Offline</Text>
          <Text style={styles.menuItemArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.menuItem}
          onPress={() => router.push('/sync-status')}
        >
          <Text style={styles.menuItemText}>🔄 Sincronización</Text>
          <Text style={styles.menuItemArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.menuItem}
          onPress={() => router.push('/report-history')}
        >
          <Text style={styles.menuItemText}>📋 Historial de Reportes</Text>
          <Text style={styles.menuItemArrow}>›</Text>
        </TouchableOpacity>
      </Card>

      <TouchableOpacity
        style={[styles.logoutButton, emergencyMode && styles.logoutButtonDisabled]}
        onPress={handleLogout}
        disabled={emergencyMode}
      >
        <Text style={styles.logoutButtonText}>
          {emergencyMode ? '⛔ No disponible en emergencia' : '🚪 Cerrar Sesión'}
        </Text>
      </TouchableOpacity>

      <Text style={styles.version}>EvacuApp v1.0.0</Text>
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
    padding: 24,
    alignItems: 'center',
    paddingTop: 60,
  },
  avatarContainer: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#FFFFFF',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 12,
  },
  avatarText: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#1A237E',
  },
  userName: {
    fontSize: 20,
    fontWeight: '600',
    color: '#FFFFFF',
  },
  userEmail: {
    fontSize: 14,
    color: '#B0BEC5',
    marginTop: 2,
  },
  section: {
    margin: 16,
    padding: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1A237E',
    marginBottom: 16,
  },
  field: {
    marginBottom: 16,
  },
  fieldLabel: {
    fontSize: 14,
    fontWeight: '500',
    color: '#333',
    marginBottom: 6,
  },
  fieldDescription: {
    fontSize: 12,
    color: '#666',
    marginTop: 4,
  },
  buttonGroup: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  typeButton: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 8,
    backgroundColor: '#F0F0F0',
    borderWidth: 1,
    borderColor: '#E0E0E0',
    marginRight: 8,
    marginBottom: 8,
  },
  typeButtonActive: {
    backgroundColor: '#1A237E',
    borderColor: '#1A237E',
  },
  typeButtonText: {
    fontSize: 14,
    color: '#333',
  },
  typeButtonTextActive: {
    color: '#FFFFFF',
  },
  switchRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  menuItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#F0F0F0',
  },
  menuItemText: {
    fontSize: 16,
    color: '#333',
  },
  menuItemArrow: {
    fontSize: 18,
    color: '#999',
  },
  logoutButton: {
    margin: 16,
    padding: 16,
    borderRadius: 8,
    backgroundColor: '#D32F2F',
    alignItems: 'center',
  },
  logoutButtonDisabled: {
    backgroundColor: '#B0BEC5',
    opacity: 0.6,
  },
  logoutButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
  version: {
    textAlign: 'center',
    color: '#999',
    fontSize: 12,
    marginBottom: 24,
  },
});