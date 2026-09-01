export interface User {
  id: string;
  name: string;
  email: string;
  role: 'USER' | 'ADMIN';
  phone?: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface AuthState {
  user: User | null;
  token: string | null;
  isLoading: boolean;
  isAuthenticated: boolean;
}

export interface Emergency {
  id: string;
  type: string;
  status: string;
  title: string;
  description: string;
  startedAt: Date;
  endedAt?: Date;
}

export interface SafeZone {
  id: string;
  name: string;
  description?: string;
  location: {
    type: string;
    coordinates: [number, number];
  };
  capacity?: number;
  services?: string[];
  emergencyId: string;
}

export interface Incident {
  id: string;
  type: string;
  severity: string;
  status: string;
  description?: string;
  latitude: number;
  longitude: number;
  userId: string;
  emergencyId?: string;
  createdAt: Date;
}

export interface PointOfInterest {
  id: string;
  name: string;
  type: string;
  location: {
    type: string;
    coordinates: [number, number];
  };
  address?: string;
  phone?: string;
}