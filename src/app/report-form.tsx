import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from '../context/AuthContext';
import { useEmergency } from '../context/EmergencyContext';
import { incidentsApi } from '../api/endpoints/incidents';
import { IncidentType, IncidentSeverity } from '../types';
import { useLocation } from '../hooks/useLocation';

const INCIDENT_TYPES: { value: IncidentType; label: string; icon: string }[] = [
  { value: 'BLOQUEO_VIAL', label: 'Bloqueo Vial', icon: '🚧' },
  { value: 'ESCOMBROS', label: 'Escombros', icon: '🪨' },
  { value: 'INUNDACION', label: 'Inundación', icon: '🌊' },
  { value: 'INCENDIO', label: 'Incendio', icon: '🔥' },
  { value: 'ACCIDENTE', label: 'Accidente', icon: '🚗' },
  { value: 'RUTA_INACCESIBLE', label: 'Ruta Inaccesible', icon: '🚫' },
  { value: 'PELIGRO_GENERAL', label: 'Peligro General', icon: '⚠️' },
];

const SEVERITY_OPTIONS: { value: IncidentSeverity; label: string; color: string }[] = [
  { value: 'LOW', label: 'Bajo', color: '#4CAF50' },
  { value: 'MEDIUM', label: 'Medio', color: '#FFC107' },
  { value: 'HIGH', label: 'Alto', color: '#FF9800' },
  { value: 'CRITICAL', label: 'Crítico', color: '#D32F2F' },
];

export default function ReportForm() {
  const router = useRouter();
  const { user } = useAuth();
  const { currentEmergency } = useEmergency();
  const { location, getLocation } = useLocation();
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    type: 'BLOQUEO_VIAL' as IncidentType,
    severity: 'MEDIUM' as IncidentSeverity,
    description: '',
  });

  useEffect(() => {
    getLocation();
  }, []);

  const handleSubmit = async () => {
    if (!location) {
      Alert.alert('Error', 'No se pudo obtener tu ubicación');
      return;
    }

    setIsSubmitting(true);
    try {
      await incidentsApi.create({
        type: formData.type,
        severity: formData.severity,
        description: formData.description || undefined,
        latitude: location.coords.latitude,
        longitude: location.coords.longitude,
        emergencyId: currentEmergency?.id,
      });

      Alert.alert(
        '✅ Reporte Enviado',
        'Tu reporte ha sido enviado y está pendiente de verificación.',
        [{ text: 'OK', onPress: () => router.back() }]
      );
    } catch (error: any) {
      Alert.alert('Error', error.response?.data?.message || 'Error al enviar el reporte');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Nuevo Reporte</Text>
        <Text style={styles.subtitle}>
          Reporta un incidente para ayudar a otros evacuantes
        </Text>
        {currentEmergency && (
          <View style={styles.emergencyBadge}>
            <Text style={styles.emergencyBadgeText}>
              ⚠️ Emergencia Activa: {currentEmergency.title}
            </Text>
          </View>
        )}
      </View>

      <View style={styles.form}>
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Tipo de Incidente</Text>
          <View style={styles.typeGrid}>
            {INCIDENT_TYPES.map((type) => (
              <TouchableOpacity
                key={type.value}
                style={[
                  styles.typeButton,
                  formData.type === type.value && styles.typeButtonActive,
                ]}
                onPress={() => setFormData({ ...formData, type: type.value })}
              >
                <Text style={styles.typeIcon}>{type.icon}</Text>
                <Text style={[
                  styles.typeLabel,
                  formData.type === type.value && styles.typeLabelActive,
                ]}>
                  {type.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Severidad</Text>
          <View style={styles.severityRow}>
            {SEVERITY_OPTIONS.map((sev) => (
              <TouchableOpacity
                key={sev.value}
                style={[
                  styles.severityButton,
                  formData.severity === sev.value && styles.severityButtonActive,
                  { borderColor: sev.color },
                ]}
                onPress={() => setFormData({ ...formData, severity: sev.value })}
              >
                <View style={[
                  styles.severityDot,
                  { backgroundColor: sev.color },
                  formData.severity === sev.value && styles.severityDotActive,
                ]} />
                <Text style={[
                  styles.severityLabel,
                  formData.severity === sev.value && styles.severityLabelActive,
                ]}>
                  {sev.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Descripción</Text>
          <TextInput
            style={styles.textArea}
            placeholder="Describe el incidente (opcional)"
            value={formData.description}
            onChangeText={(text) => setFormData({ ...formData, description: text })}
            multiline
            numberOfLines={4}
            textAlignVertical="top"
          />
        </View>

        <View style={styles.locationInfo}>
          <Text style={styles.locationLabel}>📍 Ubicación</Text>
          {isLoading ? (
            <ActivityIndicator size="small" color="#1A237E" />
          ) : location ? (
            <Text style={styles.locationText}>
              Lat: {location.coords.latitude.toFixed(6)}, Lng: {location.coords.longitude.toFixed(6)}
            </Text>
          ) : (
            <Text style={styles.locationText}>Obteniendo ubicación...</Text>
          )}
        </View>

        <TouchableOpacity
          style={[styles.submitButton, isSubmitting && styles.submitButtonDisabled]}
          onPress={handleSubmit}
          disabled={isSubmitting || !location}
        >
          {isSubmitting ? (
            <ActivityIndicator color="#FFFFFF" />
          ) : (
            <Text style={styles.submitButtonText}>Enviar Reporte</Text>
          )}
        </TouchableOpacity>
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
  emergencyBadge: {
    backgroundColor: '#D32F2F',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 8,
    marginTop: 12,
  },
  emergencyBadgeText: {
    color: '#FFFFFF',
    fontWeight: '600',
    fontSize: 14,
  },
  form: {
    padding: 16,
  },
  section: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 12,
  },
  typeGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  typeButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: '#F5F5F5',
    borderWidth: 1,
    borderColor: '#E0E0E0',
    marginRight: 6,
    marginBottom: 6,
  },
  typeButtonActive: {
    backgroundColor: '#1A237E',
    borderColor: '#1A237E',
  },
  typeIcon: {
    fontSize: 16,
    marginRight: 6,
  },
  typeLabel: {
    fontSize: 13,
    color: '#333',
  },
  typeLabelActive: {
    color: '#FFFFFF',
  },
  severityRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  severityButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    borderWidth: 1,
    backgroundColor: '#F5F5F5',
  },
  severityButtonActive: {
    backgroundColor: '#F5F5F5',
    borderWidth: 2,
  },
  severityDot: {
    width: 12,
    height: 12,
    borderRadius: 6,
    marginRight: 6,
  },
  severityDotActive: {
    width: 16,
    height: 16,
    borderRadius: 8,
  },
  severityLabel: {
    fontSize: 13,
    color: '#333',
  },
  severityLabelActive: {
    fontWeight: '600',
  },
  textArea: {
    borderWidth: 1,
    borderColor: '#DDD',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    backgroundColor: '#FAFAFA',
    minHeight: 100,
  },
  locationInfo: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
  },
  locationLabel: {
    fontSize: 14,
    fontWeight: '500',
    color: '#333',
    marginBottom: 4,
  },
  locationText: {
    fontSize: 13,
    color: '#666',
  },
  submitButton: {
    backgroundColor: '#1A237E',
    borderRadius: 12,
    paddingVertical: 16,
    alignItems: 'center',
  },
  submitButtonDisabled: {
    backgroundColor: '#B0BEC5',
  },
  submitButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
});