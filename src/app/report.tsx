import React, { useState } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, TextInput, ScrollView, Platform, useWindowDimensions, Modal } from 'react-native';
import { useRouter } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';

export default function ReportScreen() {
  const router = useRouter();
  const { width } = useWindowDimensions();
  const isDesktop = width > 768;

  const [selectedCategory, setSelectedCategory] = useState('Inundación');
  const [description, setDescription] = useState('');
  const [successModal, setSuccessModal] = useState(false);

  const categories = [
    { id: 'Inundación', icon: 'water', color: '#0284c7' },
    { id: 'Derrumbe', icon: 'landslide', color: '#d97706' },
    { id: 'Incendio', icon: 'local-fire-department', color: '#dc2626' },
    { id: 'Bloqueo Vial', icon: 'block', color: '#4b5563' },
  ];

  const handleSendReport = () => {
    if (!description.trim()) {
      alert('Por favor ingresa una breve descripción del incidente.');
      return;
    }
    setSuccessModal(true);
  };

  return (
    <View style={[styles.container, isDesktop && styles.desktopContainer]}>
      {/* Header Fijo */}
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <View style={styles.headerLeft}>
            <View style={styles.avatarPlaceholder} />
            <View>
              <Text style={styles.headerTitle}>Reportar Riesgo</Text>
              <Text style={styles.headerSubtitle}>Ayuda a la comunidad en tiempo real</Text>
            </View>
          </View>
        </View>
      </View>

      {/* Contenido Desplazable */}
      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        <View style={styles.sectionCard}>
          <Text style={styles.sectionLabel}>Selecciona el tipo de emergencia</Text>
          <View style={styles.categoriesGrid}>
            {categories.map((cat) => {
              const isSelected = selectedCategory === cat.id;
              return (
                <TouchableOpacity
                  key={cat.id}
                  style={[styles.categoryCard, isSelected && styles.categoryCardActive]}
                  onPress={() => setSelectedCategory(cat.id)}
                >
                  <MaterialIcons name={cat.icon as any} size={28} color={isSelected ? '#003d9b' : cat.color} />
                  <Text style={[styles.categoryText, isSelected && styles.categoryTextActive]}>{cat.id}</Text>
                </TouchableOpacity>
              );
            })}
          </View>

          <Text style={styles.sectionLabel}>Ubicación actual del incidente</Text>
          <View style={styles.locationBox}>
            <MaterialIcons name="my-location" size={18} color="#003d9b" />
            <Text style={styles.locationText}>Lat: -33.5900, Lon: -70.7010 (GPS Automático)</Text>
          </View>

          <Text style={styles.sectionLabel}>Descripción del evento</Text>
          <TextInput
            style={styles.textInput}
            placeholder="Ej: Calle bloqueada por caída de árbol y acumulación de agua..."
            placeholderTextColor="#8e9099"
            multiline
            numberOfLines={4}
            value={description}
            onChangeText={setDescription}
          />

          <TouchableOpacity style={styles.submitButton} onPress={handleSendReport}>
            <MaterialIcons name="send" size={18} color="#ffffff" />
            <Text style={styles.submitButtonText}>Enviar Reporte Comunitario</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>

      {/* Modal de Éxito */}
      <Modal visible={successModal} transparent={true} animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <MaterialIcons name="check-circle" size={48} color="#16a34a" />
            <Text style={styles.modalTitle}>¡Reporte Enviado!</Text>
            <Text style={styles.modalSub}>Tu aviso ha sido procesado por el sistema y notificado a los usuarios cercanos.</Text>
            <TouchableOpacity 
              style={styles.modalCloseBtn} 
              onPress={() => {
                setSuccessModal(false);
                setDescription('');
                router.push('/map');
              }}
            >
              <Text style={styles.modalCloseText}>Volver al Mapa</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

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
        <TouchableOpacity style={styles.navItemActive}>
          <MaterialIcons name="emergency" size={24} color="#003d9b" />
          <Text style={styles.navTextActive}>Reportar</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.navItem} onPress={() => router.push('/alerts')}>
          <MaterialIcons name="notifications-active" size={24} color="#434654" />
          <Text style={styles.navText}>Alertas</Text>
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
  sectionCard: { backgroundColor: '#ffffff', borderRadius: 20, padding: 20, elevation: 2, gap: 14 },
  sectionLabel: { fontSize: 13, fontWeight: '700', color: '#191c1e', marginTop: 4 },
  categoriesGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  categoryCard: { width: '48%', backgroundColor: '#f8f9fb', borderWidth: 1, borderColor: '#e2e8f0', borderRadius: 14, padding: 16, alignItems: 'center', gap: 8 },
  categoryCardActive: { backgroundColor: 'rgba(0, 61, 155, 0.05)', borderColor: '#003d9b' },
  categoryText: { fontSize: 12, fontWeight: '600', color: '#434654' },
  categoryTextActive: { color: '#003d9b', fontWeight: '700' },
  locationBox: { flexDirection: 'row', alignItems: 'center', gap: 8, backgroundColor: '#f1f5f9', padding: 12, borderRadius: 10 },
  locationText: { fontSize: 11, fontWeight: '600', color: '#334155' },
  textInput: { backgroundColor: '#f8f9fb', borderWidth: 1, borderColor: '#cbd5e1', borderRadius: 12, padding: 12, fontSize: 13, color: '#191c1e', textAlignVertical: 'top', minHeight: 100 },
  submitButton: { backgroundColor: '#003d9b', height: 48, borderRadius: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, marginTop: 8, elevation: 3 },
  submitButtonText: { color: '#ffffff', fontSize: 13, fontWeight: '700' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.6)', justifyContent: 'center', alignItems: 'center', padding: 20 },
  modalContent: { backgroundColor: '#fff', borderRadius: 20, padding: 24, alignItems: 'center', gap: 12, width: '100%', maxWidth: 320 },
  modalTitle: { fontSize: 16, fontWeight: '800', color: '#191c1e', textAlign: 'center' },
  modalSub: { fontSize: 12, color: '#434654', textAlign: 'center', lineHeight: 18 },
  modalCloseBtn: { backgroundColor: '#003d9b', paddingVertical: 10, paddingHorizontal: 20, borderRadius: 10, marginTop: 8 },
  modalCloseText: { color: '#fff', fontWeight: '700', fontSize: 12 },
  bottomNav: { position: 'absolute', bottom: 0, width: '100%', height: 80, backgroundColor: 'rgba(248, 249, 251, 0.95)', borderTopWidth: 1, borderTopColor: '#e7e8ea', flexDirection: 'row', justifyContent: 'space-around', alignItems: 'center', zIndex: 50 },
  navItem: { flex: 1, height: '100%', justifyContent: 'center', alignItems: 'center', gap: 2 },
  navItemActive: { flex: 1, height: '100%', justifyContent: 'center', alignItems: 'center', gap: 2, backgroundColor: 'rgba(0, 61, 155, 0.04)' },
  navText: { fontSize: 11, fontWeight: '500', color: '#434654' },
  navTextActive: { fontSize: 11, fontWeight: '700', color: '#003d9b' }
});