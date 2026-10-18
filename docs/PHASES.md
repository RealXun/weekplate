# Fases del trabajo

## Estado de entrega

| Fase | Resultado | Estado |
|---|---|---|
| 0. Custodia y límites | Huellas del APKS/base, material privado excluido y reglas clean-room | Completa |
| 1. Inventario estático | Pantallas, navegación, estados, modelos, recursos, intents y dependencias con REA/JADX | Completa para el APK aportado |
| 2. Observación dinámica de referencia | Instalación por splits; bloqueo legítimo por Pairip/Play Integrity documentado | Parcial, sin eludir protección |
| 3. Núcleo independiente | Modelos, planificador, compra, planes por semana, historial, cocina persistente y Compose propio | Completa para el alcance local |
| 4. Servicios propios | Backend y cliente Android propios con códigos e historial de revisión optimista | Funcional para pruebas; seguridad de producción pendiente |
| 5. Recordatorios | Domingo 10:00, ocho semanas, reinicio y permiso Android | Completa localmente |
| 6. QA y APK | 20 pruebas JVM aprobadas, 7 instrumentadas definidas, Lint sin errores y APK 0.2 compilada | Completa para debug; nuevo test instrumental pendiente de ejecución |
| 7. Comparación visual | Capturas de onboarding, menú y lista de Weekplate | Parcial: referencia no autenticada |
| 8. Precios externos | contrato JSON, 12 cadenas ES, caché, refresco manual/24 h y generador GitHub Pages con `grocery-cli` | Implementación completa; despliegue y validación de datos reales pendientes |

## Próxima fase con mayor valor

La siguiente fase debería desplegar el feed, ejecutar una captura real por cadena y probar la sincronización en un dispositivo/emulador. Después corresponde resolver cantidades/envases, endurecer el hogar propio y ampliar catálogo visual solo con material licenciado. La validación píxel a píxel contra Whipp requiere acceso legítimo a una instalación autenticada; no se intentará neutralizar Pairip ni Play Integrity.

## Condiciones para declarar más equivalencia

- Una conducta solo pasa de inferida a comprobada cuando existe evidencia estática inequívoca o una reproducción dinámica registrable.
- Los precios externos solo se consideran comprobados para una tienda y fecha después de validar la instantánea publicada; cesta y suscripción siguen fuera de alcance.
- Cualquier recurso visual o receta adicional debe ser original, generado para Weekplate o contar con licencia explícita.
- La variante release requiere identidad de publicación, firma, política de privacidad y pruebas en dispositivo físico.
