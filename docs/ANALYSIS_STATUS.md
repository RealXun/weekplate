# Estado del análisis de referencia

## Identidad exacta

- Archivo aportado: `Whipp (1.4.5).apks`.
- SHA-256 del APKS: `7F66A073671B36480D0DD7C74A3446C8A0B3D45BDD6E11919C7A5AE9D096D299`.
- Contenido: `base.apk`, `split_config.arm64_v8a.apk`, `split_config.es.apk` y `split_config.xxhdpi.apk`.
- SHA-256 de `base.apk`: `84BEAC2BEA401ACA99F480959B857A49A613CF01B8DE284CC3A1E2F1626F1D5D`.
- Nombre: Whipp: Meal Planner & Recipes.
- Application ID: `app.whipp`.
- Versión: `1.4.5` (`versionCode 14`).
- minSdk 32, targetSdk 36, compileSdk 37.
- Fecha de análisis: 2026-10-18.

## Etiquetas de evidencia

- **Comprobado-estático**: observado directamente en el APK exacto mediante REA/JADX o en sus recursos.
- **Comprobado-runtime**: reproducido en el emulador y registrado con pasos/captura.
- **Comprobado-réplica**: reproducido en Weekplate mediante prueba o captura REA.
- **Inferido**: deducido de relaciones, tipos o nombres, pero sin prueba directa suficiente.
- **Externo**: depende de red, proveedor, facturación, catálogo vivo o estado de cuenta.

## Límite de observación dinámica

Los cuatro splits se instalaron en un emulador Android 15. Al iniciar el APK de referencia, el envoltorio de Play Integrity/Pairip redirigió a Google Play para autenticación/licencia. La captura REA de ese resultado se conserva en `analysis-private/reference-runtime/launch/screen.png` (SHA-256 `751FFACACCA65F7B47A602DE124ED09F1D2649F90B7373ACD076F2DF1CBA8140`).

No se intentó eludir esa protección. Por ello, el inventario funcional de la referencia es principalmente estático y los resultados visuales de la réplica no se presentan como comparación píxel a píxel con una sesión autenticada de Whipp.

## Resultado de implementación

- APK clean-room compilado e instalado correctamente.
- Paquete: `com.cleanroom.weekplate`.
- Versión actual: `0.2.0-cleanroom` (`versionCode 2`).
- REA/JADX confirmó la entrega 0.1; la reinspección binaria de 0.2 queda pendiente y no se sustituye por una suposición. La compilación 0.2 y Android Lint terminaron correctamente.
- Pruebas actuales: 20 JVM aprobadas y 7 instrumentadas definidas; las 6 anteriores se aprobaron en emulador y el nuevo parser de catálogo está compilado pero pendiente de ejecución en dispositivo.
- Catálogo externo 0.2: feed HTTPS propio, caché local y WorkManager, sin credenciales ni servicios privados de Whipp.

