import React, { createContext, useContext, useState, useEffect } from 'react';
import { secureStore } from '../services/storage/SecureStore';
import { apiClient } from '../api/client';
import { User, AuthState } from '../types';

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string, phone?: string) => Promise<void>;
  logout: () => Promise<void>;
  updateUser: (user: User) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [state, setState] = useState<AuthState>({
    user: null,
    token: null,
    isLoading: true,
    isAuthenticated: false,
  });

  useEffect(() => {
    loadStoredAuth();
  }, []);

  const loadStoredAuth = async () => {
    try {
      const token = await secureStore.getToken();
      const user = await secureStore.getUser();
      
      if (token && user) {
        apiClient.setAuthToken(token);
        setState({
          user,
          token,
          isLoading: false,
          isAuthenticated: true,
        });
      } else {
        setState(prev => ({ ...prev, isLoading: false }));
      }
    } catch (error) {
      console.error('Error loading auth:', error);
      setState(prev => ({ ...prev, isLoading: false }));
    }
  };

  const login = async (email: string, password: string) => {
    try {
      const response = await apiClient.getAxiosInstance().post('/auth/login', { email, password });
      const { accessToken, user } = response.data;
      
      await secureStore.saveToken(accessToken);
      await secureStore.saveUser(user);
      
      apiClient.setAuthToken(accessToken);
      setState({
        user,
        token: accessToken,
        isLoading: false,
        isAuthenticated: true,
      });
    } catch (error) {
      throw error;
    }
  };

  const register = async (name: string, email: string, password: string, phone?: string) => {
    try {
      const response = await apiClient.getAxiosInstance().post('/auth/register', { name, email, password, phone });
      const { accessToken, user } = response.data;
      
      await secureStore.saveToken(accessToken);
      await secureStore.saveUser(user);
      
      apiClient.setAuthToken(accessToken);
      setState({
        user,
        token: accessToken,
        isLoading: false,
        isAuthenticated: true,
      });
    } catch (error) {
      throw error;
    }
  };

  const logout = async () => {
    try {
      await secureStore.clearAll();
      apiClient.clearAuthToken();
      setState({
        user: null,
        token: null,
        isLoading: false,
        isAuthenticated: false,
      });
    } catch (error) {
      console.error('Error logging out:', error);
    }
  };

  const updateUser = (user: User) => {
    setState(prev => ({ ...prev, user }));
    secureStore.saveUser(user);
  };

  return (
    <AuthContext.Provider
      value={{
        user: state.user,
        isLoading: state.isLoading,
        isAuthenticated: state.isAuthenticated,
        login,
        register,
        logout,
        updateUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};