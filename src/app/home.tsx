import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, Image, TouchableOpacity, ScrollView, Platform, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';
import * as Location from 'expo-location';
import { useApp } from '../context/AppContext';

export default function HomeScreen() {
  const router = useRouter();
  const { setUserLocation, setEmergencyMode } = useApp();
  
  const [coordinates, setCoordinates] = useState({ lat: -33.5951, lon: -70.7022 }); // San Bernardo por defecto
  const [locationLoading, setLocationLoading] = useState(true);

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
      }
    } catch (error) {
      console.log("No se pudo obtener la ubicación GPS, usando valores por defecto.");
    } finally {
      setLocationLoading(false);
    }
  };

  useEffect(() => {
    fetchCurrentLocation();
  }, []);

  const osmEmbedUrl = `https://www.openstreetmap.org/export/embed.html?bbox=${coordinates.lon - 0.03}%2C${coordinates.lat - 0.03}%2C${coordinates.lon + 0.03}%2C${coordinates.lat + 0.03}&layer=mapnik&marker=${coordinates.lat}%2C${coordinates.lon}`;

  return (
    <View style={styles.container}>
      {/* Header Fijo */}
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <View style={styles.headerLeft}>
            <Image 
              source={{ uri: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBPeJPPNaZjLUmbwSJqyfMA1_Ghrtqg9A7RFV7oJcIwbCMZ-pdFN7R3xvXbBW58RxPMRVNW3ndQeZvPGO2k1XV6AXoPDgCgGU8MMFmkIsGOWzM9eIbk6wSEIi2RM9eltr98r1D29La3wH4vRayAQw4CnkA-75bO1Pnqxz-ysZskIVhEBRL8rFWsuHXPEUErp6pPnfvFmgog-jR_ZIWG5bVwNJehGEe052_VpQflnn-3MufZNQqMr0Kb' }} 
              style={styles.logo}
            />
            <View>
              <Text style={styles.headerTitle}>Map</Text>
              <View style={styles.statusRow}>
                <View style={styles.statusIndicator}>
                  <View style={styles.greenDot} />
                  <Text style={styles.statusText}>Online</Text>
                </View>
                <View style={styles.separatorDot} />
                <View style={styles.statusIndicator}>
                  <View style={styles.greenDot} />
                  <Text style={styles.statusText}>GPS Active</Text>
                </View>
              </View>
            </View>
          </View>
          <View style={styles.headerRight}>
            <TouchableOpacity style={styles.iconButton}>
              <MaterialIcons name="search" size={24} color="#434654" />
            </TouchableOpacity>
            <Image 
              source={{ uri: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDns8qYCKSgp85aOpatOFEMNryQMJCYVa5TkUD7cG42pj_w5-v3YAcquBmFSVJVc0QL0Lflnto8vcta5mexFvHvYee5gW14AqknSDf201KqiVJ5QHYqO9b18Z-5rz_nCM6uWrOAYEsP57-iAk--EFVA89RJO3IBjM50RXVn-Q8Vw860qKNzr3kBdmMHmsh2j_cKVVqN9bZAvz3D4_1b-Eu-zu0RBfuT6hr_aBR4BnAj_3Fu0WYucDuh' }} 
              style={styles.profileAvatar}
            />
          </View>
        </View>
      </View>

      {/* Contenido Principal / Mapa */}
      <View style={styles.mapContainer}>
        {/* Filtros Superiores flotantes */}
        <View style={styles.floatingFiltersContainer}>
          <View style={styles.infoCard}>
            <View style={styles.infoCardLeft}>
              <View style={styles.pulsingDot} />
              <Text style={styles.infoCardTitle}>Información actualizada</Text>
            </View>
            <View style={styles.infoCardRight}>
              <MaterialIcons name="satellite-alt" size={16} color="#434654" />
              <Text style={styles.infoCardSubText}>GPS: Activo</Text>
            </View>
          </View>

          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filterScroll}>
            <TouchableOpacity style={styles.filterChip}>
              <MaterialIcons name="local-hospital" size={16} color="#003d9b" />
              <Text style={styles.filterText}>Hospitales</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.filterChip}>
              <MaterialIcons name="local-fire-department" size={16} color="#8c000e" />
              <Text style={styles.filterText}>Bomberos</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.filterChip}>
              <MaterialIcons name="local-police" size={16} color="#003d9b" />
              <Text style={styles.filterText}>Policía</Text>
            </TouchableOpacity>
            <TouchableOpacity style={[styles.filterChip, styles.filterChipActive]}>
              <MaterialIcons name="verified" size={16} color="#ffffff" />
              <Text style={styles.filterTextActive}>Zonas Seguras</Text>
            </TouchableOpacity>
          </ScrollView>
        </View>

        {/* Botones laterales derechos (Ahora con funcionalidad de actualizar ubicación) */}
        <View style={styles.mapControls}>
          <TouchableOpacity 
            style={styles.mapControlButton} 
            onPress={fetchCurrentLocation}
            activeOpacity={0.8}
          >
            {locationLoading ? (
              <ActivityIndicator size="small" color="#003d9b" />
            ) : (
              <MaterialIcons name="my-location" size={22} color="#434654" />
            )}
          </TouchableOpacity>
          <TouchableOpacity style={styles.mapControlButton}>
            <MaterialIcons name="layers" size={22} color="#434654" />
          </TouchableOpacity>
        </View>

        {/* OpenStreetMap Iframe en Web con recarga dinámica por coordenadas */}
        {Platform.OS === 'web' ? (
          // @ts-ignore
          <iframe
            key={`${coordinates.lat}-${coordinates.lon}`}
            src={osmEmbedUrl}
            style={{ width: '100%', height: '100%', border: 0, position: 'absolute', zIndex: 0 }}
            title="OpenStreetMap"
          />
        ) : (
          <View style={[StyleSheet.absoluteFillObject, { backgroundColor: '#e1e2e4' }]} />
        )}

        {/* Bottom Sheet Inferior */}
        <View style={styles.bottomSheet}>
          <View style={styles.bottomSheetIndicator} />
          <View style={styles.bottomSheetContent}>
            <View>
              <Text style={styles.bottomSheetTitle}>¿Necesitas evacuar?</Text>
              <Text style={styles.bottomSheetSubtitle}>
                <MaterialIcons name="update" size={14} /> Lat: {coordinates.lat.toFixed(4)}, Lon: {coordinates.lon.toFixed(4)}
              </Text>
            </View>
            <TouchableOpacity 
              style={styles.primaryButton}
              onPress={() => {
                setEmergencyMode(true);
                router.push('/emergency-mode' as any);
              }}
            >
              <MaterialIcons name="directions-run" size={20} color="#ffffff" />
              <Text style={styles.primaryButtonText}>Encontrar ruta segura</Text>
            </TouchableOpacity>

            <View style={styles.gridActionRow}>
              <TouchableOpacity style={styles.gridActionCard}>
                <MaterialIcons name="emergency-share" size={28} color="#434654" />
                <Text style={styles.gridActionText}>Reportar</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.gridActionCard}>
                <MaterialIcons name="family-restroom" size={28} color="#434654" />
                <Text style={styles.gridActionText}>Mi Grupo</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </View>

      {/* Barra de Navegación Inferior (Tabs) */}
      <View style={styles.bottomNav}>
        <TouchableOpacity style={styles.navItemActive} onPress={() => router.push('/home')}>
          <MaterialIcons name="map" size={24} color="#003d9b" />
          <Text style={styles.navTextActive}>Map</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/explore')}>
          <MaterialIcons name="directions" size={24} color="#434654" />
          <Text style={styles.navText}>Routes</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/report' as any)}>
          <MaterialIcons name="emergency" size={24} color="#434654" />
          <Text style={styles.navText}>Report</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/alerts' as any)}>
          <MaterialIcons name="notifications" size={24} color="#434654" />
          <Text style={styles.navText}>Alerts</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/profile' as any)}>
          <MaterialIcons name="person" size={24} color="#434654" />
          <Text style={styles.navText}>Profile</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8f9fb' },
  header: { position: 'absolute', top: 0, width: '100%', zIndex: 50, backgroundColor: 'rgba(248, 249, 251, 0.9)', borderBottomWidth: 1, borderBottomColor: '#e7e8ea' },
  headerContent: { height: 80, paddingHorizontal: 20, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingTop: 15 },
  headerLeft: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  logo: { width: 32, height: 32, resizeMode: 'contain' },
  headerTitle: { fontSize: 18, fontWeight: '700', color: '#191c1e', lineHeight: 18 },
  statusRow: { flexDirection: 'row', alignItems: 'center', marginTop: 4, gap: 6 },
  statusIndicator: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  greenDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: '#22c55e' },
  separatorDot: { width: 4, height: 4, borderRadius: 2, backgroundColor: '#c3c6d6' },
  statusText: { fontSize: 10, fontWeight: '700', color: '#434654', textTransform: 'uppercase' },
  headerRight: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  iconButton: { width: 48, height: 48, justifyContent: 'center', alignItems: 'center' },
  profileAvatar: { width: 40, height: 40, borderRadius: 20, borderWidth: 2, borderColor: '#0052cc' },
  mapContainer: { flex: 1, marginTop: 80, marginBottom: 80, position: 'relative', backgroundColor: '#e1e2e4' },
  floatingFiltersContainer: { position: 'absolute', top: 16, left: 16, right: 16, zIndex: 10, gap: 8 },
  infoCard: { backgroundColor: 'rgba(248, 249, 251, 0.95)', borderRadius: 12, padding: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 4, elevation: 3 },
  infoCardLeft: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  pulsingDot: { width: 10, height: 10, borderRadius: 5, backgroundColor: '#22c55e' },
  infoCardTitle: { fontSize: 14, fontWeight: '600', color: '#191c1e' },
  infoCardRight: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  infoCardSubText: { fontSize: 12, color: '#434654', fontWeight: '500' },
  filterScroll: { gap: 8, paddingBottom: 4 },
  filterChip: { flexDirection: 'row', alignItems: 'center', gap: 6, backgroundColor: '#ffffff', paddingHorizontal: 12, paddingVertical: 8, borderRadius: 20, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.1, shadowRadius: 2, elevation: 2 },
  filterChipActive: { backgroundColor: '#003d9b' },
  filterText: { fontSize: 12, fontWeight: '600', color: '#191c1e' },
  filterTextActive: { fontSize: 12, fontWeight: '600', color: '#ffffff' },
  mapControls: { position: 'absolute', top: 110, right: 16, zIndex: 10, gap: 8 },
  mapControlButton: { width: 48, height: 48, backgroundColor: '#ffffff', borderRadius: 24, justifyContent: 'center', alignItems: 'center', shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.15, shadowRadius: 4, elevation: 4 },
  bottomSheet: { position: 'absolute', bottom: 0, width: '100%', backgroundColor: '#ffffff', borderTopLeftRadius: 32, borderTopRightRadius: 32, paddingTop: 8, paddingBottom: 24, paddingHorizontal: 20, shadowColor: '#000', shadowOffset: { width: 0, height: -8 }, shadowOpacity: 0.15, shadowRadius: 16, elevation: 12, zIndex: 20 },
  bottomSheetIndicator: { width: 48, height: 6, backgroundColor: '#c3c6d6', borderRadius: 3, alignSelf: 'center', marginBottom: 12 },
  bottomSheetContent: { gap: 14 },
  bottomSheetTitle: { fontSize: 20, fontWeight: '700', color: '#191c1e' },
  bottomSheetSubtitle: { fontSize: 13, color: '#434654', marginTop: 2 },
  primaryButton: { backgroundColor: '#003d9b', height: 52, borderRadius: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, shadowColor: '#003d9b', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 8, elevation: 5 },
  primaryButtonText: { color: '#ffffff', fontSize: 14, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 0.5 },
  gridActionRow: { flexDirection: 'row', gap: 12, marginTop: 4 },
  gridActionCard: { flex: 1, backgroundColor: '#f3f4f6', borderRadius: 12, padding: 12, alignItems: 'center', justifyContent: 'center', gap: 6 },
  gridActionText: { fontSize: 12, fontWeight: '600', color: '#191c1e' },
  bottomNav: { position: 'absolute', bottom: 0, width: '100%', height: 80, backgroundColor: 'rgba(248, 249, 251, 0.95)', borderTopWidth: 1, borderTopColor: '#e7e8ea', flexDirection: 'row', justifyContent: 'space-around', alignItems: 'center', zIndex: 50 },
  navItem: { flex: 1, height: '100%', justifyContent: 'center', alignItems: 'center', gap: 2 },
  navItemActive: { flex: 1, height: '100%', justifyContent: 'center', alignItems: 'center', gap: 2 },
  navText: { fontSize: 11, fontWeight: '500', color: '#434654' },
  navTextActive: { fontSize: 11, fontWeight: '700', color: '#003d9b' }
});