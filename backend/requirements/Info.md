# (Si usas Python) o package.json (si usas Node)


Dependencias aisladas: Nunca mezcles los paquetes de Node.js del frontend con los del backend. Si usas Node/Express para el backend, inicializa un nuevo npm init dentro de la carpeta backend/. Si usas Python con FastAPI, gestiona un entorno virtual (venv) también dentro de backend/.

Control de versiones: El archivo .gitignore general en la raíz excluirá carpetas pesadas como node_modules tanto del frontend como del backend si configuras adecuadamente los entornos.