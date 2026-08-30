import React, { createContext, useContext, useState } from 'react';
import { useRouter } from 'expo-router';

export type ScreenName =
  | 'splash' | 'onboarding' | 'setup'
  | 'dashboard' | 'routes-tab' | 'reports-tab' | 'alerts-tab' | 'profile-tab'
  | 'emergency-type' | 'emergency-mode' | 'route-detail' | 'navigation'
  | 'meeting-points' | 'meeting-point-detail'
  | 'report-form' | 'report-sent'
  | 'offline-maps' | 'sync-status' | 'report-history'
  | 'settings';

export type ConnectionStatus = 'online' | 'limited' | 'offline';

export interface AppCtx {
  navigate: (screen: ScreenName, params?: Record<string, unknown>) => void;
  goBack: () => void;
  params: Record<string, unknown>;
  connectionStatus: ConnectionStatus;
  emergencyMode: boolean;
  setEmergencyMode: (v: boolean) => void;
  activeTab: ScreenName;
  setActiveTab: (tab: ScreenName) => void;
  userLocation: { latitude: number; longitude: number } | null;
  setUserLocation: (loc: { latitude: number; longitude: number }) => void;
}

const AppContext = createContext<AppCtx | undefined>(undefined);

export function AppProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [params, setParams] = useState<Record<string, unknown>>({});
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('online');
  const [emergencyMode, setEmergencyMode] = useState<boolean>(false);
  const [activeTab, setActiveTab] = useState<ScreenName>('dashboard');
  const [userLocation, setUserLocation] = useState<{ latitude: number; longitude: number } | null>(null);

  const navigate = (screen: ScreenName, newParams?: Record<string, unknown>) => {
    setParams(newParams ?? {});
    
    // Mapeo inteligente hacia las rutas físicas de Expo Router
    switch (screen) {
      case 'dashboard':
      case 'routes-tab':
      case 'reports-tab':
      case 'alerts-tab':
      case 'profile-tab':
        setActiveTab(screen);
        router.push('/home');
        break;
      case 'emergency-mode':
        setEmergencyMode(true);
        router.push('/emergency-mode');
        break;
      case 'report-form':
        router.push('/report');
        break;
      default:
        router.push(`/${screen}` as any);
        break;
    }
  };

  const goBack = () => {
    router.back();
  };

  return (
    <AppContext.Provider
      value={{
        navigate,
        goBack,
        params,
        connectionStatus,
        emergencyMode,
        setEmergencyMode,
        activeTab,
        setActiveTab,
        userLocation,
        setUserLocation,
      }}
    >
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp debe ser utilizado dentro de un AppProvider');
  }
  return context;
}