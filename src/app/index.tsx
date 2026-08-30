import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, Image, SafeAreaView } from 'react-native';
import { useRouter } from 'expo-router';

const messages = [
  "Preparando información...",
  "Conectando a satélites...",
  "Verificando rutas seguras..."
];

export default function SplashScreen() {
  const [messageIndex, setMessageIndex] = useState(0);
  const router = useRouter();

  useEffect(() => {
    const interval = setInterval(() => {
      setMessageIndex((prevIndex) => (prevIndex + 1) % messages.length);
    }, 2500);

    // Simula el tiempo de carga inicial antes de entrar a la app (ej. 4 segundos)
    const timer = setTimeout(() => {
      router.replace('/home'); // Cambia '/home' por la ruta de tu panel principal
    }, 4000);

    return () => {
      clearInterval(interval);
      clearTimeout(timer);
    };
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.contentContainer}>
        <View style={styles.logoContainer}>
          <Image 
            source={{ uri: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBPeJPPNaZjLUmbwSJqyfMA1_Ghrtqg9A7RFV7oJcIwbCMZ-pdFN7R3xvXbBW58RxPMRVNW3ndQeZvPGO2k1XV6AXoPDgCgGU8MMFmkIsGOWzM9eIbk6wSEIi2RM9eltr98r1D29La3wH4vRayAQw4CnkA-75bO1Pnqxz-ysZskIVhEBRL8rFWsuHXPEUErp6pPnfvFmgog-jR_ZIWG5bVwNJehGEe052_VpQflnn-3MufZNQqMr0Kb' }} 
            style={styles.logo}
            resizeMode="contain"
          />
        </View>

        <View style={styles.textContainer}>
          <Text style={styles.title}>EvacuApp</Text>
          <Text style={styles.subtitle}>Orientación inteligente para emergencias</Text>
        </View>
      </View>

      <View style={styles.bottomContainer}>
        <View style={styles.progressBarBackground}>
          <View style={styles.progressBarFill} />
        </View>
        
        <View style={styles.statusRow}>
          <Text style={styles.statusText}>{messages[messageIndex]}</Text>
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fb',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
  },
  contentContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    width: '100%',
    maxWidth: 360,
  },
  logoContainer: {
    width: 128,
    height: 128,
    borderRadius: 16,
    backgroundColor: '#ffffff',
    shadowColor: '#0052cc',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 10,
    elevation: 5,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 16,
    marginBottom: 32,
  },
  logo: {
    width: '100%',
    height: '100%',
    borderRadius: 4,
  },
  textContainer: {
    alignItems: 'center',
  },
  title: {
    fontSize: 36,
    fontWeight: '800',
    color: '#003d9b',
    letterSpacing: -0.5,
    marginBottom: 12,
  },
  subtitle: {
    fontSize: 16,
    color: '#5d5e61',
    textAlign: 'center',
    lineHeight: 22,
    maxWidth: 280,
  },
  bottomContainer: {
    width: '100%',
    alignItems: 'center',
    paddingBottom: 48,
  },
  progressBarBackground: {
    width: 192,
    height: 4,
    backgroundColor: '#e7e8ea',
    borderRadius: 999,
    overflow: 'hidden',
    marginBottom: 16,
  },
  progressBarFill: {
    width: '50%',
    height: '100%',
    backgroundColor: '#003d9b',
    borderRadius: 999,
  },
  statusRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  statusText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#434654',
    textTransform: 'uppercase',
    letterSpacing: 1,
  }
});