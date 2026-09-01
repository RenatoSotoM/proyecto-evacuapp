import React, { useState } from 'react';
import { StyleSheet, Text, View, TextInput, TouchableOpacity, Alert, ScrollView } from 'react-native';
import * as Location from 'expo-location';
import { createIncident, IncidentPayload } from '../services/incidentService';

export default function ReportFormScreen() {
  const [type, setType] = useState<IncidentPayload['type']>('BLOQUEO_VIAL');
  const [severity, setSeverity] = useState<IncidentPayload['severity']>('HIGH');
  const [description, setDescription] = useState('');

  const incidentTypes: IncidentPayload['type'][] = [
    'BLOQUEO_VIAL', 'ESCOMBROS', 'INUNDACION', 'INCENDIO', 'ACCIDENTE', 'RUTA_INACCESIBLE', 'PELIGRO_GENERAL'
  ];

  const severities: IncidentPayload['severity'][] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  const handleSubmit = async () => {
    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        Alert.alert('Permiso denegado', 'Se requiere acceso a la ubicación para registrar el incidente.');
        return;
      }

      const location = await Location.getCurrentPositionAsync({});
      
      await createIncident({
        type,
        severity,
        description,
        latitude: location.coords.latitude,
        longitude: location.coords.longitude,
      });

      Alert.alert('Éxito', 'Incidente reportado correctamente a la red de emergencia');
    } catch (error) {
      Alert.alert('Error', 'No se pudo registrar el incidente en el servidor');
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>Reportar Incidente</Text>
      
      <Text style={styles.label}>Tipo de incidente:</Text>
      <View style={styles.grid}>
        {incidentTypes.map((t) => (
          <TouchableOpacity 
            key={t} 
            style={[styles.chip, type === t && styles.selectedChip]} 
            onPress={() => setType(t)}
          >
            <Text style={[styles.chipText, type === t && styles.selectedChipText]}>{t}</Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.label}>Severidad:</Text>
      <View style={styles.grid}>
        {severities.map((s) => (
          <TouchableOpacity 
            key={s} 
            style={[styles.chip, severity === s && styles.selectedChip]} 
            onPress={() => setSeverity(s)}
          >
            <Text style={[styles.chipText, severity === s && styles.selectedChipText]}>{s}</Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.label}>Descripción (Opcional):</Text>
      <TextInput
        style={styles.input}
        placeholder="Ej: Calle bloqueada por escombros"
        value={description}
        onChangeText={setDescription}
        multiline
      />

      <TouchableOpacity style={styles.button} onPress={handleSubmit}>
        <Text style={styles.buttonText}>Enviar Reporte</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 20, backgroundColor: '#fff', flexGrow: 1, justifyContent: 'center' },
  title: { fontSize: 22, fontWeight: 'bold', marginBottom: 20, textAlign: 'center' },
  label: { fontSize: 14, fontWeight: '600', marginBottom: 8, color: '#333' },
  grid: { flexDirection: 'row', flexWrap: 'wrap', marginBottom: 15 },
  chip: { paddingVertical: 6, paddingHorizontal: 10, borderWidth: 1, borderColor: '#ccc', borderRadius: 15, margin: 3 },
  selectedChip: { backgroundColor: '#007AFF', borderColor: '#007AFF' },
  chipText: { fontSize: 11, color: '#333' },
  selectedChipText: { color: '#fff', fontWeight: 'bold' },
  input: { borderWidth: 1, borderColor: '#ccc', padding: 10, borderRadius: 5, height: 80, textAlignVertical: 'top', marginBottom: 20 },
  button: { backgroundColor: '#FF3B30', padding: 15, borderRadius: 5, alignItems: 'center' },
  buttonText: { color: '#fff', fontWeight: 'bold' }
});