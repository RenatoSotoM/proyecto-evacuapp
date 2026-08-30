import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, Platform, useWindowDimensions, ScrollView, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';
import { WebView } from 'react-native-webview';
import * as Location from 'expo-location';

export default function RoutesScreen() {
  const router = useRouter();
  const { width } = useWindowDimensions();
  const isDesktop = width > 768;

  const [userLocation, setUserLocation] = useState({ lat: -33.5937, lon: -70.7029 });
  const [loading, setLoading] = useState(false);
  const [selectedVariant, setSelectedVariant] = useState(0);

  const [routeVariants, setRouteVariants] = useState([
    { id: 0, title: 'Variante A: Directa Local', risk: 'Moderado', color: '#2563eb', destLat: -33.5937, destLon: -70.7029, description: 'Cargando ubicación...' },
    { id: 1, title: 'Variante B: Perimetral Segura', risk: 'Bajo', color: '#16a34a', destLat: -33.5937, destLon: -70.7029, description: 'Cargando ubicación...' },
    { id: 2, title: 'Variante C: Refugio Alternativo', risk: 'Mínimo', color: '#9333ea', destLat: -33.5937, destLon: -70.7029, description: 'Cargando ubicación...' }
  ]);

  const [routeDetails, setRouteDetails] = useState([
    { distance: 'Calculando...', time: '...', geometry: null, valid: true },
    { distance: 'Calculando...', time: '...', geometry: null, valid: true },
    { distance: 'Calculando...', time: '...', geometry: null, valid: true },
  ]);

  const calculateRealMetrics = async (lat: number, lon: number, variants: typeof routeVariants) => {
    setLoading(true);
    try {
      const updatedDetails = await Promise.all(
        variants.map(async (variant) => {
          const url = `https://router.project-osrm.org/route/v1/driving/${lon},${lat};${variant.destLon},${variant.destLat}?overview=full&geometries=geojson`;
          const res = await fetch(url);
          const data = await res.json();

          if (data.routes && data.routes.length > 0) {
            const route = data.routes[0];
            const distanceKm = route.distance / 1000;
            return {
              distance: distanceKm.toFixed(1) + ' km',
              time: Math.round(route.duration / 60) + ' min',
              geometry: route.geometry.coordinates,
              valid: distanceKm <= 25
            };
          }
          return { distance: 'N/A', time: 'N/A', geometry: null, valid: false };
        })
      );
      setRouteDetails(updatedDetails);
    } catch (error) {
      console.error('Error al calcular métricas:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleInitPosition = async () => {
    try {
      let { status } = await Location.requestForegroundPermissionsAsync();
      let currentLat = -33.5937;
      let currentLon = -70.7029;

      if (status === 'granted') {
        let location = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High });
        currentLat = location.coords.latitude;
        currentLon = location.coords.longitude;
      }

      setUserLocation({ lat: currentLat, lon: currentLon });

      const dynamicVariants = [
        {
          id: 0,
          title: 'Variante A: Directa Local',
          risk: 'Moderado',
          color: '#2563eb',
          destLat: currentLat + 0.012,
          destLon: currentLon + 0.008,
          description: 'Eje vial principal hacia la zona de seguridad urbana más próxima[cite: 1].'
        },
        {
          id: 1,
          title: 'Variante B: Perimetral Segura',
          risk: 'Bajo',
          color: '#16a34a',
          destLat: currentLat - 0.010,
          destLon: currentLon + 0.012,
          description: 'Ruta perimetral alternativa que evita congestión vehicular[cite: 1].'
        },
        {
          id: 2,
          title: 'Variante C: Refugio Alternativo',
          risk: 'Mínimo',
          color: '#9333ea',
          destLat: currentLat + 0.005,
          destLon: currentLon - 0.015,
          description: 'Trayecto de menor riesgo hacia un punto de encuentro secundario[cite: 1].'
        }
      ];

      setRouteVariants(dynamicVariants);
      await calculateRealMetrics(currentLat, currentLon, dynamicVariants);
    } catch (error) {
      setLoading(false);
      alert('Error al obtener la ubicación GPS.');
    }
  };

  useEffect(() => {
    handleInitPosition();
  }, []);

  const currentRoute = routeVariants[selectedVariant];
  const currentMetric = routeDetails[selectedVariant] || { distance: '...', time: '...' };

  const generateRouteMapHtml = () => {
    const coordsJson = currentMetric.geometry ? JSON.stringify(currentMetric.geometry.map(c => [c[1], c[0]])) : '[]';

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
          var map = L.map('map', { zoomControl: false }).setView([${userLocation.lat}, ${userLocation.lon}], 14);
          L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);

          L.marker([${userLocation.lat}, ${userLocation.lon}], {
            icon: L.divIcon({
              className: 'vehicle-marker',
              html: '<div style="font-size: 18px; background: #2563eb; color: white; border-radius: 50%; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; box-shadow: 0 0 10px rgba(37,99,235,0.8); border: 2px solid white;">🚗</div>',
              iconSize: [34, 34]
            })
          }).addTo(map).bindPopup('<b>Tu Ubicación GPS Actual</b>');

          L.marker([${currentRoute.destLat}, ${currentRoute.destLon}], {
            icon: L.divIcon({
              className: 'target-marker',
              html: '<div style="font-size: 18px; background: ${currentRoute.color}; color: white; border-radius: 50%; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; box-shadow: 0 0 10px rgba(0,0,0,0.3); border: 2px solid white;">🛡️</div>',
              iconSize: [34, 34]
            })
          }).addTo(map).bindPopup('<b>Zona de Seguridad Local</b>');

          var coords = ${coordsJson};
          if (coords.length > 0) {
            L.polyline(coords, { color: '#1e3a8a', weight: 8, opacity: 0.4, lineCap: 'round', lineJoin: 'round' }).addTo(map);
            var mainPolyline = L.polyline(coords, { color: '${currentRoute.color}', weight: 5, opacity: 0.9, lineCap: 'round', lineJoin: 'round' }).addTo(map);
            map.fitBounds(mainPolyline.getBounds(), {padding: [40, 40]});
          }
        </script>
      </body>
      </html>
    `;
  };

  return (
    <View style={[styles.container, isDesktop && styles.desktopContainer]}>
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <Text style={styles.headerTitle}>EvacuApp - Radio Local (Tesis)</Text>
          <TouchableOpacity style={styles.calcButton} onPress={handleInitPosition} disabled={loading}>
            {loading ? <ActivityIndicator size="small" color="#fff" /> : <><MaterialIcons name="radar" size={16} color="#fff" /><Text style={styles.calcButtonText}>Actualizar</Text></>}
          </TouchableOpacity>
        </View>
      </View>

      <View style={styles.variantsContainer}>
        <ScrollView 
          horizontal 
          showsHorizontalScrollIndicator={false} 
          contentContainerStyle={{ gap: 8, paddingHorizontal: 16, paddingRight: 48 }}
        >
          {routeVariants.map((variant, index) => (
            <TouchableOpacity
              key={variant.id}
              style={[styles.variantChip, selectedVariant === index && { backgroundColor: variant.color, borderColor: variant.color }]}
              onPress={() => setSelectedVariant(index)}
            >
              <Text style={[styles.variantChipText, selectedVariant === index && { color: '#fff' }]}>
                {variant.title} ({routeDetails[index]?.distance})
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      <View style={styles.infoCardContainer}>
        <View style={styles.infoCard}>
          <View style={{ flex: 1 }}>
            <View style={styles.infoRowTop}>
              <Text style={styles.infoTitle}>{currentRoute.title}</Text>
              <Text style={[styles.riskBadge, { color: currentRoute.color }]}>Riesgo: {currentRoute.risk}</Text>
            </View>
            <Text style={styles.infoDesc}>{currentRoute.description}</Text>
          </View>
          <View style={styles.metricsBox}>
            <Text style={styles.metricDist}>{currentMetric.distance}</Text>
            <Text style={styles.metricTime}>{currentMetric.time}</Text>
          </View>
        </View>
      </View>

      <View style={styles.mapContainer}>
        {Platform.OS === 'web' ? (
          // @ts-ignore
          <iframe key={`${userLocation.lat}-${userLocation.lon}-${selectedVariant}`} srcDoc={generateRouteMapHtml()} style={{ width: '100%', height: '100%', border: 0 }} title="Mapa Local" />
        ) : (
          <WebView key={`${userLocation.lat}-${userLocation.lon}-${selectedVariant}`} originWhitelist={['*']} source={{ html: generateRouteMapHtml() }} style={{ flex: 1 }} />
        )}
      </View>

      <View style={styles.bottomNav}>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/map')}><MaterialIcons name="map" size={24} color="#434654" /><Text style={styles.navText}>Mapa</Text></TouchableOpacity>
        <TouchableOpacity style={styles.navItemActive}><MaterialIcons name="directions-car" size={24} color="#003d9b" /><Text style={styles.navTextActive}>Rutas</Text></TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/report')}><MaterialIcons name="emergency" size={24} color="#434654" /><Text style={styles.navText}>Reportar</Text></TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/alerts')}><MaterialIcons name="notifications-active" size={24} color="#434654" /><Text style={styles.navText}>Alertas</Text></TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/profile')}><MaterialIcons name="person" size={24} color="#434654" /><Text style={styles.navText}>Perfil</Text></TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8f9fb', width: '100%', height: '100%' },
  desktopContainer: { maxWidth: 480, alignSelf: 'center', ...Platform.select({ web: { boxShadow: '0px 4px 12px rgba(0, 0, 0, 0.1)' }, default: { shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.1, shadowRadius: 12, elevation: 8 } }) },
  header: { position: 'absolute', top: 0, width: '100%', zIndex: 50, backgroundColor: 'rgba(248, 249, 251, 0.95)', borderBottomWidth: 1, borderBottomColor: '#e7e8ea' },
  headerContent: { height: 70, paddingHorizontal: 20, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingTop: 15 },
  headerTitle: { fontSize: 13, fontWeight: '800', color: '#191c1e' },
  calcButton: { backgroundColor: '#003d9b', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 12, flexDirection: 'row', alignItems: 'center', gap: 4 },
  calcButtonText: { color: '#fff', fontSize: 11, fontWeight: '700' },
  variantsContainer: { position: 'absolute', top: 75, width: '100%', zIndex: 40, paddingVertical: 4 },
  variantChip: { backgroundColor: '#f1f5f9', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 16, borderWidth: 1, borderColor: '#cbd5e1' },
  variantChipText: { fontSize: 11, fontWeight: '700', color: '#334155' },
  infoCardContainer: { position: 'absolute', top: 120, width: '100%', zIndex: 40, paddingHorizontal: 16 },
  infoCard: { backgroundColor: '#ffffff', borderRadius: 14, padding: 12, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 3, borderWidth: 1, borderColor: '#e7e8ea' },
  infoRowTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  infoTitle: { fontSize: 12, fontWeight: '800', color: '#191c1e' },
  riskBadge: { fontSize: 11, fontWeight: '700' },
  infoDesc: { fontSize: 10, color: '#64748b', marginTop: 3 },
  metricsBox: { alignItems: 'flex-end', paddingLeft: 12, borderLeftWidth: 1, borderLeftColor: '#e2e8f0' },
  metricDist: { fontSize: 14, fontWeight: '800', color: '#003d9b' },
  metricTime: { fontSize: 10, color: '#64748b', fontWeight: '600', marginTop: 2 },
  mapContainer: { flex: 1, marginTop: 195, marginBottom: 80, position: 'relative' },
  bottomNav: { position: 'absolute', bottom: 0, width: '100%', height: 80, backgroundColor: 'rgba(248, 249, 251, 0.95)', borderTopWidth: 1, borderTopColor: '#e7e8ea', flexDirection: 'row', justifyContent: 'space-around', alignItems: 'center', zIndex: 50 },
  navItem: { flex: 1, height: '100%', justifyContent: 'center', alignItems: 'center', gap: 2 },
  navItemActive: { flex: 1, height: '100%', justifyContent: 'center', alignItems: 'center', gap: 2, backgroundColor: 'rgba(0, 61, 155, 0.04)' },
  navText: { fontSize: 11, fontWeight: '500', color: '#434654' },
  navTextActive: { fontSize: 11, fontWeight: '700', color: '#003d9b' }
});