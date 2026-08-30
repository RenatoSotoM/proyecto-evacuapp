import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, Image, TouchableOpacity, ScrollView, Platform, ActivityIndicator, Modal, TextInput } from 'react-native';
import { useRouter } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';
import * as Location from 'expo-location';
import { WebView } from 'react-native-webview';
import { useApp } from '../context/AppContext';

const FALLBACK_POINTS = [
  { id: '1', name: 'Hospital Parroquial de San Bernardo', category: 'hospital', lat: -33.5920, lon: -70.7020, address: 'Av Colón Sur, San Bernardo' },
  { id: '2', name: 'Cuerpo de Bomberos San Bernardo', category: 'bomberos', lat: -33.5880, lon: -70.7000, address: 'Covadonga 459, San Bernardo' },
  { id: '3', name: '14 Comisaría de San Bernardo', category: 'policia', lat: -33.5900, lon: -70.7010, address: "O'Higgins 326, San Bernardo" },
  { id: '4', name: 'Cerro de Chena (Zona Segura)', category: 'zonas', lat: -33.5700, lon: -70.7200, address: 'Cerro de Chena, San Bernardo' }
];

export default function HomeScreen() {
  const router = useRouter();
  const { setUserLocation, setEmergencyMode } = useApp();
  
  const [coordinates, setCoordinates] = useState({ lat: -33.5951, lon: -70.7022 });
  const [locationLoading, setLocationLoading] = useState(true);
  const [activeFilter, setActiveFilter] = useState<'hospital' | 'bomberos' | 'policia' | 'zonas' | null>('zonas');
  const [mapLayer, setMapLayer] = useState<'standard' | 'relief'>('relief'); // Predeterminado en relieve para autos
  const [emergencyPoints, setEmergencyPoints] = useState(FALLBACK_POINTS);
  const [loadingPoints, setLoadingPoints] = useState(false);

  // Estados de Configuración Inicial (Onboarding)
  const [showOnboarding, setShowOnboarding] = useState(true);
  const [userName, setUserName] = useState('');
  const [mobilityType, setMobilityType] = useState<'pie' | 'vehiculo'>('vehiculo');
  const [isAccompanied, setIsAccompanied] = useState(false);

  const fetchNearbyEmergencyPoints = async (lat: number, lon: number) => {
    try {
      setLoadingPoints(true);
      const radius = 15000;
      const query = `
        [out:json];
        (
          node["amenity"="hospital"](around:${radius},${lat},${lon});
          way["amenity"="hospital"](around:${radius},${lat},${lon});
          node["amenity"="fire_station"](around:${radius},${lat},${lon});
          way["amenity"="fire_station"](around:${radius},${lat},${lon});
          node["amenity"="police"](around:${radius},${lat},${lon});
          way["amenity"="police"](around:${radius},${lat},${lon});
          node["natural"="peak"](around:${radius},${lat},${lon});
          way["natural"="peak"](around:${radius},${lat},${lon});
        );
        out body;
      `;

      const response = await fetch('https://overpass-api.de/api/interpreter', {
        method: 'POST',
        body: query,
      });

      const data = await response.json();

      if (data && data.elements && data.elements.length > 0) {
        const parsedPoints = data.elements
          .filter((el: any) => el.lat && el.lon)
          .map((el: any, index: number) => {
            let cat = 'zonas';
            if (el.tags?.amenity === 'hospital') cat = 'hospital';
            else if (el.tags?.amenity === 'fire_station') cat = 'bomberos';
            else if (el.tags?.amenity === 'police') cat = 'policia';
            else if (el.tags?.natural === 'peak') cat = 'zonas';

            return {
              id: el.id?.toString() || index.toString(),
              name: el.tags?.name || (cat === 'zonas' ? 'Cerro / Zona Segura' : 'Punto de Emergencia'),
              category: cat,
              lat: el.lat,
              lon: el.lon,
              address: el.tags?.['addr:street'] ? `${el.tags['addr:street']} ${el.tags['addr:housenumber'] || ''}` : 'Radio 15 km'
            };
          });

        if (parsedPoints.length > 0) {
          setEmergencyPoints(parsedPoints);
        }
      }
    } catch (error) {
      console.log("Error consultando Overpass API, usando respaldo.");
    } finally {
      setLoadingPoints(false);
    }
  };

  const fetchCurrentLocation = async () => {
    try {
      setLocationLoading(true);
      let { status } = await Location.requestForegroundPermissionsAsync();
      if (status === 'granted') {
        let location = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High });
        const coords = {
          lat: location.coords.latitude,
          lon: location.coords.longitude,
        };
        setCoordinates(coords);
        setUserLocation({ latitude: coords.lat, longitude: coords.lon });
        await fetchNearbyEmergencyPoints(coords.lat, coords.lon);
      } else {
        await fetchNearbyEmergencyPoints(coordinates.lat, coordinates.lon);
      }
    } catch (error) {
      await fetchNearbyEmergencyPoints(coordinates.lat, coordinates.lon);
    } finally {
      setLocationLoading(false);
    }
  };

  useEffect(() => {
    fetchCurrentLocation();
  }, []);

  // Filtrado opcional (permite desactivar filtros para ver ubicación limpia)
  const filteredPoints = activeFilter 
    ? emergencyPoints.filter(p => p.category === activeFilter)
    : [];

  const selectedPoint = filteredPoints[0] || { 
    name: userName ? `Ubicación de ${userName}` : 'Mi Ubicación GPS', 
    lat: coordinates.lat, 
    lon: coordinates.lon, 
    address: mobilityType === 'vehiculo' ? 'Modo Vehículo (Relieve Activo)' : 'Modo A pie' 
  };

  // Capas de OpenStreetMap: relieve (cyclemap/outdoors style) o estándar
  const tileLayer = mapLayer === 'relief' ? 'https://tile.thunderforest.com/outdoors/{z}/{x}/{y}.png?apikey=6170aad10d194bb587a81e3a612513f5' : 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
  const osmEmbedUrl = Platform.OS === 'web' 
    ? `https://www.openstreetmap.org/export/embed.html?bbox=${selectedPoint.lon - 0.04}%2C${selectedPoint.lat - 0.04}%2C${selectedPoint.lon + 0.04}%2C${selectedPoint.lat + 0.04}&layer=mapnik&marker=${selectedPoint.lat}%2C${selectedPoint.lon}`
    : `https://www.openstreetmap.org/?mlat=${selectedPoint.lat}&mlon=${selectedPoint.lon}#map=14/${selectedPoint.lat}/${selectedPoint.lon}`;

  return (
    <View style={styles.container}>
      {/* Modal de Configuración Inicial (Onboarding al ingresar) */}
      <Modal visible={showOnboarding} animationType="slide" transparent={true}>
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Configuración de Emergencia</Text>
            <Text style={styles.modalSubtitle}>Ingresa tus datos para personalizar tu ruta y evacuación.</Text>

            <Text style={styles.label}>Tu Nombre:</Text>
            <TextInput 
              style={styles.input} 
              placeholder="Ej. Juan Pérez" 
              placeholderTextColor="#999"
              value={userName}
              onChangeText={setUserName}
            />

            <Text style={styles.label}>Tipo de Movilidad:</Text>
            <View style={styles.rowOptions}>
              <TouchableOpacity 
                style={[styles.optionButton, mobilityType === 'vehiculo' && styles.optionActive]}
                onPress={() => setMobilityType('vehiculo')}
              >
                <MaterialIcons name="directions-car" size={20} color={mobilityType === 'vehiculo' ? '#fff' : '#003d9b'} />
                <Text style={[styles.optionText, mobilityType === 'vehiculo' && styles.optionTextActive]}>Vehículo (Relieve)</Text>
              </TouchableOpacity>
              <TouchableOpacity 
                style={[styles.optionButton, mobilityType === 'pie' && styles.optionActive]}
                onPress={() => setMobilityType('pie')}
              >
                <MaterialIcons name="directions-walk" size={20} color={mobilityType === 'pie' ? '#fff' : '#003d9b'} />
                <Text style={[styles.optionText, mobilityType === 'pie' && styles.optionTextActive]}>A pie</Text>
              </TouchableOpacity>
            </View>

            <Text style={styles.label}>¿Vas acompañado?</Text>
            <View style={styles.rowOptions}>
              <TouchableOpacity 
                style={[styles.optionButton, !isAccompanied && styles.optionActive]}
                onPress={() => setIsAccompanied(false)}
              >
                <MaterialIcons name="person" size={20} color={!isAccompanied ? '#fff' : '#003d9b'} />
                <Text style={[styles.optionText, !isAccompanied && styles.optionTextActive]}>Solo/a</Text>
              </TouchableOpacity>
              <TouchableOpacity 
                style={[styles.optionButton, isAccompanied && styles.optionActive]}
                onPress={() => setIsAccompanied(true)}
              >
                <MaterialIcons name="group" size={20} color={isAccompanied ? '#fff' : '#003d9b'} />
                <Text style={[styles.optionText, isAccompanied && styles.optionTextActive]}>Acompañado/a</Text>
              </TouchableOpacity>
            </View>

            <TouchableOpacity 
              style={styles.startButton} 
              onPress={() => {
                if (userName.trim()) setShowOnboarding(false);
              }}
            >
              <Text style={styles.startButtonText}>Comenzar Monitoreo</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* Header Fijo */}
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <View style={styles.headerLeft}>
            <View>
              <Text style={styles.headerTitle}>Mapa Seguro ({mobilityType === 'vehiculo' ? 'Auto / Relieve' : 'A pie'})</Text>
              <View style={styles.statusRow}>
                <View style={styles.statusIndicator}>
                  <View style={styles.greenDot} />
                  <Text style={styles.statusText}>{userName || 'Usuario'}</Text>
                </View>
                <View style={styles.separatorDot} />
                <View style={styles.statusIndicator}>
                  <View style={styles.greenDot} />
                  <Text style={styles.statusText}>{isAccompanied ? 'Acompañado' : 'Solo'}</Text>
                </View>
              </View>
            </View>
          </View>
          <View style={styles.headerRight}>
            <TouchableOpacity 
              style={[styles.badgeToggle, activeFilter === null && styles.badgeToggleActive]}
              onPress={() => setActiveFilter(null)}
            >
              <MaterialIcons name="location-searching" size={16} color={activeFilter === null ? '#fff' : '#003d9b'} />
              <Text style={[styles.badgeToggleText, activeFilter === null && styles.badgeToggleTextActive]}>Ver Mi Ubicación</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>

      {/* Contenido Principal / Mapa */}
      <View style={styles.mapContainer}>
        {/* Filtros Superiores con opción de desactivar */}
        <View style={styles.floatingFiltersContainer}>
          <View style={styles.infoCard}>
            <View style={styles.infoCardLeft}>
              <View style={styles.pulsingDot} />
              <Text style={styles.infoCardTitle}>
                {activeFilter ? `Filtro: ${activeFilter.toUpperCase()} (${filteredPoints.length})` : "Filtros Desactivados (Mostrando tu posición)"}
              </Text>
            </View>
            {activeFilter && (
              <TouchableOpacity onPress={() => setActiveFilter(null)}>
                <Text style={styles.clearFilterText}>Desactivar filtros</Text>
              </TouchableOpacity>
            )}
          </View>

          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filterScroll}>
            <TouchableOpacity 
              style={[styles.filterChip, activeFilter === 'hospital' && styles.filterChipActive]}
              onPress={() => setActiveFilter(activeFilter === 'hospital' ? null : 'hospital')}
            >
              <MaterialIcons name="local-hospital" size={16} color={activeFilter === 'hospital' ? '#fff' : '#003d9b'} />
              <Text style={[styles.filterText, activeFilter === 'hospital' && styles.filterTextActive]}>Hospitales</Text>
            </TouchableOpacity>

            <TouchableOpacity 
              style={[styles.filterChip, activeFilter === 'bomberos' && styles.filterChipActive]}
              onPress={() => setActiveFilter(activeFilter === 'bomberos' ? null : 'bomberos')}
            >
              <MaterialIcons name="local-fire-department" size={16} color={activeFilter === 'bomberos' ? '#fff' : '#8c000e'} />
              <Text style={[styles.filterText, activeFilter === 'bomberos' && styles.filterTextActive]}>Bomberos</Text>
            </TouchableOpacity>

            <TouchableOpacity 
              style={[styles.filterChip, activeFilter === 'policia' && styles.filterChipActive]}
              onPress={() => setActiveFilter(activeFilter === 'policia' ? null : 'policia')}
            >
              <MaterialIcons name="local-police" size={16} color={activeFilter === 'policia' ? '#fff' : '#003d9b'} />
              <Text style={[styles.filterText, activeFilter === 'policia' && styles.filterTextActive]}>Policía</Text>
            </TouchableOpacity>

            <TouchableOpacity 
              style={[styles.filterChip, activeFilter === 'zonas' && styles.filterChipActive]}
              onPress={() => setActiveFilter(activeFilter === 'zonas' ? null : 'zonas')}
            >
              <MaterialIcons name="terrain" size={16} color={activeFilter === 'zonas' ? '#fff' : '#22c55e'} />
              <Text style={[styles.filterText, activeFilter === 'zonas' && styles.filterTextActive]}>Zonas Seguras / Cerros</Text>
            </TouchableOpacity>
          </ScrollView>
        </View>

        {/* Botón flotante para marcar y centrar donde estoy yo + Relieve de Auto */}
        <View style={styles.mapControls}>
          <TouchableOpacity 
            style={styles.mapControlButton} 
            onPress={fetchCurrentLocation}
            activeOpacity={0.8}
          >
            {locationLoading ? (
              <ActivityIndicator size="small" color="#003d9b" />
            ) : (
              <MaterialIcons name="my-location" size={22} color="#003d9b" />
            )}
          </TouchableOpacity>
          <TouchableOpacity 
            style={[styles.mapControlButton, mapLayer === 'relief' && styles.mapControlButtonActive]} 
            onPress={() => setMapLayer(mapLayer === 'relief' ? 'standard' : 'relief')}
          >
            <MaterialIcons name="terrain" size={22} color={mapLayer === 'relief' ? '#fff' : '#434654'} />
          </TouchableOpacity>
        </View>

        {/* Renderizado de Mapa */}
        {Platform.OS === 'web' ? (
          // @ts-ignore
          <iframe
            key={`${selectedPoint.lat}-${selectedPoint.lon}-${mapLayer}`}
            src={osmEmbedUrl}
            style={{ width: '100%', height: '100%', border: 0, position: 'absolute', zIndex: 0 }}
            title="OpenStreetMap"
          />
        ) : (
          <WebView
            key={`${selectedPoint.lat}-${selectedPoint.lon}-${mapLayer}`}
            source={{ uri: osmEmbedUrl }}
            style={{ flex: 1, position: 'absolute', width: '100%', height: '100%', zIndex: 0 }}
          />
        )}

        {/* Bottom Sheet Informativo */}
        <View style={styles.bottomSheet}>
          <View style={styles.bottomSheetIndicator} />
          <View style={styles.bottomSheetContent}>
            <View>
              <Text style={styles.bottomSheetTitle}>{selectedPoint.name}</Text>
              <Text style={styles.bottomSheetSubtitle}>
                <MaterialIcons name="place" size={14} color="#003d9b" /> {selectedPoint.address}
              </Text>
            </View>
            <TouchableOpacity 
              style={styles.primaryButton}
              onPress={() => {
                setEmergencyMode(true);
                router.push('/emergency-mode' as any);
              }}
            >
              <MaterialIcons name={mobilityType === 'vehiculo' ? 'directions-car' : 'directions-walk'} size={20} color="#ffffff" />
              <Text style={styles.primaryButtonText}>Iniciar Ruta ({mobilityType === 'vehiculo' ? 'Vehículo' : 'A pie'})</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>

      {/* Navegación Inferior */}
      <View style={styles.bottomNav}>
        <TouchableOpacity style={styles.navItemActive} onPress={() => router.push('/home')}>
          <MaterialIcons name="map" size={24} color="#003d9b" />
          <Text style={styles.navTextActive}>Mapa</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/explore')}>
          <MaterialIcons name="directions" size={24} color="#434654" />
          <Text style={styles.navText}>Rutas</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/report' as any)}>
          <MaterialIcons name="emergency" size={24} color="#434654" />
          <Text style={styles.navText}>Reportes</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/alerts' as any)}>
          <MaterialIcons name="notifications" size={24} color="#434654" />
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

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8f9fb' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.6)', justifyContent: 'center', padding: 20, zIndex: 100 },
  modalContent: { backgroundColor: '#fff', borderRadius: 20, padding: 24, gap: 12, shadowColor: '#000', shadowOpacity: 0.2, elevation: 10 },
  modalTitle: { fontSize: 20, fontWeight: '700', color: '#191c1e', textAlign: 'center' },
  modalSubtitle: { fontSize: 13, color: '#434654', textAlign: 'center', marginBottom: 8 },
  label: { fontSize: 13, fontWeight: '600', color: '#191c1e', marginTop: 4 },
  input: { borderWidth: 1, borderColor: '#e1e2e4', borderRadius: 10, padding: 12, fontSize: 14, backgroundColor: '#f9f9fb' },
  rowOptions: { flexDirection: 'row', gap: 10 },
  optionButton: { flex: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 6, borderWidth: 1, borderColor: '#003d9b', padding: 10, borderRadius: 10 },
  optionActive: { backgroundColor: '#003d9b' },
  optionText: { fontSize: 12, fontWeight: '600', color: '#003d9b' },
  optionTextActive: { color: '#fff' },
  startButton: { backgroundColor: '#003d9b', padding: 14, borderRadius: 10, alignItems: 'center', marginTop: 10 },
  startButtonText: { color: '#fff', fontWeight: '700', fontSize: 14, textTransform: 'uppercase' },
  header: { position: 'absolute', top: 0, width: '100%', zIndex: 50, backgroundColor: 'rgba(248, 249, 251, 0.95)', borderBottomWidth: 1, borderBottomColor: '#e7e8ea' },
  headerContent: { height: 75, paddingHorizontal: 20, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingTop: 15 },
  headerLeft: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  headerTitle: { fontSize: 15, fontWeight: '700', color: '#191c1e' },
  statusRow: { flexDirection: 'row', alignItems: 'center', marginTop: 2, gap: 6 },
  statusIndicator: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  greenDot: { width: 7, height: 7, borderRadius: 3.5, backgroundColor: '#22c55e' },
  separatorDot: { width: 3, height: 3, borderRadius: 1.5, backgroundColor: '#c3c6d6' },
  statusText: { fontSize: 10, fontWeight: '600', color: '#434654', textTransform: 'uppercase' },
  headerRight: { flexDirection: 'row', alignItems: 'center' },
  badgeToggle: { flexDirection: 'row', alignItems: 'center', gap: 4, backgroundColor: '#eef2ff', paddingHorizontal: 10, paddingVertical: 6, borderRadius: 15, borderWidth: 1, borderColor: '#c7d2fe' },
  badgeToggleActive: { backgroundColor: '#003d9b' },
  badgeToggleText: { fontSize: 11, fontWeight: '700', color: '#003d9b' },
  badgeToggleTextActive: { color: '#fff' },
  mapContainer: { flex: 1, marginTop: 75, marginBottom: 80, position: 'relative', backgroundColor: '#e1e2e4' },
  floatingFiltersContainer: { position: 'absolute', top: 12, left: 16, right: 16, zIndex: 10, gap: 8 },
  infoCard: { backgroundColor: 'rgba(248, 249, 251, 0.95)', borderRadius: 12, padding: 10, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', shadowColor: '#000', shadowOpacity: 0.1, elevation: 3 },
  infoCardLeft: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  pulsingDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: '#22c55e' },
  infoCardTitle: { fontSize: 12, fontWeight: '600', color: '#191c1e' },
  clearFilterText: { fontSize: 11, fontWeight: '700', color: '#8c000e' },
  filterScroll: { gap: 8, paddingBottom: 4 },
  filterChip: { flexDirection: 'row', alignItems: 'center', gap: 6, backgroundColor: '#ffffff', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 20, elevation: 2 },
  filterChipActive: { backgroundColor: '#003d9b' },
  filterText: { fontSize: 11, fontWeight: '600', color: '#191c1e' },
  filterTextActive: { color: '#ffffff' },
  mapControls: { position: 'absolute', top: 105, right: 16, zIndex: 10, gap: 8 },
  mapControlButton: { width: 44, height: 44, backgroundColor: '#ffffff', borderRadius: 22, justifyContent: 'center', alignItems: 'center', shadowColor: '#000', shadowOpacity: 0.15, elevation: 4 },
  mapControlButtonActive: { backgroundColor: '#003d9b' },
  bottomSheet: { position: 'absolute', bottom: 0, width: '100%', backgroundColor: '#ffffff', borderTopLeftRadius: 28, borderTopRightRadius: 28, paddingTop: 8, paddingBottom: 20, paddingHorizontal: 20, elevation: 12, zIndex: 20 },
  bottomSheetIndicator: { width: 40, height: 5, backgroundColor: '#c3c6d6', borderRadius: 2.5, alignSelf: 'center', marginBottom: 10 },
  bottomSheetContent: { gap: 12 },
  bottomSheetTitle: { fontSize: 16, fontWeight: '700', color: '#191c1e' },
  bottomSheetSubtitle: { fontSize: 12, color: '#434654', marginTop: 2 },
  primaryButton: { backgroundColor: '#003d9b', height: 48, borderRadius: 10, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8 },
  primaryButtonText: { color: '#ffffff', fontSize: 13, fontWeight: '700', textTransform: 'uppercase' },
  bottomNav: { position: 'absolute', bottom: 0, width: '100%', height: 80, backgroundColor: 'rgba(248, 249, 251, 0.95)', borderTopWidth: 1, borderTopColor: '#e7e8ea', flexDirection: 'row', justifyContent: 'space-around', alignItems: 'center', zIndex: 50 },
  navItem: { flex: 1, height: '100%', justifyContent: 'center', alignItems: 'center', gap: 2 },
  navItemActive: { flex: 1, height: '100%', justifyContent: 'center', alignItems: 'center', gap: 2 },
  navText: { fontSize: 11, fontWeight: '500', color: '#434654' },
  navTextActive: { fontSize: 11, fontWeight: '700', color: '#003d9b' }
});