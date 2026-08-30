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

  // Variantes de rutas con destinos reales en la comuna para enrutamiento por calles reales (OSRM)
  const routeVariants = [
    {
      id: 0,
      title: 'Variante A: Directa Principal',
      distance: '3.2 km',
      time: '7 min (Auto)',
      risk: 'Moderado',
      color: '#2563eb', // Azul Waze activo
      destLat: -33.5785, // Destino real hacia el norte (Hospital El Pino)
      destLon: -70.6850,
      description: 'Ruta vehicular principal. Sigue ejes viales asfaltados de alta capacidad.'
    },
    {
      id: 1,
      title: 'Variante B: Perimetral Segura',
      distance: '4.5 km',
      time: '11 min (Auto)',
      risk: 'Bajo',
      color: '#16a34a', // Verde seguro
      destLat: -33.5720, // Destino poniente seguro
      destLon: -70.7100,
      description: 'Ruta perimetral que evita zonas de alta congestión y riesgo evaluado.'
    },
    {
      id: 2,
      title: 'Variante C: Alternativa Oriente',
      distance: '5.1 km',
      time: '14 min (Auto)',
      risk: 'Mínimo',
      color: '#9333ea', // Morado alternativo
      destLat: -33.6050, // Destino sur/oriente
      destLon: -70.6750,
      description: 'Trayecto vehicular alternativo con menor flujo de tráfico reportado.'
    }
  ];

  const handleCalculateRoutes = async () => {
    try {
      setLoading(true);
      let { status } = await Location.requestForegroundPermissionsAsync();
      if (status === 'granted') {
        let location = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High });
        setUserLocation({
          lat: location.coords.latitude,
          lon: location.coords.longitude
        });
      }
      setTimeout(() => {
        setLoading(false);
      }, 500);
    } catch (error) {
      setLoading(false);
      alert('Error al calcular las variantes viales.');
    }
  };

  useEffect(() => {
    handleCalculateRoutes();
  }, []);

  const currentRoute = routeVariants[selectedVariant];

  // HTML que consulta OSRM (Open Source Routing Machine) para obtener las calles reales de OSM
  const generateRouteMapHtml = () => {
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

          // Marcador de Vehículo (Origen)
          L.marker([${userLocation.lat}, ${userLocation.lon}], {
            icon: L.divIcon({
              className: 'vehicle-marker',
              html: '<div style="font-size: 20px; background: #2563eb; color: white; border-radius: 50%; width: 38px; height: 38px; display: flex; align-items: center; justify-content: center; box-shadow: 0 0 12px rgba(37,99,235,0.8); border: 2px solid white;">🚗</div>',
              iconSize: [38, 38]
            })
          }).addTo(map).bindPopup('<b>Tu Vehículo (Posición Actual)</b>');

          // Marcador de Zona Segura (Destino)
          L.marker([${currentRoute.destLat}, ${currentRoute.destLon}], {
            icon: L.divIcon({
              className: 'target-marker',
              html: '<div style="font-size: 20px; background: ${currentRoute.color}; color: white; border-radius: 50%; width: 38px; height: 38px; display: flex; align-items: center; justify-content: center; box-shadow: 0 0 12px rgba(0,0,0,0.3); border: 2px solid white;">🛡️</div>',
              iconSize: [38, 38]
            })
          }).addTo(map).bindPopup('<b>Zona Segura Vehicular</b>');

          // Petición al motor de ruteo OSRM para seguir las calles reales de OpenStreetMap
          var startLon = ${userLocation.lon};
          var startLat = ${userLocation.lat};
          var destLon = ${currentRoute.destLon};
          var destLat = ${currentRoute.destLat};

          var osrmUrl = 'https://router.project-osrm.org/route/v1/driving/' + startLon + ',' + startLat + ';' + destLon + ',' + destLat + '?overview=full&geometries=geojson';

          fetch(osrmUrl)
            .then(function(res) { return res.json(); })
            .then(function(data) {
              if (data.routes && data.routes.length > 0) {
                var coords = data.routes[0].geometry.coordinates.map(function(c) {
                  return [c[1], c[0]]; // Leaflet requiere [lat, lon]
                });

                // Sombra de ruta estilo Waze
                L.polyline(coords, {
                  color: '#1e3a8a',
                  weight: 8,
                  opacity: 0.5,
                  lineCap: 'round',
                  lineJoin: 'round'
                }).addTo(map);

                // Ruta principal oficial sobre las calles de OSM
                var mainPolyline = L.polyline(coords, {
                  color: '${currentRoute.color}',
                  weight: 5,
                  opacity: 0.95,
                  lineCap: 'round',
                  lineJoin: 'round'
                }).addTo(map);

                map.fitBounds(mainPolyline.getBounds(), {padding: [50, 50]});
              }
            })
            .catch(function(err) {
              console.error('Error al obtener ruta OSRM:', err);
            });
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
          <Text style={styles.headerTitle}>Navegación Vehicular (IA Tesis)</Text>
          <TouchableOpacity style={styles.calcButton} onPress={handleCalculateRoutes} disabled={loading}>
            {loading ? (
              <ActivityIndicator size="small" color="#fff" />
            ) : (
              <>
                <MaterialIcons name="alt-route" size={16} color="#fff" />
                <Text style={styles.calcButtonText}>Calcular Rutas</Text>
              </>
            )}
          </TouchableOpacity>
        </View>
      </View>

      {/* Selector Horizontal de Variantes de Rutas (Foco Riesgo / Vehículo) */}
      <View style={styles.variantsContainer}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8, paddingHorizontal: 16 }}>
          {routeVariants.map((variant, index) => (
            <TouchableOpacity
              key={variant.id}
              style={[
                styles.variantChip, 
                selectedVariant === index && { backgroundColor: variant.color, borderColor: variant.color }
              ]}
              onPress={() => setSelectedVariant(index)}
            >
              <Text style={[styles.variantChipText, selectedVariant === index && { color: '#fff' }]}>
                {variant.title} ({variant.risk})
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      {/* Tarjeta de Información Detallada */}
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
            <Text style={styles.metricDist}>{currentRoute.distance}</Text>
            <Text style={styles.metricTime}>{currentRoute.time}</Text>
          </View>
        </View>
      </View>

      {/* Contenedor del Mapa con Calles Reales de OSM */}
      <View style={styles.mapContainer}>
        {Platform.OS === 'web' ? (
          // @ts-ignore
          <iframe 
            key={`${userLocation.lat}-${userLocation.lon}-${selectedVariant}`}
            srcDoc={generateRouteMapHtml()} 
            style={{ width: '100%', height: '100%', border: 0 }} 
            title="Navegación Vehicular Oficial Tesis" 
          />
        ) : (
          <WebView 
            key={`${userLocation.lat}-${userLocation.lon}-${selectedVariant}`}
            originWhitelist={['*']}
            source={{ html: generateRouteMapHtml() }} 
            style={{ flex: 1 }}
          />
        )}
      </View>

      {/* Barra de Navegación Inferior */}
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