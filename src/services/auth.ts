import { loginUser } from '../services/auth';

// Dentro de tu función de botón:
const handleLoginPress = async () => {
  try {
    const user = await loginUser(email, password);
    // Navegar a la pantalla principal (ej. router.replace('/home'))
  } catch (error) {
    // Manejar error visual en pantalla
  }
};