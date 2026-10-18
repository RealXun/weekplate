# Comparación funcional y diferencias pendientes

## Método

La referencia se comparó mediante inventario estático REA/JADX, recursos y un intento de ejecución. El intento dinámico quedó en la pantalla de autenticación de Google Play por Pairip/Play Integrity; no se sorteó. Weekplate sí se ejecutó en un AVD Android 15 de 1080×2400 y se inspeccionó con REA/ADB.

Por este límite, “equivalente” significa que el flujo y el estado verificados existen y funcionan, no que la interfaz sea píxel a píxel idéntica.

## Matriz

| Área | Referencia | Weekplate 0.2 | Estado |
|---|---|---|---|
| Estado raíz | onboarding/loading/gate/main | onboarding/main; carga inmediata | Parcial |
| Onboarding | 9 pasos, 2 condicionales | 8 pasos España; ZIP no mostrado | Equivalente para ES, sin paywall |
| Validación onboarding | tienda/días/prioridades/equipo y ZIP US | tienda/días/prioridades/equipo | Equivalente para ES |
| Plan semanal | generación, semanas, coste, cambio, cocinado, omisión | anterior/actual/siguiente, coste, regenerar, cambiar, cocinar, omitir, añadir | Alta |
| Algoritmo de plan | propietario/no probado | heurística propia documentada | Diferente |
| Descubrir | búsqueda, cocinas, filtros | texto, prioridad, cocina y dieta combinables | Alta para el catálogo local |
| Favoritos | persistente | persistente | Alta |
| Receta | detalle, raciones, nutrición, método | implementado | Alta |
| Catálogo | 948 recetas y fotos | 8 recetas originales, arte geométrico | Deliberadamente diferente |
| Modo cocina | pasos, alarmas persistentes, notificación | pasos, posición persistida y alarma/notificación fuera de la app | Alta local |
| Lista | agregación, pasillos, check, extras, precio, combinar, compartir | flujo local completo y coincidencias de precio externas opcionales; sin compra online | Alta local; precio externo parcial |
| Historial | persistido | planes reales persistidos por semana, hasta 24 | Alta local |
| Preferencias | modelo amplio | campos principales | Alta |
| Recordatorio | domingo 10:00, 8 avisos, receptor/boot | domingo 10:00, 8 alarmas, receptor/boot y permiso | Alta local |
| Hogar | Firebase y código/dispositivos | backend y cliente propios configurables, revisión optimista | Funcional para pruebas; endurecimiento pendiente |
| Suscripción | Play Billing + Superwall | no incluida | Externo/no reproducido |
| Precios y tienda online | catálogo vivo | feed estático propio generado con `grocery-cli`, caché offline y actualización WorkManager; sin carrito | Parcial y dependiente de terceros |
| Deep links/notificaciones | acciones privadas específicas | recordatorio con acción y namespace propios; sin compatibilidad privada | Parcial deliberado |
| Tableta | pestañas superiores | pestañas superiores desde 700 dp | Alta estructural |
| Español | 626 entradas | UI española propia | Alta en superficie implementada |

## Comparación visual observada

Capturas REA de Weekplate:

| Pantalla | SHA-256 | Observación |
|---|---|---|
| Onboarding | `4091051EA70E6EF10E4BD946D2A836BC16BBD18730A6D6FE0C020DCF577E6598` | jerarquía, progreso, opciones y CTA visibles sin recorte |
| Menú | `BD51E6F96849D55591DA814FEE604E9D3624685145C65DA77E220A0F30875885` | presupuesto, acciones, comidas y navegación inferior visibles |
| Lista | `5F0B3A1B0DFCEC45E416429288D5E0D7A77CF37471A20C073921A436D1DD5031` | resumen, alta de artículo, pasillo, cantidad, check y precio visibles |

Las capturas están en `analysis-private/replica-runtime/` y no se distribuyen como assets de la app.

## Diferencias prioritarias para una fase siguiente

1. Desplegar el feed estático en una cuenta controlada por el proyecto y validar precios por cadena, zona y fecha con una muestra manual reproducible.
2. Resolver cantidades/envases sin asumir que una coincidencia equivale al coste exacto de la receta; mientras tanto la UI muestra explícitamente “1 producto”.
3. Endurecer el hogar propio con autenticación por dispositivo, TLS desplegado, caducidad/rate limiting, almacenamiento duradero y pruebas de conflicto extremo a extremo.
4. Ampliar el catálogo solo con recetas/imágenes licenciadas o creadas para el proyecto.
5. Añadir filtros verificados adicionales únicamente si la referencia o una sesión legítima permiten confirmarlos.
6. Validar visualmente contra una instalación legítima y autenticada de la referencia; hasta entonces no se puede afirmar equivalencia visual exacta.
7. Preparar firma release, política de privacidad, accesibilidad formal, localización inglesa y pruebas de dispositivo físico.

## Criterio de no invención

Las funciones sin evidencia suficiente no se simulan como completas. El hogar exige un servidor propio configurado; la lista distingue estimaciones locales de precios externos, informa de su fecha y no llama “total real” a una suma incompleta. No existe paywall ficticio. Temporizador y recordatorio usan textos, almacenamiento y acciones propios, no contratos privados de Whipp.
