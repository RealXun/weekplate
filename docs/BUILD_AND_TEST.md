# Compilación y pruebas

## Toolchain validado

- Gradle 8.13 (wrapper incluido).
- Android Gradle Plugin 8.13.2.
- Kotlin 2.2.10 y plugin Compose.
- JDK Temurin 21 ejecutando bytecode objetivo Java 17.
- compileSdk/targetSdk 36, minSdk 32.
- Compose UI 1.8.3 y Material 3 1.3.2.
- AVD Android 15/API 35, x86_64, 1080×2400.

La compilación 0.2 se validó desde la raíz del repositorio mediante una copia temporal clean-room montada en la imagen pública `ghcr.io/cirruslabs/android-sdk:36`, fijada por digest. Las carpetas `reference-private`, `analysis-private`, `.git` y cualquier credencial quedaron fuera del montaje.

## Comandos

Definir `JAVA_HOME` y `ANDROID_SDK_ROOT` y ejecutar:

```powershell
.\gradlew.bat :core:test :backend:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

En este equipo se usó además:

```text
-Dkotlin.compiler.execution.strategy=in-process
```

## Pruebas aprobadas

| Suite | Casos | Resultado |
|---|---:|---|
| `PlanEngineTest` | 6 | aprobado |
| `RecipeSearchTest` | 3 | aprobado |
| `HouseholdRepositoryTest` + `HouseholdServerHttpTest` | 4 | aprobado |
| `CookingSessionTest` + `HouseholdClientTest` | 3 | aprobado |
| `CatalogMatcherTest` + `CatalogUrlPolicyTest` | 4 | aprobado |
| `OnboardingSmokeTest` | 6 | aprobado en emulador |
| `CatalogJsonTest` | 1 | compilado; ejecución pendiente en emulador/dispositivo |
| Total | 27 | 20 JVM aprobadas; 6 instrumentadas aprobadas previamente; 1 instrumental pendiente |

Cobertura de comportamiento:

- plan determinista y filtros duros;
- lista escalada/agregada y exclusión de ingredientes disponibles;
- rechazo de días vacíos;
- exclusión conservadora de ingredientes incompatibles con una dieta;
- cálculo del siguiente domingo a las 10:00, incluido el límite exacto;
- actualización optimista y conflicto de revisión del hogar;
- unión de dispositivos;
- búsqueda y filtros combinados por cocina, dieta y prioridad;
- combinación y agregación de ingredientes de dos semanas;
- persistencia de sesión/temporizador de cocina;
- primer paso de onboarding;
- recorrido completo hasta menú y acceso a la lista;
- generación, persistencia y retorno desde la semana siguiente.
- coincidencia conservadora por tienda/nombre y rechazo de URLs inseguras para el catálogo externo.

## Artefacto

- Ruta: `dist/weekplate-cleanroom-0.2.0-debug.apk`
- Tamaño: 28.494.302 bytes.
- SHA-256: `1D7A62F2A8CF971D774E9DC317FF45C23336807D84E0C024640274DB0CA8BE52`.
- Firma: debug de Android; no apta para publicación.

`apkanalyzer` confirmó directamente en la APK 0.2 el paquete `com.cleanroom.weekplate`, `versionCode 2`, versión `0.2.0-cleanroom`, minSdk 32 y targetSdk 36. Sus permisos efectivos son Internet, estado de red, notificaciones, reinicio, alarma exacta, wake lock, foreground service y el permiso interno no exportado de AndroidX. No aparecen facturación, Firebase, Google Sign-In ni Pairip. La inspección REA/JADX citada en la entrega anterior corresponde a la versión 0.1; la reinspección completa de clases/recursos de 0.2 sigue pendiente.

## Advertencias no bloqueantes

- La combinación de AGP 8.13.2 y command-line tools actuales emite una advertencia de versión XML del SDK; el build y las pruebas terminan correctamente.
- El empaquetador conserva `libandroidx.graphics.path.so` sin strip en debug.
- Android Lint termina sin errores y mantiene 31 advertencias no bloqueantes, principalmente actualizaciones de dependencias, orientación fija heredada y sugerencias KTX.
- El APK de depuración incluye componentes de tooling/perfil AndroidX; para distribución se debe generar una variante release firmada.
