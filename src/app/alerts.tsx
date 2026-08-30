import React, { useState } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, ScrollView, Platform, useWindowDimensions } from 'react-native';
import { useRouter } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';

export default function AlertsScreen() {
  const router = useRouter();
  const { width } = useWindowDimensions();
  const isDesktop = width > 768;

  const [alerts, setAlerts] = useState([
    {
      id: '1',
      title: 'Alerta Roja: Evacuación Preventiva',
      description: 'Se solicita evacuación inmediata hacia la Zona Segura 01 debido al desborde de canal principal.',
      time: 'Hace 5 min',
      severity: 'high',
      icon: 'warning',
    },
    {
      id: '2',
      title: 'Aviso ML: Desvío en Ruta Principal',
      description: 'El sistema de Machine Learning detectó alta congestión en Av. Los Cerezos. Se recomienda tomar ruta alternativa.',
      time: 'Hace 22 min',
      severity: 'medium',
      icon: 'alt-route',
    },
    {
      id: '3',
      title: 'Reporte Comunitario Verificado',
      description: 'Anegamiento moderado reportado en intersección Calle 4 con Pasaje Los Copihues.',
      time: 'Hace 45 min',
      severity: 'low',
      icon: 'water',
    },
  ]);

  return (
    <View style={[styles.container, isDesktop && styles.desktopContainer]}>
      {/* Header Fijo */}
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <View style={styles.headerLeft}>
            <View style={styles.avatarPlaceholder} />
            <View>
              <Text style={styles.headerTitle}>Centro de Alertas</Text>
              <Text style={styles.headerSubtitle}>Avisos oficiales y del sistema</Text>
            </View>
          </View>
        </View>
      </View>

      {/* Contenido Desplazable */}
      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        <View style={styles.listContainer}>
          {alerts.map((item) => {
            const isHigh = item.severity === 'high';
            const isMedium = item.severity === 'medium';
            return (
              <View 
                key={item.id} 
                style={[
                  styles.alertCard, 
                  isHigh && styles.alertCardHigh,
                  isMedium && styles.alertCardMedium
                ]}
              >
                <View style={styles.alertHeaderRow}>
                  <View style={[
                    styles.iconBox, 
                    isHigh ? styles.iconBoxHigh : isMedium ? styles.iconBoxMedium : styles.iconBoxLow
                  ]}>
                    <MaterialIcons 
                      name={item.icon as any} 
                      size={20} 
                      color={isHigh ? '#ba1a1a' : isMedium ? '#d97706' : '#0284c7'} 
                    />
                  </View>
                  <View style={{ flex: 1, marginLeft: 10 }}>
                    <Text style={styles.alertTitle}>{item.title}</Text>
                    <Text style={styles.alertTime}>{item.time}</Text>
                  </View>
                </View>
                <Text style={styles.alertDescription}>{item.description}</Text>
              </View>
            );
          })}
        </View>
      </ScrollView>

      {/* Barra de Navegación Inferior */}
      <View style={styles.bottomNav}>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/map')}>
          <MaterialIcons name="map" size={24} color="#434654" />
          <Text style={styles.navText}>Mapa</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/routes')}>
          <MaterialIcons name="directions" size={24} color="#434654" />
          <Text style={styles.navText}>Rutas</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/report')}>
          <MaterialIcons name="emergency" size={24} color="#434654" />
          <Text style={styles.navText}>Reportar</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItemActive}>
          <MaterialIcons name="notifications-active" size={24} color="#003d9b" />
          <Text style={styles.navTextActive}>Alertas</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/profile')}>
          <MaterialIcons name="person" size={24} color="#434654" />
          <Text style={styles.navText}>Perfil</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8f9fb', width: '100%', height: '100%' },
  desktopContainer: { maxWidth: 480, alignSelf: 'center', shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.1, shadowRadius: 12, elevation: 8 },
  header: { position: 'absolute', top: 0, width: '100%', zIndex: 50, backgroundColor: 'rgba(248, 249, 251, 0.95)', borderBottomWidth: 1, borderBottomColor: '#e7e8ea' },
  headerContent: { height: 80, paddingHorizontal: 20, flexDirection: 'row', alignItems: 'center', paddingTop: 15 },
  headerLeft: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  avatarPlaceholder: { width: 32, height: 32, borderRadius: 16, backgroundColor: '#003d9b' },
  headerTitle: { fontSize: 15, fontWeight: '700', color: '#191c1e' },
  headerSubtitle: { fontSize: 11, color: '#5d5e61', marginTop: 1 },
  scrollContent: { paddingTop: 95, paddingBottom: 100, paddingHorizontal: 16 },
  listContainer: { gap: 12 },
  alertCard: { backgroundColor: '#ffffff', borderRadius: 16, padding: 16, borderWidth: 1, borderColor: '#e2e8f0', elevation: 1, gap: 10 },
  alertCardHigh: { borderColor: '#fca5a5', backgroundColor: '#fff5f5' },
  alertCardMedium: { borderColor: '#fde68a', backgroundColor: '#fffbeb' },
  alertHeaderRow: { flexDirection: 'row', alignItems: 'center' },
  iconBox: { width: 36, height: 36, borderRadius: 10, justifyContent: 'center', alignItems: 'center' },
  iconBoxHigh: { backgroundColor: '#fee2e2' },
  iconBoxMedium: { backgroundColor: '#fef3c7' },
  iconBoxLow: { backgroundColor: '#e0f2fe' },
  alertTitle: { fontSize: 13, fontWeight: '700', color: '#191c1e' },
  alertTime: { fontSize: 10, color: '#64748b', marginTop: 1 },
  alertDescription: { fontSize: 12, color: '#334155', lineHeight: 18 },
  bottomNav: { position: 'absolute', bottom: 0, width: '100%', height: 80, backgroundColor: 'rgba(248, 249, 251, 0.95)', borderTopWidth: 1, borderTopColor: '#e7e8ea', flexDirection: 'row', justifyContent: 'space-around', alignItems: 'center', zIndex: 50 },
  navItem: { flex: 1, height: '100%', justifyContent: 'center', alignItems: 'center', gap: 2 },
  navItemActive: { flex: 1, height: '100%', justifyContent: 'center', alignItems: 'center', gap: 2, backgroundColor: 'rgba(0, 61, 155, 0.04)' },
  navText: { fontSize: 11, fontWeight: '500', color: '#434654' },
  navTextActive: { fontSize: 11, fontWeight: '700', color: '#003d9b' }
});