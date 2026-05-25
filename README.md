# 👾 Mascota Virtual (Mochi) — Simulador y Cuidado Dinámico

¡Bienvenido a **Mascota Virtual**, una aplicación interactiva de última generación desarrollada para Android con **Kotlin** y **Jetpack Compose**! Cuida a tu adorable compañero **Mochi**, ayúdalo a crecer, toma decisiones críticas para guiar su evolución única y personaliza por completo su apariencia.

Esta aplicación cuenta con persistencia robusta fuera de línea impulsada por **Room Database**, arquitectura modular **MVVM** y un diseño retro-futurista de consola portátil optimizado con Material Design 3.

---

## 🎨 Características Clave

### 1. Ciclos Biológicos Reales e Integración Fuera de Línea
*   **Gestión de Necesidades**: Rastrea en tiempo real el nivel de **Nutrición (Hambre)**, **Energía (Sueño)** y **Felicidad** general.
*   **Simulación de Decaimiento Off-line**: Mochi sigue viviendo incluso cuando cierras la aplicación. Se calculan las tasas de decaimiento natural y recuperación en base al tiempo exacto transcurrido entre sesiones.

### 2. Animaciones y Gestos de Expresión Únicos (`PetCanvas`)
*   Se desarrolló un motor gráfico a medida sobre el lienzo nativo (`Canvas`) de Compose para dibujar a la mascota con fluidez:
    *   **Animación de Respiración**: Movimientos sutiles continuos de compresión y expansión horizontal/vertical.
    *   **Sistemas de Partículas**: Generación de burbujas flotantes brillantes (al bañar), corazones (al acariciar), migajas interactivas (al comer) y ondas Zzz (mientras duerme).
    *   **Parpadeo y Expresiones Reactivas**: Mochi parpadea naturalmente y cambia por completo sus ojos, mejillas y boca para expresar estados de *hambre*, *sueño*, *tristeza* o *alegría extrema*.

### 3. Sistema de Evolución Dinámica Ramificada
*   Al ganar suficiente experiencia (EXP) mediante cuidados diarios o minijuegos, Mochi subirá de nivel.
*   En hitos de nivel clave (Niveles 3, 7 y 12), se activa una **Elección de Sendero de Evolución**:
    *   **Fuego 🔥** (Fugaz Ígneo, Fénix Errante, Dragón del Sol Divino)
    *   **Agua 💧** (Gota Cristalina, Tritón Sabio, Leviatán de los Abisales)
    *   **Naturaleza 🌿** (Retoño Brote, Guardián del Bosque, Espíritu Eterno de Gaia)
    *   **Cosmos 🌌** (Polvillo de Estrellas, Estela Estelar, Arcano del Vacío Celestial)

### 4. Boutique de Personalización y Logros (`Shop`)
*   **Ganar Monedas (g)**: Por cada mímica de cuidado (alimentar, cepillar, acariciar) o éxito en actividades obtienes monedas de recompensa.
*   **La Tienda de Accesorios**: Desbloquea y equipa a tu mascota:
    *   *Skins de Piel*: Cielo Azul, Menta Fresca, Melocotón Suave, Oro Celestial, Violeta Mochi.
    *   *Accesorios Tactiles*: Gafas de Sol Cool 😎, Gorro de Fiesta 🎉, Sombrero de Mago 🧙‍♂️, Corona Real 👑, Pajarita Elegante 🎀.

### 5. Minijuegos Interactivos: "Caja Misteriosa 📦"
*   Pon a prueba tu suerte con el minijuego de cajas sorpresa para encontrar el cofre de oro. ¡Diversión garantizada que incrementa drásticamente la felicidad y otorga jugosas cantidades de monedas y experiencia!

### 6. Sistema de Alertas y Notificaciones Inteligentes
*   **Notificaciones Locales**: Recordatorios inteligentes del sistema cuando Mochi tiene hambre extrema o está exhausto y necesita dormir.
*   **Historial de Alertas in-app**: Un buzón centralizado interactivo que archiva todas las aventuras de Mochi, hitos de nivel y desbloqueos cosméticos con posibilidad de archivado general.

---

## 🛠️ Stack Tecnológico

*   **Lenguaje**: [Kotlin](https://kotlinlang.org/) — 100% interoperable, seguro y expresivo.
*   **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) — Interfaz declarativa de alto rendimiento con Material Design 3.
*   **Base de Datos Local**: [Room (SQLite)](https://developer.android.com/training/data-storage/room) — Almacenamiento seguro, transaccional y reactivo mediante flujos asíncronos (`Flow`).
*   **Arquitectura**: **MVVM (Model-View-ViewModel)** — Separación limpia de responsabilidades que garantiza código testeable y mantenible.
*   **Gestión del Estado**: `StateFlow` y `collectAsStateWithLifecycle` para reaccionar inmediatamente a los cambios de estado en pantalla.
*   **Construcción**: Gradle (Kotlin DSL con Version Catalogs para estandarización de dependencias).

---

## 🚀 Cómo Empezar / Instalación

Para compilar y ejecutar este proyecto de forma local en Android Studio:

1.  **Clonar este repositorio**:
    ```bash
    git clone https://github.com/TU_USUARIO/mascota-virtual-mochi.git
    cd mascota-virtual-mochi
    ```
2.  **Abrir en Android Studio**:
    *   Selecciona *File > Open* y elige la carpeta raíz del repositorio clonado.
    *   Permite que la sincronización de Gradle finalice con éxito.
3.  **Ejecutar en un Dispositivo o Emulador**:
    *   Haz clic en el botón verde de **Run (Ejecutar)** en la parte superior para instalar el APK en tu emulador o dispositivo Android físico (mínimo SDK 24 / Android 7+).

---

## 🕹️ Mecánicas de Cuidado y Atributos

| Acción | Efecto Principal | Monedas Recibidas | EXP Recibida |
| :--- | :--- | :---: | :---: |
| **Acariciar ❤️** | Recupere Felicidad (+15 puntos) | +5 g | +5 EXP |
| **Alimentar 🍪** | Sacia el Hambre de Mochi (+25 puntos) | +8 g | +10 EXP |
| **Dormir 💤** | Alterna luces de sueño para restaurar Energía de Mochi | Variable | - |
| **Bañar 🛁** | Limpia a fondo e incrementa la Felicidad (+10 puntos) | +15 g | +8 EXP |
| **Caja Misteriosa 🎮** | Encuentra el cofre del tesoro para ganar a lo grande | +8g o +30 g | +5 a +20 EXP |

---

## 📐 Detalles de Diseño e Interfaz
El diseño de la aplicación emula la calidez envolvente de una **consola portátil retro-futurista**:
*   **Tema de Colores Ambientales**: Fondos degradados profundos usando tonos Slate y Midnight de alto contraste que protegen la vista.
*   **Píldoras de Estado y Feedback Táctil**: Cada botón de acción de cuidado cuenta con una mini-insignia, un área de impacto ergonómica de al menos `48dp` (accesibilidad AAA) e incentiva sensaciones agradables mediante colores vibrantes y transiciones fluidas.
