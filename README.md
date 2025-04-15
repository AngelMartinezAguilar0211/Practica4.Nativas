# Practica4.Nativas 📱

Aplicación Android nativa desarrollada en Kotlin que integra dos funcionalidades principales:

1. 🗂 **Gestor de Archivos Avanzado**
2. 🧠 **Juego de Memoria con Gestión de Archivos**

---

## 📦 Contenido del Proyecto

- `MainActivity.kt` - Pantalla inicial con acceso al gestor y al juego.
- `FileExplorerActivity.kt` - Explorador de archivos completo con temas, favoritos, recientes y operaciones.
- `MemoryGameMenuActivity.kt` - Menú intermedio para elegir entre iniciar o cargar una partida.
- `MemoryGameActivity.kt` - Lógica del juego de memoria.
- `SavedGamesActivity.kt` - Lista de partidas guardadas filtrables por tipo.
- `Room` - Persistencia de archivos favoritos y recientes.
- `Gson`, `XML`, `TXT` - Múltiples formatos de guardado de partida.

---

## 🗂 Gestor de Archivos

### Funcionalidades principales

- Explora almacenamiento interno y externo
- Vista jerárquica de carpetas y archivos
- Visualizador integrado para:
  - Archivos `.txt`, `.xml`, `.json`
  - Imágenes con zoom y rotación
- Metadatos del archivo: tamaño, tipo, fecha
- Iconos según tipo de archivo

### Gestión de archivos

- Copiar, mover, renombrar, eliminar
- Vista separada de:
  - Archivos recientes
  - Archivos favoritos

### Temas personalizables

- Tema Guinda (IPN)
- Tema Azul (ESCOM)
- Cambio de tema desde la interfaz

### Persistencia

- Favoritos y recientes almacenados con Room
- Soporte para modo claro y oscuro

---

## 🧠 Juego de Memoria

### Mecánicas del juego

- Juego de emparejamiento de emojis
- Niveles con dificultad progresiva automática
- Sistema de puntuación y contador de movimientos
- Temporizador en tiempo real
- Retroalimentación visual y sonora básica

### Gestión de partidas

- Guardado en tres formatos:
  - `.json` (usando Gson)
  - `.xml` (XML Pull Parser)
  - `.txt` (formato plano)
- Carga de partidas desde una lista de archivos
- Selector para elegir el formato de carga
- Diálogo interactivo para guardar con:
  - Nombre personalizado
  - Selección de formato

### Interfaz de Usuario

- Pantalla intermedia para elegir "Nueva Partida" o "Cargar Partida"
- Visualización de partidas guardadas
- Selector de formato al guardar
- Botón para regresar al menú desde el juego

---

## 💾 Almacenamiento

### ¿Dónde se guardan los archivos?

- Todos los archivos del juego se almacenan en:  
  `/data/data/com.example.practica4nativas/files/`

- Archivos creados desde el explorador pueden residir en:  
  `/storage/emulated/0/Documents/`

Esto ocurre porque el gestor de archivos puede acceder tanto al almacenamiento interno privado como a rutas públicas como la carpeta "Documents", mientras que el juego guarda en el espacio privado de la aplicación por seguridad y persistencia.

Ambas funcionalidades comparten contexto, por lo que puedes ver archivos de la primera parte (explorador) desde la segunda parte (juego) si se ubican en el mismo directorio o si el explorador interactúa con archivos dentro de `filesDir`.

---

## 🛠 Tecnologías Utilizadas

- **Kotlin**: Lenguaje principal
- **Android SDK**: Desarrollo nativo
- **Room**: Base de datos local para favoritos y recientes
- **Gson**: Serialización y deserialización JSON
- **XML Pull Parser**: Lectura eficiente de XML
- **RecyclerView**: Listado dinámico de archivos y partidas
- **Jetpack ViewBinding**: Manejo seguro de vistas

---

## 🚀 Ejecución y Pruebas

1. Abre el proyecto en Android Studio.
2. Ejecuta en un emulador (recomendado API 30+ con permisos de almacenamiento).
3. Desde `MainActivity`, selecciona:
   - 🗂 **Gestor de Archivos**: Explora carpetas, copia, mueve, guarda favoritos.
   - 🎮 **Juego de Memoria**: Accede al menú para comenzar o cargar una partida.
4. Prueba guardar partidas en distintos formatos (.json, .xml, .txt).
5. Verifica la restauración de estado desde archivos listados.
6. Cambia de tema desde el botón superior para probar personalización.

