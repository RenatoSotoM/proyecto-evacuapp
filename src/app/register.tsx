import { registerUser } from '../services/auth';

// Dentro de tu función de registro:
const handleRegisterPress = async () => {
  try {
    const user = await registerUser(name, email, password, phone);
    // Navegar a la pantalla principal o perfil
  } catch (error) {
    // Manejar error visual
  }
};