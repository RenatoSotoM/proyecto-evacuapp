import React from 'react';
import { Tabs } from 'expo-router';
import { SyncStatusBadge } from '../../components/sync/SyncStatusBadge';

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: '#1A237E',
        tabBarInactiveTintColor: '#999',
        headerRight: () => <SyncStatusBadge />,
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: 'Mapa',
          tabBarIcon: ({ color, size }) => (
            <Text style={{ fontSize: size, color }}>🗺️</Text>
          ),
        }}
      />
      <Tabs.Screen
        name="alerts-tab"
        options={{
          title: 'Alertas',
          tabBarIcon: ({ color, size }) => (
            <Text style={{ fontSize: size, color }}>🔔</Text>
          ),
        }}
      />
      <Tabs.Screen
        name="routes-tab"
        options={{
          title: 'Rutas',
          tabBarIcon: ({ color, size }) => (
            <Text style={{ fontSize: size, color }}>🚶</Text>
          ),
        }}
      />
      <Tabs.Screen
        name="profile-tab"
        options={{
          title: 'Perfil',
          tabBarIcon: ({ color, size }) => (
            <Text style={{ fontSize: size, color }}>👤</Text>
          ),
        }}
      />
    </Tabs>
  );
}