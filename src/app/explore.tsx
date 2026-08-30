import React, { useState } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, Platform, Modal } from 'react-native';
import { useRouter } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';
import { WebView } from 'react-native-webview';

export default function RoutesScreen() {
  const router = useRouter();
  
  // Simulación de variables del Backend impulsado por Machine Learning
  const [mobilityMode, setMobilityMode] = useState<'vehiculo' | 'pie'>('vehiculo');
  const [routeVariant, setRouteVariant] = useState(0);
  const [sosModalVisible, setSosModalVisible] = useState(false);

  // Datos simulados del modelo ML según reportes comunitarios y modalidad
  const mlRoutes = [
    {
      instruction: "Continúa por Avenida Principal",
      distance: "350 m",
      estTime: mobilityMode === 'vehiculo' ? "8 min" : "14 min",
      distanceTotal: "1,2 km",
      riskLevel: "Moderado",
      confidence: "94% IA",
      warning: "Zona de riesgo adelante (Reporte comunitario verificado)",
    },
    {
      instruction: "Gira a la derecha en Calle Los Cerezos (Desvío ML)",
      distance: "600 m",
      estTime: mobilityMode === 'vehiculo' ? "11 min" : "18 min",
      distanceTotal: "1,8 km",
      riskLevel: "Bajo",
      confidence: "98% IA",
      warning: "Ruta optimizada por Machine Learning para evitar congestión",
    }
  ];

  const currentRoute = mlRoutes[routeVariant];
  const osmEmbedUrl = `https://www.openstreetmap.org/export/embed.html?bbox=-70.72%2C-33.60%2C-70.68%2C-33.58&layer=mapnik&marker=-33.5900%2C-70.7010`;

  return (
    <View style={styles.container}>
      {/* Header Fijo */}
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <View style={styles.headerLeft}>
            <ImagePlaceholder />
            <View>
              <Text style={styles.headerTitle}>Rutas (ML Activo)</Text>
              <View style={styles.statusRow}>
                <View style={styles.statusIndicator}>
                  <View style={styles.greenDot} />
                  <Text style={styles.statusText}>En línea</Text>
                </View>
                <View style={styles.separatorDot} />
                <View style={styles.statusIndicator}>
                  <View style={styles.greenDot} />
                  <Text style={styles.statusText}>GPS Activo</Text>
                </View>
              </View>
            </View>
          </View>
          <View style={styles.headerRight}>
            <TouchableOpacity 
              style={styles.mobilityToggleBtn}
              onPress={() => setMobilityMode(prev => prev === 'vehiculo' ? 'pie' : 'vehiculo')}
            >
              <MaterialIcons name={mobilityMode === 'vehiculo' ? 'directions-car' : 'directions-walk'} size={18} color="#003d9b" />
              <Text style={styles.mobilityToggleText}>{mobilityMode === 'vehiculo' ? 'Auto' : 'A pie'}</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>

      {/* Contenedor del Mapa con simulación ML */}
      <View style={styles.mapContainer}>
        {Platform.OS === 'web' ? (
          // @ts-ignore
          <iframe src={osmEmbedUrl} style={{ width: '100%', height: '100%', border: 0 }} title="Mapa de Rutas" />
        ) : (
          <WebView source={{ uri: osmEmbedUrl }} style={{ flex: 1, width: '100%', height: '100%' }} />
        )}

        {/* Tarjeta de Instrucción Superior (Pronóstico ML) */}
        <View style={styles.topInstructionCard}>
          <View style={styles.instructionIconBox}>
            <MaterialIcons name="turn-right" size={28} color="#ffffff" />
          </View>
          <View style={{ flex: 1 }}>
            <View style={{ flexDirection: 'row', alignItems: 'baseline', gap: 4 }}>
              <Text style={styles.instructionDistance}>{currentRoute.distance}</Text>
              <Text style={styles.instructionUnit}>m</Text>
              <Text style={styles.mlConfidenceBadge}>{currentRoute.confidence}</Text>
            </View>
            <Text style={styles.instructionText} numberOfLines={1}>{currentRoute.instruction}</Text>
          </View>
        </View>

        {/* Botones Flotantes Laterales */}
        <View style={styles.floatingControls}>
          <TouchableOpacity 
            style={styles.controlButton} 
            onPress={() => setRouteVariant(prev => (prev + 1) % mlRoutes.length)}
          >
            <MaterialIcons name="alt-route" size={22} color="#434654" />
          </TouchableOpacity>
          <TouchableOpacity style={styles.controlButton}>
            <MaterialIcons name="my-location" size={22} color="#434654" />
          </TouchableOpacity>
        </View>

        {/* Bottom Sheet Informativo de Ruta */}
        <View style={styles.bottomSheet}>
          <View style={styles.bottomSheetIndicator} />
          
          <View style={styles.warningBanner}>
            <MaterialIcons name="error" size={18} color="#93000a" />
            <Text style={styles.warningText}>{currentRoute.warning}</Text>
          </View>

          <View style={styles.destinationRow}>
            <View>
              <Text style={styles.destLabel}>Destino</Text>
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 4, marginTop: 2 }}>
                <MaterialIcons name="verified-user" size={16} color="#16a34a" />
                <Text style={styles.destTitle}>Zona segura ({currentRoute.riskLevel} Riesgo)</Text>
              </View>
            </View>
            <View style={{ alignItems: 'flex-end' }}>
              <Text style={styles.estTimeText}>{currentRoute.estTime}</Text>
              <Text style={styles.estDistanceText}>{currentRoute.distanceTotal}</Text>
            </View>
          </View>

          <View style={styles.actionButtonsRow}>
            <TouchableOpacity style={styles.exitButton} onPress={() => router.push('/home')}>
              <MaterialIcons name="close" size={20} color="#191c1e" />
              <Text style={styles.exitButtonText}>Salir</Text>
            </TouchableOpacity>

            <TouchableOpacity style={styles.sosButton} onPress={() => setSosModalVisible(true)}>
              <MaterialIcons name="sos" size={20} color="#ffffff" />
              <Text style={styles.sosButtonText}>SOS</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>

      {/* Modal de Alerta SOS */}
      <Modal visible={sosModalVisible} transparent={true} animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <MaterialIcons name="warning" size={40} color="#ba1a1a" />
            <Text style={styles.modalTitle}>¡EMERGENCIA SOS ACTIVADA!</Text>
            <Text style={styles.modalSub}>Transmitiendo tu ubicación GPS y tipo de movilidad ({mobilityMode}) a los servicios de rescate cercanos.</Text>
            <TouchableOpacity style={styles.modalCloseBtn} onPress={() => setSosModalVisible(false)}>
              <Text style={styles.modalCloseText}>Cancelar / Cerrar</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* Barra de Navegación Inferior */}
      <View style={styles.bottomNav}>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/home')}>
          <MaterialIcons name="map" size={24} color="#434654" />
          <Text style={styles.navText}>Mapa</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItemActive}>
          <MaterialIcons name="directions" size={24} color="#003d9b" />
          <Text style={styles.navTextActive}>Rutas</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/report' as any)}>
          <MaterialIcons name="emergency" size={24} color="#434654" />
          <Text style={styles.navText}>Reportar</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/alerts' as any)}>
          <MaterialIcons name="notifications-active" size={24} color="#434654" />
          <Text style={styles.navText}>Alertas</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/profile' as any)}>
          <MaterialIcons name="person" size={24} color="#434654" />
          <Text style={styles.navText}>Perfil</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

