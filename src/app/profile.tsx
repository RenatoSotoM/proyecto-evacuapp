import React, { useState } from 'react';
import { StyleSheet, Text, View, TextInput, Switch, TouchableOpacity, Alert } from 'react-native';
import { updateMobilityProfile } from '../services/authService';

export default function ProfileTabScreen() {
  const [mobilityType, setMobilityType] = useState<'PEATON' | 'VEHICULO' | 'MOVILIDAD_REDUCIDA'>('PEATON');
  const [requiresAccessibleRoute, setRequiresAccessibleRoute] = useState(false);
  const [travelsWithMinors, setTravelsWithMinors] = useState(false);
  const [companionCount, setCompanionCount] = useState('0');

  const handleSave = async () => {
    try {
      await updateMobilityProfile({
        mobilityType,
        requiresAccessibleRoute,
        travelsWithMinors,
        companionCount: parseInt(companionCount) || 0,
      });
      Alert.alert('Éxito', 'Perfil de movilidad actualizado correctamente.');
    } catch (error) {
      Alert.alert('Error', 'No se pudo actualizar el perfil en el servidor.');
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Perfil de Movilidad</Text>
      
      <Text style={styles.label}>Modo de Desplazamiento:</Text>
      <View style={styles.row}>
        {['PEATON', 'VEHICULO', 'MOVILIDAD_REDUCIDA'].map((type) => (
          <TouchableOpacity
            key={type}
            style={[styles.optionButton, mobilityType === type && styles.selectedOption]}
            onPress={() => setMobilityType(type as any)}
          >
            <Text style={[styles.optionText, mobilityType === type && styles.selectedText]}>{type}</Text>
          </TouchableOpacity>
        ))}
      </View>

      <View style={styles.switchRow}>
        <Text style={styles.labelSwitch}>¿Requiere ruta accesible?</Text>
        <Switch value={requiresAccessibleRoute} onValueChange={setRequiresAccessibleRoute} />
      </View>

      <View style={styles.switchRow}>
        <Text style={styles.labelSwitch}>¿Viaja con menores?</Text>
        <Switch value={travelsWithMinors} onValueChange={setTravelsWithMinors} />
      </View>

      <Text style={styles.label}>Cantidad de acompañantes:</Text>
      <TextInput
        style={styles.input}
        placeholder="0"
        value={companionCount}
        onChangeText={setCompanionCount}
        keyboardType="numeric"
      />

      <TouchableOpacity style={styles.button} onPress={handleSave}>
        <Text style={styles.buttonText}>Guardar Perfil</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, justifyContent: 'center', backgroundColor: '#fff' },
  title: { fontSize: 22, fontWeight: 'bold', marginBottom: 20, textAlign: 'center' },
  label: { fontSize: 14, marginBottom: 8, fontWeight: '600', color: '#333' },
  labelSwitch: { fontSize: 14, fontWeight: '600', color: '#333' },
  row: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 20 },
  optionButton: { padding: 8, borderWidth: 1, borderColor: '#ccc', borderRadius: 5, flex: 1, marginHorizontal: 2, alignItems: 'center' },
  selectedOption: { backgroundColor: '#007AFF', borderColor: '#007AFF' },
  optionText: { fontSize: 11, color: '#333' },
  selectedText: { color: '#fff', fontWeight: 'bold' },
  switchRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 },
  input: { borderWidth: 1, borderColor: '#ccc', padding: 10, marginBottom: 20, borderRadius: 5 },
  button: { backgroundColor: '#28A745', padding: 15, borderRadius: 5, alignItems: 'center' },
  buttonText: { color: '#fff', fontWeight: 'bold' }
});