import { Stack } from 'expo-router';
import { View, StyleSheet } from 'react-native';
import { AppProvider, useApp } from '../context/AppContext';

function MainLayout() {
  const { emergencyMode } = useApp();

  return (
    <View style={styles.container}>
      {emergencyMode && <View style={styles.emergencyGlow} pointerEvents="none" />}
      <Stack screenOptions={{ headerShown: false }} />
    </View>
  );
}

export default function RootLayout() {
  return (
    <AppProvider>
      <MainLayout />
    </AppProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0A0D10',
  },
  emergencyGlow: {
    ...StyleSheet.absoluteFillObject,
    borderWidth: 2,
    borderColor: '#FF3B30',
    zIndex: 9999,
  },
});