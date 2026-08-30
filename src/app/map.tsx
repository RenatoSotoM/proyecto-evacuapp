import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, Platform, useWindowDimensions, ScrollView } from 'react-native';
import { useRouter } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';
import { WebView } from 'react-native-webview';
import * as Location from 'expo-location';

export default function MapScreen() {
  const router = useRouter();
  const { width } = useWindowDimensions();
  const isDesktop = width > 768;

  // Ubicación por defecto de respaldo (San Bernardo)
  const [mapCenter, setMapCenter] = useState({ lat: -33.5937, lon: -70.7029 }); 
  const [selectedService, setSelectedService] = useState<string | null>(null);
  const [emergencyServices, setEmergencyServices] = useState<any[]>([]);

  useEffect(() => {
    (async () => {
      let { status } = await Location.requestForegroundPermissionsAsync();
      let lat = -33.5937;
      let lon = -70.7029;

      if (status === 'granted') {
        let location = await Location.getCurrentPositionAsync({});
        lat = location.coords.latitude;
        lon = location.coords.longitude;
        setMapCenter({ lat, lon });
      }

      // Generar puntos de servicios estrictamente en el radio local de la comuna actual
      const localServices = [
        { id: 'hospital', name: 'Hospital de Urgencia Local', lat: lat + 0.035, lon: lon + 0.025, emoji: '🏥', type: 'hospital', dist: '4.2 km' },
        { id: 'bomberos', name: 'Compañía de Bomberos Local', lat: lat - 0.025, lon: lon + 0.030, emoji: '🚒', type: 'bomberos', dist: '3.8 km' },
        { id: 'policia', name: 'Comisaría de Carabineros Local', lat: lat + 0.020, lon: lon - 0.035, emoji: '🚨', type: 'policia', dist: '3.1 km' },
        { id: 'comercial', name: 'Centro Comercial / Zona Segura', lat: lat - 0.030, lon: lon - 0.020, emoji: '🏬', type: 'comercial', dist: '5.0 km' },
      ];
      setEmergencyServices(localServices);
    })();
  }, []);

  const filteredServices = selectedService 
    ? emergencyServices.filter(s => s.type === selectedService) 
    : emergencyServices;

  const generateMapHtml = () => {
    const markersScript = filteredServices.map(s => `
      L.marker([${s.lat}, ${s.lon}], {
        icon: L.divIcon({
          className: 'custom-emoji-marker',
          html: '<div style="font-size: 26px; background: white; border-radius: 50%; width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 8px rgba(0,0,0,0.3); border: 2px solid #003d9b;">${s.emoji}</div>',
          iconSize: [44, 44]
        })
      }).addTo(map).bindPopup('<b>${s.name}</b><br><span style="color:#003d9b; font-weight:bold;">A ${s.dist} de tu posición (Radio seguro)</span>');
    `).join('');

    // Marcador de la posición actual del usuario
    const userMarker = `
      L.marker([${mapCenter.lat}, ${mapCenter.lon}], {
        icon: L.divIcon({
          className: 'user-marker',
          html: '<div style="font-size: 20px; background: #003d9b; color: white; border-radius: 50%; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; box-shadow: 0 0 10px rgba(0,61,155,0.7); border: 2px solid white;">📍</div>',
          iconSize: [36, 36]
        })
      }).addTo(map).bindPopup('<b>Tu ubicación actual (GPS)</b>');
    `;

    return `
      <!DOCTYPE html>
      <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <style>body, html { margin: 0; padding: 0; height: 100%; width: 100%; }</style>
      </head>
      <body>
        <div id="map" style="width:100%; height:100%;"></div>
        <script>
          var map = L.map('map', { zoomControl: false }).setView([${mapCenter.lat}, ${mapCenter.lon}], 14);
          L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
          ${userMarker}
          ${markersScript}
        </script>
      </body>
      </html>
    `;
  };

  return (
    <View style={[styles.container, isDesktop && styles.desktopContainer]}>
      {/* Header Fijo */}
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <Text style={styles.headerTitle}>Mapa Local (Radio 15-20 km)</Text>
          <TouchableOpacity style={styles.routeRedirectBtn} onPress={() => router.push('/routes')}>
            <MaterialIcons name="directions" size={18} color="#ffffff" />
            <Text style={styles.routeRedirectText}>Rutas IA Propias</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Barra Horizontal de Filtros con Emojis */}
      <View style={styles.filterContainer}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8, paddingHorizontal: 16 }}>
          <TouchableOpacity 
            style={[styles.filterChip, selectedService === null && styles.filterChipActive]}
            onPress={() => setSelectedService(null)}
          >
            <Text style={[styles.filterText, selectedService === null && styles.filterTextActive]}>🌐 Todos</Text>
          </TouchableOpacity>
          <TouchableOpacity 
            style={[styles.filterChip, selectedService === 'hospital' && styles.filterChipActive]}
            onPress={() => setSelectedService('hospital')}
          >
            <Text style={[styles.filterText, selectedService === 'hospital' && styles.filterTextActive]}>🏥 Hospitales</Text>
          </TouchableOpacity>
          <TouchableOpacity 
            style={[styles.filterChip, selectedService === 'bomberos' && styles.filterChipActive]}
            onPress={() => setSelectedService('bomberos')}
          >
            <Text style={[styles.filterText, selectedService === 'bomberos' && styles.filterTextActive]}>🚒 Bomberos</Text>
          </TouchableOpacity>
          <TouchableOpacity 
            style={[styles.filterChip, selectedService === 'policia' && styles.filterChipActive]}
            onPress={() => setSelectedService('policia')}
          >
            <Text style={[styles.filterText, selectedService === 'policia' && styles.filterTextActive]}>🚨 Policía</Text>
          </TouchableOpacity>
          <TouchableOpacity 
            style={[styles.filterChip, selectedService === 'comercial' && styles.filterChipActive]}
            onPress={() => setSelectedService('comercial')}
          >
            <Text style={[styles.filterText, selectedService === 'comercial' && styles.filterTextActive]}>🏬 Zonas Seguras</Text>
          </TouchableOpacity>
        </ScrollView>
      </View>

      {/* Contenedor del Mapa Local con renderizado ultra rápido */}
      <View style={styles.mapContainer}>
        {Platform.OS === 'web' ? (
          // @ts-ignore
          <iframe 
            key={`${mapCenter.lat}-${mapCenter.lon}-${selectedService}`}
            srcDoc={generateMapHtml()} 
            style={{ width: '100%', height: '100%', border: 0 }} 
            title="Mapa Local Tesis" 
          />
        ) : (
          <WebView 
            key={`${mapCenter.lat}-${mapCenter.lon}-${selectedService}`}
            originWhitelist={['*']}
            source={{ html: generateMapHtml() }} 
            style={{ flex: 1 }}
          />
        )}
      </View>

      {/* Barra de Navegación Inferior */}
      <View style={styles.bottomNav}>
        <TouchableOpacity style={styles.navItemActive}><MaterialIcons name="map" size={24} color="#003d9b" /><Text style={styles.navTextActive}>Mapa</Text></TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/routes')}><MaterialIcons name="directions" size={24} color="#434654" /><Text style={styles.navText}>Rutas</Text></TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/report')}><MaterialIcons name="emergency" size={24} color="#434654" /><Text style={styles.navText}>Reportar</Text></TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/alerts')}><MaterialIcons name="notifications-active" size={24} color="#434654" /><Text style={styles.navText}>Alertas</Text></TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/profile')}><MaterialIcons name="person" size={24} color="#434654" /><Text style={styles.navText}>Perfil</Text></TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8f9fb', width: '100%', height: '100%' },
  desktopContainer: { 
    maxWidth: 480, 
    alignSelf: 'center', 
    ...Platform.select({
      web: { boxShadow: '0px 4px 12px rgba(0, 0, 0, 0.1)' },
      default: { 
        shadowColor: '#000', 
        shadowOffset: { width: 0, height: 4 }, 
        shadowOpacity: 0.1, 
        shadowRadius: 12, 
        elevation: 8 
      }
    })
  },
  header: { position: 'absolute', top: 0, width: '100%', zIndex: 50, backgroundColor: 'rgba(248, 249, 251, 0.95)', borderBottomWidth: 1, borderBottomColor: '#e7e8ea' },
  headerContent: { height: 70, paddingHorizontal: 20, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingTop: 15 },
  headerTitle: { fontSize: 14, fontWeight: '800', color: '#191c1e' },
  routeRedirectBtn: { backgroundColor: '#003d9b', paddingHorizontal: 10, paddingVertical: 6, borderRadius: 12, flexDirection: 'row', alignItems: 'center', gap: 4 },
  routeRedirectText: { color: '#fff', fontSize: 11, fontWeight: '700' },
  filterContainer: { position: 'absolute', top: 75, width: '100%', zIndex: 40, paddingVertical: 6, backgroundColor: 'rgba(255,255,255,0.9)' },
  filterChip: { backgroundColor: '#f1f5f9', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 16, borderWidth: 1, borderColor: '#cbd5e1' },
  filterChipActive: { backgroundColor: '#003d9b', borderColor: '#003d9b' },
  filterText: { fontSize: 11, fontWeight: '700', color: '#334155' },
  filterTextActive: { color: '#ffffff' },
  mapContainer: { flex: 1, marginTop: 120, marginBottom: 80, position: 'relative' },
  bottomNav: { position: 'absolute', bottom: 0, width: '100%', height: 80, backgroundColor: 'rgba(248, 249, 251, 0.95)', borderTopWidth: 1, borderTopColor: '#e7e8ea', flexDirection: 'row', justifyContent: 'space-around', alignItems: 'center', zIndex: 50 },
  navItem: { flex: 1, height: '100%', justifyContent: 'center', alignItems: 'center', gap: 2 },
  navItemActive: { flex: 1, height: '100%', justifyContent: 'center', alignItems: 'center', gap: 2, backgroundColor: 'rgba(0, 61, 155, 0.04)' },
  navText: { fontSize: 11, fontWeight: '500', color: '#434654' },
  navTextActive: { fontSize: 11, fontWeight: '700', color: '#003d9b' }
});