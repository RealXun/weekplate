# Catálogo externo de supermercados (fase 1)

## Qué está implementado

- La app acepta un feed JSON público por HTTPS y conserva la última copia válida para uso sin conexión.
- WorkManager comprueba el feed aproximadamente cada 24 horas cuando hay red y batería suficiente. Android puede retrasar la ejecución; no es un reloj exacto.
- La lista de compra cruza cada ingrediente por nombre exacto normalizado y por la tienda seleccionada. No sustituye un artículo si no hay coincidencia.
- El importe externo representa **una unidad del producto encontrado**. No se interpreta como cantidad exacta de la receta ni como total de caja.
- El workflow `update-grocery-catalog.yml` instala `grocery-cli`, consulta sin login y publica un fichero estático mediante GitHub Pages.

## Activación sin servidor propio

1. Sube este proyecto a un repositorio de GitHub.
2. En `Settings → Pages`, selecciona **GitHub Actions** como origen.
3. Ejecuta manualmente `Update public grocery catalog` una vez. Después se programa a diario a las 04:17 UTC.
4. Abre la lista de compra de la APK e introduce:

   `https://USUARIO.github.io/REPOSITORIO/catalog/latest.json`

5. Pulsa `Guardar y actualizar`. La URL queda local en el dispositivo.

## Cadenas incluidas

El generador usa las claves públicas verificadas en el registro de `grocery-cli`: Mercadona, DIA, Lidl España, Alcampo, Consum, Eroski, Bonpreu/Esclat, Ametller Origen, Gadis, ALDI España, Makro España y masymas.

## Límites y seguridad

- `grocery-cli` es un proyecto independiente con licencia MIT; Weekplate no copia sus adaptadores dentro del APK, solo consume una instantánea normalizada generada por la herramienta.
- No se usan ni almacenan credenciales, cookies, login o carrito.
- Los precios, disponibilidad y cobertura dependen de fuentes externas y pueden variar por zona, tienda o momento.
- Una cadena puede fallar sin inutilizar las demás. El feed marca cada resultado como `ok`, `partial` o `error`.
- Antes de distribuir o monetizar, hay que revisar las condiciones de uso y robots de cada comercio y ajustar la frecuencia si procede.

## Contrato JSON

La versión actual es `schemaVersion: 1`. Cada tienda contiene consultas con un producto opcional. Los campos aceptados del producto son: `id`, `name`, `price`, `pricePerUnit`, `unit`, `currency`, `brand`, `category`, `available` y `url`. La app rechaza esquemas desconocidos, precios no válidos, ficheros mayores de 8 MB y URLs que no sean HTTPS.
