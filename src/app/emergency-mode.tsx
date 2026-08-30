import React, { useState } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, ScrollView, Platform, Animated } from 'react-native';
import { useRouter } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';
import { WebView } from 'react-native-webview';

export default function EmergencyModeScreen() {
  const router = useRouter();
  
  // Simulación de reportes comunitarios y estado de ruta
  const [routeIndex, setRouteIndex] = useState(0);
  const communityReports = [
    "⚠️ Alerta vecinal: Congestión moderada reportada en sector norte por caída de ramas.",
    "🚨 Reporte de usuario: Bloqueo temporal en cruce principal. Desvío seguro activado.",
    "ℹ️ Zona libre de incidentes reportados en los últimos 10 minutos."
  ];

  const routeOptions = [
    { distance: "2,4 km", time: "18 min", risk: "Bajo", desc: "La ruta sugerida evita las zonas de congestión principal reportadas por la comunidad. Proceda hacia el norte." },
    { distance: "3,8 km", time: "25 min", risk: "Mínimo", desc: "Ruta alternativa por circunvalación exterior. Mayor distancia pero flujo vehicular despejado." }
  ];

  const currentRoute = routeOptions[routeIndex];
  const activeReport = communityReports[routeIndex % communityReports.length];

  const osmEmbedUrl = `https://www.openstreetmap.org/export/embed.html?bbox=-70.74%2C-33.61%2C-70.68%2C-33.56&layer=mapnik&marker=-33.5880%2C-70.7000`;

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.contentContainer}>
      {/* Header Superior */}
      <View style={styles.header}>
        <TouchableOpacity style={styles.backButton} onPress={() => router.back()}>
          <MaterialIcons name="arrow-back" size={24} color="#f0f1f3" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Route Details</Text>
        <View style={styles.gpsBadge}>
          <View style={styles.greenDot} />
          <Text style={styles.gpsText}>GPS</Text>
        </View>
      </View>

      {/* Contenido Modo Emergencia */}
      <View style={styles.emergencyBox}>
        <View style={styles.warningRow}>
          <MaterialIcons name="warning" size={22} color="#ba1a1a" />
          <View style={styles.pulsingWarningDot} />
          <MaterialIcons name="warning" size={22} color="#ba1a1a" />
        </View>
        <Text style={styles.emergencyTitle}>Modo Emergencia</Text>
        <Text style={styles.emergencySubtitle}>Siga las instrucciones cuidadosamente. Mantenga la calma.</Text>

        {/* Banner de Reportes Comunitarios (Simulación Frontend) */}
        <View style={styles.communityAlertBanner}>
          <MaterialIcons name="campaign" size={18} color="#ffb3ac" />
          <Text style={styles.communityAlertText}>{activeReport}</Text>
        </View>
      </View>

      {/* Mapa Táctico */}
      <View style={styles.mapWrapper}>
        {Platform.OS === 'web' ? (
          // @ts-ignore
          <iframe src={osmEmbedUrl} style={{ width: '100%', height: '100%', border: 0 }} title="Mapa de Emergencia" />
        ) : (
          <WebView source={{ uri: osmEmbedUrl }} style={{ flex: 1, width: '100%', height: '100%' }} />
        )}
      </View>

      {/* Tarjetas de Datos de Ruta */}
      <View style={styles.dataCardsContainer}>
        <View style={styles.statsCard}>
          <View style={styles.statsRow}>
            <View>
              <Text style={styles.statLabel}>ZONA SEGURA MÁS CERCANA</Text>
              <Text style={styles.statValue}>{currentRoute.distance}</Text>
            </View>
            <View style={{ alignItems: 'flex-end' }}>
              <Text style={styles.statLabel}>TIEMPO EST.</Text>
              <Text style={styles.statValueTime}>{currentRoute.time}</Text>
            </View>
          </View>

          <View style={styles.riskRow}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
              <MaterialIcons name="verified-user" size={18} color="#10b981" />
              <Text style={styles.riskLabel}>Nivel de riesgo actual</Text>
            </View>
            <View style={styles.riskBadge}>
              <Text style={styles.riskBadgeText}>{currentRoute.risk}</Text>
            </View>
          </View>
        </View>

        {/* Nota de Guía */}
        <View style={styles.guidanceCard}>
          <MaterialIcons name="directions-run" size={20} color="#003d9b" />
          <Text style={styles.guidanceText}>{currentRoute.desc}</Text>
        </View>
      </View>

      {/* Botones de Acción */}
      <View style={styles.actionsContainer}>
        <TouchableOpacity style={styles.primaryButton} activeOpacity={0.9}>
          <MaterialIcons name="navigation" size={20} color="#fff" />
          <Text style={styles.primaryButtonText}>Iniciar Evacuación</Text>
        </TouchableOpacity>

        <TouchableOpacity 
          style={styles.secondaryButton} 
          onPress={() => setRouteIndex(prev => (prev + 1) % routeOptions.length)}
          activeOpacity={0.8}
        >
          <MaterialIcons name="route" size={20} color="#f0f1f3" />
          <Text style={styles.secondaryButtonText}>Buscar Otra Ruta (Simular Desvío)</Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#2e3132' },
  contentContainer: { paddingBottom: 40 },
  header: { height: 60, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingTop: 10, backgroundColor: '#2e3132', borderBottomWidth: 1, borderBottomColor: '#434654' },
  backButton: { width: 40, height: 40, justifyContent: 'center', alignItems: 'center' },
  headerTitle: { fontSize: 16, fontWeight: '600', color: '#f0f1f3' },
  gpsBadge: { flexDirection: 'row', alignItems: 'center', gap: 6, backgroundColor: 'rgba(255,255,255,0.08)', paddingHorizontal: 8, paddingVertical: 4, borderRadius: 12 },
  greenDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: '#10b981' },
  gpsText: { fontSize: 11, fontWeight: '700', color: '#c3c6d6' },
  emergencyBox: { alignItems: 'center', paddingVertical: 20, paddingHorizontal: 20 },
  warningRow: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 8 },
  pulsingWarningDot: { width: 10, height: 10, borderRadius: 5, backgroundColor: '#ba1a1a', shadowColor: '#ba1a1a', shadowRadius: 10, shadowOpacity: 0.8, elevation: 6 },
  emergencyTitle: { fontSize: 20, fontWeight: '800', color: '#ba1a1a', textTransform: 'uppercase', letterSpacing: 1.5, textAlign: 'center' },
  emergencySubtitle: { fontSize: 13, color: 'rgba(240,241,243,0.7)', textAlign: 'center', marginTop: 4 },
  communityAlertBanner: { flexDirection: 'row', alignItems: 'center', gap: 8, backgroundColor: 'rgba(140, 0, 14, 0.25)', borderWidth: 1, borderColor: '#ba1a1a', padding: 10, borderRadius: 10, marginTop: 12, width: '100%' },
  communityAlertText: { fontSize: 12, color: '#ffc5bf', flex: 1, fontWeight: '500' },
  mapWrapper: { width: '100%', height: 240, backgroundColor: '#191c1e', position: 'relative' },
  dataCardsContainer: { paddingHorizontal: 20, marginTop: -20, zIndex: 10, gap: 12 },
  statsCard: { backgroundColor: 'rgba(0,0,0,0.45)', borderRadius: 14, padding: 16, borderWidth: 1, borderColor: 'rgba(255,255,255,0.1)' },
  statsRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  statLabel: { fontSize: 10, fontWeight: '700', color: 'rgba(240,241,243,0.6)', letterSpacing: 1 },
  statValue: { fontSize: 24, fontWeight: '700', color: '#f0f1f3', marginTop: 2 },
  statValueTime: { fontSize: 20, fontWeight: '700', color: '#f0f1f3', marginTop: 2 },
  riskRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 14, backgroundColor: 'rgba(0,0,0,0.3)', padding: 10, borderRadius: 8 },
  riskLabel: { fontSize: 12, fontWeight: '600', color: '#f0f1f3' },
  riskBadge: { backgroundColor: '#10b981', paddingHorizontal: 12, paddingVertical: 4, borderRadius: 12, shadowColor: '#10b981', shadowOpacity: 0.4, elevation: 4 },
  riskBadgeText: { fontSize: 11, fontWeight: '800', color: '#ffffff', textTransform: 'uppercase' },
  guidanceCard: { flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: 'rgba(0, 82, 204, 0.155)', borderWidth: 1, borderColor: '#0052cc', borderRadius: 12, padding: 14 },
  guidanceText: { fontSize: 12, color: 'rgba(240,241,243,0.85)', flex: 1, lineHeight: 18 },
  actionsContainer: { paddingHorizontal: 20, marginTop: 20, gap: 12 },
  primaryButton: { backgroundColor: '#10b981', height: 52, borderRadius: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, shadowColor: '#10b981', shadowOpacity: 0.3, shadowRadius: 8, elevation: 5 },
  primaryButtonText: { color: '#ffffff', fontSize: 13, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 0.5 },
  secondaryButton: { backgroundColor: 'rgba(240,241,243,0.1)', height: 52, borderRadius: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8 },
  secondaryButtonText: { color: '#f0f1f3', fontSize: 12, fontWeight: '600', textTransform: 'uppercase', letterSpacing: 0.5 }
});