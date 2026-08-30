import React, { useState } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, ScrollView, Platform, useWindowDimensions, Switch } from 'react-native';
import { useRouter } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';

export default function ProfileScreen() {
  const router = useRouter();
  const { width } = useWindowDimensions();
  const isDesktop = width > 768;

  const [gpsSync, setGpsSync] = useState(true);
  const [notificationsEnabled, setNotificationsEnabled] = useState(true);

  return (
    <View style={[styles.container, isDesktop && styles.desktopContainer]}>
      {/* Header Fijo */}
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <View style={styles.headerLeft}>
            <View style={styles.avatarPlaceholder} />
            <View>
              <Text style={styles.headerTitle}>Perfil y Configuración</Text>
              <Text style={styles.headerSubtitle}>Estado del dispositivo y cuenta</Text>
            </View>
          </View>
        </View>
      </View>

      {/* Contenido Desplazable */}
      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        <View style={styles.sectionCard}>
          <View style={styles.userProfileRow}>
            <View style={styles.largeAvatar}>
              <MaterialIcons name="person" size={32} color="#ffffff" />
            </View>
            <View style={{ flex: 1 }}>
              <Text style={styles.userName}>Ciudadano Conectado</Text>
              <Text style={styles.userEmail}>usuario.emergencia@red.cl</Text>
              <View style={styles.badgeContainer}>
                <MaterialIcons name="verified" size={14} color="#16a34a" />
                <Text style={styles.badgeText}>Cuenta Verificada</Text>
              </View>
            </View>
          </View>
        </View>

        <View style={styles.sectionCard}>
          <Text style={styles.sectionHeader}>Preferencias del Sistema</Text>
          
          <View style={styles.settingRow}>
            <View style={styles.settingInfo}>
              <MaterialIcons name="my-location" size={20} color="#003d9b" />
              <View style={{ marginLeft: 12, flex: 1 }}>
                <Text style={styles.settingTitle}>Sincronización GPS en Vivo</Text>
                <Text style={styles.settingSub}>Actualiza ubicación para Machine Learning</Text>
              </View>
            </View>
            <Switch
              value={gpsSync}
              onValueChange={setGpsSync}
              trackColor={{ false: '#cbd5e1', true: '#93c5fd' }}
              thumbColor={gpsSync ? '#003d9b' : '#f1f5f9'}
            />
          </View>

          <View style={styles.settingRow}>
            <View style={styles.settingInfo}>
              <MaterialIcons name="notifications-active" size={20} color="#003d9b" />
              <View style={{ marginLeft: 12, flex: 1 }}>
                <Text style={styles.settingTitle}>Alertas Push de Emergencia</Text>
                <Text style={styles.settingSub}>Recibir avisos oficiales inmediatos</Text>
              </View>
            </View>
            <Switch
              value={notificationsEnabled}
              onValueChange={setNotificationsEnabled}
              trackColor={{ false: '#cbd5e1', true: '#93c5fd' }}
              thumbColor={notificationsEnabled ? '#003d9b' : '#f1f5f9'}
            />
          </View>
        </View>

        <View style={styles.sectionCard}>
          <Text style={styles.sectionHeader}>Contactos de Emergencia</Text>
          <View style={styles.contactItem}>
            <MaterialIcons name="phone" size={18} color="#16a34a" />
            <Text style={styles.contactText}>Emergencias Médicas / Rescate: 131</Text>
          </View>
          <View style={styles.contactItem}>
            <MaterialIcons name="security" size={18} color="#003d9b" />
            <Text style={styles.contactText}>Protección Civil / Municipalidad</Text>
          </View>
        </View>

        <TouchableOpacity style={styles.logoutButton} onPress={() => alert('Sesión cerrada correctamente')}>
          <MaterialIcons name="logout" size={18} color="#ba1a1a" />
          <Text style={styles.logoutText}>Cerrar Sesión</Text>
        </TouchableOpacity>
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
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/alerts')}>
          <MaterialIcons name="notifications-active" size={24} color="#434654" />
          <Text style={styles.navText}>Alertas</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItemActive}>
          <MaterialIcons name="person" size={24} color="#003d9b" />
          <Text style={styles.navTextActive}>Perfil</Text>
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
  scrollContent: { paddingTop: 95, paddingBottom: 100, paddingHorizontal: 16, gap: 14 },
  sectionCard: { backgroundColor: '#ffffff', borderRadius: 20, padding: 20, elevation: 2, gap: 14 },
  userProfileRow: { flexDirection: 'row', alignItems: 'center', gap: 14 },
  largeAvatar: { width: 60, height: 60, borderRadius: 30, backgroundColor: '#003d9b', justifyContent: 'center', alignItems: 'center' },
  userName: { fontSize: 16, fontWeight: '800', color: '#191c1e' },
  userEmail: { fontSize: 12, color: '#5d5e61', marginTop: 2 },
  badgeContainer: { flexDirection: 'row', alignItems: 'center', gap: 4, marginTop: 6 },
  badgeText: { fontSize: 11, fontWeight: '700', color: '#16a34a' },
  sectionHeader: { fontSize: 13, fontWeight: '800', color: '#191c1e', marginBottom: 4 },
  settingRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 4 },
  settingInfo: { flexDirection: 'row', alignItems: 'center', flex: 1 },
  settingTitle: { fontSize: 13, fontWeight: '700', color: '#191c1e' },
  settingSub: { fontSize: 11, color: '#5d5e61', marginTop: 1 },
  contactItem: { flexDirection: 'row', alignItems: 'center', gap: 10, backgroundColor: '#f8f9fb', padding: 12, borderRadius: 10 },
  contactText: { fontSize: 12, fontWeight: '700', color: '#191c1e' },
  logoutButton: { backgroundColor: '#fee2e2', height: 48, borderRadius: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, marginTop: 4 },
  logoutText: { color: '#ba1a1a', fontSize: 13, fontWeight: '700' },
  bottomNav: { position: 'absolute', bottom: 0, width: '100%', height: 80, backgroundColor: 'rgba(248, 249, 251, 0.95)', borderTopWidth: 1, borderTopColor: '#e7e8ea', flexDirection: 'row', justifyContent: 'space-around', alignItems: 'center', zIndex: 50 },
  navItem: { flex: 1, height: '100%', justifyContent: 'center', alignItems: 'center', gap: 2 },
  navItemActive: { flex: 1, height: '100%', justifyContent: 'center', alignItems: 'center', gap: 2, backgroundColor: 'rgba(0, 61, 155, 0.04)' },
  navText: { fontSize: 11, fontWeight: '500', color: '#434654' },
  navTextActive: { fontSize: 11, fontWeight: '700', color: '#003d9b' }
});