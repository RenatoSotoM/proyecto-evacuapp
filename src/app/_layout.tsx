import { Stack } from 'expo-router';

export default function RootLayout() {
  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Screen name="index" />
      <Stack.Screen name="map" />
      <Stack.Screen name="routes" />
      <Stack.Screen name="report" />
      <Stack.Screen name="alerts" />
      <Stack.Screen name="profile" />
    </Stack>
  );
}