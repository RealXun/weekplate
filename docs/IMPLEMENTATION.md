# Implementación clean-room

## Arquitectura

El proyecto tiene tres módulos Gradle:

| Módulo | Responsabilidad |
|---|---|
| `:core` | modelos de dominio, generador semanal y agregador de compra, sin Android |
| `:app` | UI Jetpack Compose, estado, almacenamiento local y contenido de demostración original |
| `:backend` | servicio HTTP propio para hogares, códigos de invitación y revisiones optimistas |

La app usa un `WeekplateViewModel` como store observable y una máquina de destinos explícita. No depende de un SDK de navegación para mantener el binario pequeño y hacer visibles todas las transiciones.

## Funciones implementadas

- Onboarding de ocho pasos para España, con el ZIP estadounidense omitido según la condición observada.
- Selección de tienda genérica, presupuesto, personas, comidas por día, días, prioridades, equipos, dietas y exclusiones.
- Generación determinista de plan con restricciones duras de dieta/equipo/exclusiones y ponderación propia de prioridad/coste.
- Menú por días con coste, cambio, cocinado, omisión, adición y navegación persistida entre semana anterior, actual y siguiente.
- Descubrimiento por texto, prioridad, cocina y dieta, con filtros combinables.
- Favoritos persistentes.
- Detalle con escalado de ingredientes, nutrición y método.
- Modo cocina paso a paso, posición persistida y temporizador con alarma/notificación que continúa fuera de la app.
- Lista agregada por pasillo, marcado, artículos extra, estimación, combinación de dos semanas y compartir mediante Android.
- Preferencias, regeneración, planes completos persistidos por semana, historial real, recordatorio local y restablecimiento.
- Recordatorio semanal los domingos a las 10:00: ocho alarmas futuras, permiso de notificación, precisión cuando Android la autoriza, fallback no exacto y reprogramación tras reinicio/actualización.
- Cliente de hogar configurable para crear, unir, publicar y traer instantáneas del backend propio, con revisión optimista y validación local.
- Diseño compacto inferior y diseño ancho con pestañas superiores.

## Algoritmos propios

### Generador

1. Excluye recetas incompatibles con dietas, equipos, modo infantil o exclusiones; nunca relaja una restricción seleccionada como fallback.
2. Puntúa coincidencias de prioridades.
3. Añade una preferencia moderada por recetas económicas.
4. Introduce ruido determinista mediante semilla para variar sin hacer pruebas inestables.
5. Intenta mantener el coste bajo presupuesto; si no es posible, elige la alternativa compatible más barata y marca exceso.

Las dietas positivas (por ejemplo, vegana) requieren una etiqueta explícita en el catálogo propio. Las restricciones negativas se validan además contra los ingredientes propios revisados. No se afirma que este algoritmo sea el de Whipp.

### Lista de compra

1. Omite comidas marcadas como omitidas.
2. Escala cada ingrediente por raciones.
3. Omite básicos de despensa e ingredientes declarados disponibles.
4. Combina por ID y unidad.
5. Conserva pasillo, marcado y estimación.
6. Incorpora artículos libres como “Otros”.

## Persistencia

`SharedPreferences` guarda onboarding, preferencias, favoritos, recordatorio, planes completos por semana, checks, extras, historial, posición/temporizador de cocina y conexión del hogar. Se conservan hasta 24 semanas, distinguiendo la semana actual de las semanas navegadas. No se reutiliza el formato de almacenamiento privado de la referencia.

## Contenido y diseño

Las ocho recetas incluidas son originales. Las ilustraciones son composiciones geométricas generadas en Compose a partir del ID; no hay fotografías ni recursos de Whipp. La paleta, marca Weekplate, icono y textos son propios. Se reproduce la densidad funcional y la jerarquía general —tarjetas, presupuesto, navegación, pasos— sin una copia de marca o asset.

## Límites deliberados

- No se incluyó una clave Firebase, Google, Superwall, Play Billing ni analítica.
- No se conecta a dominios de Whipp.
- No se copiaron JSON de recetas, precios, ZIP, traducciones ni imágenes.
- No se intentó neutralizar Pairip/Play Integrity.
- No se presentan precios locales como datos vivos.
- El hogar exige una URL HTTPS administrada por el operador; en debug solo se admite además `http://10.0.2.2` para pruebas locales.