function ImagePlaceholder() {
  return <View style={{ width: 32, height: 32, borderRadius: 16, backgroundColor: '#003d9b' }} />;
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8f9fb' },
  header: { position: 'absolute', top: 0, width: '100%', zIndex: 50, backgroundColor: 'rgba(248, 249, 251, 0.9)', borderBottomWidth: 1, borderBottomColor: '#e7e8ea' },
  headerContent: { height: 80, paddingHorizontal: 20, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingTop: 15 },
  headerLeft: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  headerTitle: { fontSize: 16, fontWeight: '700', color: '#191c1e' },
  statusRow: { flexDirection: 'row', alignItems: 'center', marginTop: 2, gap: 6 },
  statusIndicator: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  greenDot: { width: 7, height: 7, borderRadius: 3.5, backgroundColor: '#16a34a' },
  separatorDot: { width: 3, height: 3, borderRadius: 1.5, backgroundColor: '#c3c6d6' },
  statusText: { fontSize: 10, fontWeight: '700', color: '#434654', textTransform: 'uppercase' },
  headerRight: { flexDirection: 'row', alignItems: 'center' },
  mobilityToggleBtn: { flexDirection: 'row', alignItems: 'center', gap: 6, backgroundColor: '#e2e8f0', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 15 },
  mobilityToggleText: { fontSize: 12, fontWeight: '700', color: '#003d9b' },
  mapContainer: { flex: 1, marginTop: 80, marginBottom: 80, position: 'relative', backgroundColor: '#e1e2e4' },
  topInstructionCard: { position: 'absolute', top: 16, left: 16, right: 16, zIndex: 20, backgroundColor: '#003d9b', borderRadius: 14, padding: 14, flexDirection: 'row', alignItems: 'center', gap: 12, elevation: 6 },
  instructionIconBox: { backgroundColor: 'rgba(255,255,255,0.15)', padding: 10, borderRadius: 10 },
  instructionDistance: { fontSize: 26, fontWeight: '800', color: '#ffffff', lineHeight: 28 },
  instructionUnit: { fontSize: 16, fontWeight: '700', color: 'rgba(255,255,255,0.8)' },
  mlConfidenceBadge: { fontSize: 10, fontWeight: '800', backgroundColor: '#16a34a', color: '#fff', paddingHorizontal: 6, paddingVertical: 2, borderRadius: 6, overflow: 'hidden', marginLeft: 8 },
  instructionText: { fontSize: 13, color: '#ffffff', fontWeight: '500', marginTop: 2 },
  floatingControls: { position: 'absolute', top: 110, right: 16, zIndex: 20, gap: 8 },
  controlButton: { width: 44, height: 44, backgroundColor: '#ffffff', borderRadius: 22, justifyContent: 'center', alignItems: 'center', elevation: 4 },
  bottomSheet: { position: 'absolute', bottom: 0, width: '100%', backgroundColor: '#ffffff', borderTopLeftRadius: 28, borderTopRightRadius: 28, padding: 20, elevation: 12, zIndex: 30, gap: 12 },
  bottomSheetIndicator: { width: 40, height: 5, backgroundColor: '#c3c6d6', borderRadius: 2.5, alignSelf: 'center', marginBottom: 2 },
  warningBanner: { flexDirection: 'row', alignItems: 'center', gap: 8, backgroundColor: '#ffdad6', padding: 10, borderRadius: 10 },
  warningText: { fontSize: 11, fontWeight: '700', color: '#93000a', flex: 1 },
  destinationRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end', marginVertical: 4 },
  destLabel: { fontSize: 11, color: '#5d5e61' },
  destTitle: { fontSize: 14, fontWeight: '700', color: '#191c1e' },
  estTimeText: { fontSize: 18, fontWeight: '800', color: '#003d9b' },
  estDistanceText: { fontSize: 12, color: '#5d5e61' },
  actionButtonsRow: { flexDirection: 'row', gap: 12, marginTop: 4 },
  exitButton: { flex: 1, backgroundColor: '#edeef0', height: 48, borderRadius: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 6 },
  exitButtonText: { fontSize: 13, fontWeight: '700', color: '#191c1e' },
  sosButton: { flex: 1, backgroundColor: '#ba1a1a', height: 48, borderRadius: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 6, elevation: 4 },
  sosButtonText: { fontSize: 13, fontWeight: '700', color: '#ffffff' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.6)', justifyContent: 'center', alignItems: 'center', padding: 20 },
  modalContent: { backgroundColor: '#fff', borderRadius: 20, padding: 24, alignItems: 'center', gap: 12, width: '100%', maxWidth: 320 },
  modalTitle: { fontSize: 16, fontWeight: '800', color: '#ba1a1a', textAlign: 'center' },
  modalSub: { fontSize: 12, color: '#434654', textAlign: 'center', lineHeight: 18 },
  modalCloseBtn: { backgroundColor: '#191c1e', paddingVertical: 10, paddingHorizontal: 20, borderRadius: 10, marginTop: 8 },
  modalCloseText: { color: '#fff', fontWeight: '700', fontSize: 12 },
  bottomNav: { position: 'absolute', bottom: 0, width: '100%', height: 80, backgroundColor: 'rgba(248, 249, 251, 0.95)', borderTopWidth: 1, borderTopColor: '#e7e8ea', flexDirection: 'row', justifyContent: 'space-around', alignItems: 'center', zIndex: 50 },
  navItem: { flex: 1, height: '100%', justifyContent: 'center', alignItems: 'center', gap: 2 },
  navItemActive: { flex: 1, height: '100%', justifyContent: 'center', alignItems: 'center', gap: 2 },
  navText: { fontSize: 11, fontWeight: '500', color: '#434654' },
  navTextActive: { fontSize: 11, fontWeight: '700', color: '#003d9b' }
});