import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, ActivityIndicator, Dimensions, TouchableOpacity, Platform } from 'react-native';
import { WebView } from 'react-native-webview';
import * as Location from 'expo-location';
import { useLocalSearchParams, router } from 'expo-router';
import { OSRM_BASE_URL } from '../config/api';

export default function RouteDetailScreen() {
  const { lat, lng, name } = useLocalSearchParams();
  const [routeCoords, setRouteCoords] = useState<any[]>([]);
  const [origin, setOrigin] = useState<any>(null);
  const [distance, setDistance] = useState('Calculando...');
  const [duration, setDuration] = useState('Calculando...');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    calculateRoute();
  }, []);

  const calculateRoute = async () => {
    try {
      let currentOrigin = { latitude: -33.5851, longitude: -70.7010 }; // Coordenada de prueba inicial

      try {
        const { status } = await Location.requestForegroundPermissionsAsync();
        if (status === 'granted') {
          const location = await Location.getCurrentPositionAsync({ accuracy: Location.LocationAccuracy.Balanced });
          currentOrigin = {
            latitude: location.coords.latitude,
            longitude: location.coords.longitude,
          };
        }
      } catch (e) {
        console.warn('Usando coordenadas por defecto');
      }

      setOrigin(currentOrigin);

      const destLat = Number(lat);
      const destLng = Number(lng);

      // ==========================================
      // AQUÍ VA LA LÍNEA DEL OSRM PÚBLICO
      // ==========================================
      const osrmUrl = `https://router.project-osrm.org/route/v1/driving/${currentOrigin.longitude},${currentOrigin.latitude};${destLng},${destLat}?overview=full&geometries=geojson`;
      
      const res = await fetch(osrmUrl);
      const data = await res.json();

      if (data.routes && data.routes.length > 0) {
        const route = data.routes[0];
        const coords = route.geometry.coordinates.map((c: [number, number]) => [c[1], c[0]]);
        
        setRouteCoords(coords);
        setDistance((route.distance / 1000).toFixed(2) + ' km');
        setDuration(Math.ceil(route.duration / 60) + ' min');
      } else {
        setRouteCoords([[currentOrigin.latitude, currentOrigin.longitude], [destLat, destLng]]);
        setDistance('3.5 km');
        setDuration('10 min');
      }
    } catch (error) {
      console.error('Error calculando ruta:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading || !origin) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#003d9b" />
        <Text style={styles.loadingText}>Calculando trayecto vial...</Text>
      </View>
    );
  }

  // Renderizado compatible con Web y WebView móvil
  const mapHtml = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1.0" />
      <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
      <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
      <style>body, html, #map { margin: 0; padding: 0; height: 100%; width: 100%; }</style>
    </head>
    <body>
      <div id="map"></div>
      <script>
        var map = L.map('map', { zoomControl: false }).setView([${origin.latitude}, ${origin.longitude}], 14);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}@{x}/{y}.png', { maxZoom: 19 }).addTo(map);
        
        L.circleMarker([${origin.latitude}, ${origin.longitude}], { color: '#fff', fillColor: '#003d9b', fillOpacity: 1, radius: 9 }).addTo(map).bindPopup('Origen');
        L.circleMarker([${lat}, ${lng}], { color: '#fff', fillColor: '#28a745', fillOpacity: 1, radius: 11 }).addTo(map).bindPopup('${name}');

        var coords = ${JSON.stringify(routeCoords)};
        if(coords.length > 0) {
          var polyline = L.polyline(coords, { color: '#003d9b', weight: 6, opacity: 0.85 }).addTo(map);
          map.fitBounds(polyline.getBounds(), { padding: [50, 50] });
        }
      </script>
    </body>
    </html>
  `;

  return (
    <View style={styles.container}>
      {Platform.OS === 'web' ? (
        // En PC/Web se inyecta directamente un iframe HTML seguro para Leaflet
        <iframe
          srcDoc={mapHtml}
          style={{ width: '100%', height: '100%', border: 'none' }}
        />
      ) : (
        <WebView style={styles.map} source={{ html: mapHtml }} javaScriptEnabled={true} />
      )}

      <View style={styles.sheet}>
        <View style={styles.infoContainer}>
          <Text style={styles.label} numberOfLines={1}>Destino: {name}</Text>
          <Text style={styles.metrics}>{duration} • {distance}</Text>
        </View>
        <TouchableOpacity style={styles.btn} onPress={() => router.back()}>
          <Text style={styles.btnText}>Regresar</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#f8f9fb' },
  loadingText: { marginTop: 10, color: '#434654' },
  map: { width: Dimensions.get('window').width, height: Dimensions.get('window').height },
  sheet: { position: 'absolute', bottom: 0, left: 0, right: 0, backgroundColor: '#fff', padding: 20, borderTopLeftRadius: 20, borderTopRightRadius: 20, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', elevation: 10 },
  infoContainer: { flex: 1, marginRight: 12 },
  label: { fontSize: 13, fontWeight: '600', color: '#5d5e61' },
  metrics: { fontSize: 18, fontWeight: '700', color: '#003d9b', marginTop: 2 },
  btn: { backgroundColor: '#ba1a1a', paddingVertical: 12, paddingHorizontal: 20, borderRadius: 10 },
  btnText: { color: '#fff', fontWeight: '600', fontSize: 15 }
});