import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  RefreshControl,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useEmergency } from '../../context/EmergencyContext';
import { useLocation } from '../../hooks/useLocation';
import { safeZonesApi } from '../../api/endpoints/safe-zones';
import { incidentsApi } from '../../api/endpoints/incidents';
import { Incident } from '../../types';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';

export default function AlertsTab() {
  const router = useRouter();
  const { emergencyMode, currentEmergency } = useEmergency();
  const { location } = useLocation();
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const intervalRef = useRef<NodeJS.Timeout>();

  useEffect(() => {
    loadData();
    startPolling();
    return () => stopPolling();
  }, []);

  const startPolling = () => {
    intervalRef.current = setInterval(loadData, 30000); // Cada 30 segundos
  };

  const stopPolling = () => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }
  };

  const loadData = async () => {
    try {
      const [incidentsResponse] = await Promise.all([
        incidentsApi.list(currentEmergency?.id),
      ]);
      setIncidents(incidentsResponse.data);
    } catch (error) {
      console.error('Error loading data:', error);
    } finally {
      setIsLoading(false);
      setRefreshing(false);
    }
  };

  const onRefresh = () => {
    setRefreshing(true);
    loadData();
  };

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'LOW': return '#4CAF50';
      case 'MEDIUM': return '#FFC107';
      case 'HIGH': return '#FF9800';
      case 'CRITICAL': return '#D32F2F';
      default: return '#999';
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'VERIFIED':
        return { text: '✅ Verificado', color: '#4CAF50' };
      case 'PENDING':
        return { text: '⏳ Pendiente', color: '#FFC107' };
      case 'REJECTED':
        return { text: '❌ Rechazado', color: '#D32F2F' };
      default:
        return { text: '📌 Desconocido', color: '#999' };
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
    <ScrollView
      style={styles.container}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
      }
    >
      <View style={styles.header}>
        <Text style={styles.title}>Alertas</Text>
        <Text style={styles.subtitle}>
          Incidentes reportados por la comunidad
        </Text>
      </View>

      {emergencyMode && currentEmergency && (
        <View style={styles.emergencyBanner}>
          <Text style={styles.emergencyTitle}>🚨 {currentEmergency.title}</Text>
          <Text style={styles.emergencyDescription}>
            {currentEmergency.description}
          </Text>
        </View>
      )}

      <View style={styles.filterRow}>
        <TouchableOpacity style={[styles.filterButton, styles.filterButtonActive]}>
          <Text style={styles.filterButtonText}>Todos</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.filterButton}>
          <Text style={styles.filterButtonText}>Verificados</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.filterButton}>
          <Text style={styles.filterButtonText}>Pendientes</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.list}>
        <Text style={styles.listTitle}>Incidentes Recientes</Text>

        {incidents.length === 0 ? (
          <View style={styles.emptyState}>
            <Text style={styles.emptyStateIcon}>✅</Text>
            <Text style={styles.emptyStateText}>No hay incidentes reportados</Text>
            <Text style={styles.emptyStateSubtext}>
              Reporta cualquier peligro que encuentres durante la evacuación
            </Text>
          </View>
        ) : (
          incidents.map((incident) => {
            const status = getStatusBadge(incident.status);
            const severityColor = getSeverityColor(incident.severity);
            
            return (
              <TouchableOpacity
                key={incident.id}
                style={styles.incidentCard}
                onPress={() => router.push(`/incident-detail/${incident.id}`)}
              >
                <View style={styles.incidentHeader}>
                  <View style={styles.incidentType}>
                    <Text style={styles.incidentTypeText}>{incident.type}</Text>
                  </View>
                  <View style={[styles.statusBadge, { backgroundColor: status.color + '20' }]}>
                    <Text style={[styles.statusBadgeText, { color: status.color }]}>
                      {status.text}
                    </Text>
                  </View>
                </View>

                {incident.description && (
                  <Text style={styles.incidentDescription} numberOfLines={2}>
                    {incident.description}
                  </Text>
                )}

                <View style={styles.incidentFooter}>
                  <View style={[styles.severityIndicator, { backgroundColor: severityColor }]}>
                    <Text style={styles.severityText}>
                      {incident.severity}
                    </Text>
                  </View>
                  <Text style={styles.incidentTime}>
                    {new Date(incident.createdAt).toLocaleString()}
                  </Text>
                </View>
              </TouchableOpacity>
            );
          })
        )}
      </View>

      <TouchableOpacity
        style={styles.fab}
        onPress={() => router.push('/report-form')}
      >
        <Text style={styles.fabText}>+</Text>
      </TouchableOpacity>
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
  emergencyBanner: {
    backgroundColor: '#D32F2F',
    margin: 16,
    padding: 16,
    borderRadius: 12,
  },
  emergencyTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  emergencyDescription: {
    fontSize: 14,
    color: '#FFEBEE',
    marginTop: 4,
  },
  filterRow: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 8,
  },
  filterButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: '#E0E0E0',
  },
  filterButtonActive: {
    backgroundColor: '#1A237E',
  },
  filterButtonText: {
    fontSize: 13,
    color: '#333',
  },
  list: {
    paddingHorizontal: 16,
    paddingBottom: 80,
  },
  listTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 12,
  },
  emptyState: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 32,
    alignItems: 'center',
  },
  emptyStateIcon: {
    fontSize: 48,
    marginBottom: 12,
  },
  emptyStateText: {
    fontSize: 16,
    fontWeight: '500',
    color: '#333',
  },
  emptyStateSubtext: {
    fontSize: 14,
    color: '#666',
    textAlign: 'center',
    marginTop: 4,
  },
  incidentCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
  },
  incidentHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  incidentType: {
    backgroundColor: '#E3F2FD',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
  },
  incidentTypeText: {
    fontSize: 12,
    fontWeight: '500',
    color: '#1A237E',
  },
  statusBadge: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
  },
  statusBadgeText: {
    fontSize: 12,
    fontWeight: '500',
  },
  incidentDescription: {
    fontSize: 14,
    color: '#333',
    marginBottom: 8,
  },
  incidentFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  severityIndicator: {
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 10,
  },
  severityText: {
    fontSize: 11,
    fontWeight: '600',
    color: '#FFFFFF',
  },
  incidentTime: {
    fontSize: 12,
    color: '#999',
  },
  fab: {
    position: 'absolute',
    bottom: 24,
    right: 24,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#1A237E',
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 4,
  },
  fabText: {
    fontSize: 32,
    color: '#FFFFFF',
    fontWeight: '300',
  },
});