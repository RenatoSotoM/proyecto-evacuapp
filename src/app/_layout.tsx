import React from 'react';
import { Stack } from 'expo-router';
import { AppProvider } from '../context/AppContext';
import { SyncStatusBadge } from '../components/sync/SyncStatusBadge';

export default function RootLayout() {
  return (
    <AppProvider>
      <Stack>
        <Stack.Screen
          name="(tabs)"
          options={{
            headerShown: false,
          }}
        />
        <Stack.Screen
          name="login"
          options={{
            headerShown: false,
          }}
        />
        <Stack.Screen
          name="register"
          options={{
            headerShown: false,
          }}
        />
        <Stack.Screen
          name="report-form"
          options={{
            title: 'Nuevo Reporte',
            headerBackTitle: 'Volver',
          }}
        />
        <Stack.Screen
          name="meeting-points"
          options={{
            title: 'Puntos de Encuentro',
            headerBackTitle: 'Volver',
          }}
        />
      </Stack>
    </AppProvider>
  );
}