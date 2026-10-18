# Weekplate — réplica funcional clean-room

Aplicación Android independiente que reproduce los flujos verificados de Whipp 1.4.5 sin reutilizar su código, identidad, recetas, imágenes, credenciales ni servicios privados.

La entrega actual incluye:

- app Kotlin + Jetpack Compose con onboarding, menú semanal, descubrimiento, favoritos, recetas, modo cocina, lista de compra, preferencias, historial y perfil;
- motor local determinista de planificación y agregación de ingredientes en `:core`;
- backend propio mínimo para hogares compartidos en `:backend`;
- catálogo de precios externo opcional, desacoplado y cacheado, generado sin login con `grocery-cli` y publicable como fichero estático;
- veintisiete pruebas automáticas definidas (veinte JVM y siete instrumentadas);
- APK de depuración instalable en `dist/weekplate-cleanroom-0.2.0-debug.apk`.

Documentación:

- [`docs/INVENTORY.md`](docs/INVENTORY.md): pantallas, navegación, estados, recursos y funciones de la referencia;
- [`docs/IMPLEMENTATION.md`](docs/IMPLEMENTATION.md): arquitectura y decisiones clean-room;
- [`docs/COMPARISON.md`](docs/COMPARISON.md): equivalencia alcanzada y diferencias pendientes;
- [`docs/BUILD_AND_TEST.md`](docs/BUILD_AND_TEST.md): compilación y pruebas reproducibles;
- [`docs/BACKEND.md`](docs/BACKEND.md): contrato del servicio propio.
- [`docs/PHASES.md`](docs/PHASES.md): fases completadas, evidencia y siguiente trabajo autorizado.
- [`docs/GROCERY_CATALOG.md`](docs/GROCERY_CATALOG.md): generación, publicación y límites del catálogo externo.

Los binarios de referencia, extracciones, capturas y salidas de decompilación permanecen en `reference-private/` y `analysis-private/`, excluidos del control de versiones.

