# Backend propio de hogar

El módulo `:backend` evita reutilizar Firebase, credenciales o endpoints privados. Usa únicamente el servidor HTTP del JDK y memoria local; es una base verificable, no un despliegue de producción.

## Ejecución

```powershell
.\gradlew.bat :backend:run
```

Escucha por defecto en `127.0.0.1:8080`; la variable `PORT` permite cambiar el puerto.

## Endpoints

| Método | Ruta | Resultado |
|---|---|---|
| `GET` | `/health` | estado del proceso |
| `POST` | `/v1/households` | crea código de seis caracteres y revisión 0 |
| `POST` | `/v1/households/{code}/join` | incrementa dispositivos |
| `GET` | `/v1/households/{code}` | devuelve instantánea |
| `PUT` | `/v1/households/{code}` | actualiza usando cabecera `If-Match` |

Una revisión incorrecta devuelve `409 revision_conflict`. El cuerpo está limitado a 256 KB.

## Requisitos antes de producción

- almacenamiento persistente y migraciones;
- HTTPS detrás de proxy confiable;
- token por dispositivo y rotación/revocación;
- códigos con caducidad, rate limiting y defensa contra enumeración;
- validación JSON y esquema versionado;
- cifrado, copias y borrado solicitado;
- telemetría propia consentida;
- autenticación por dispositivo y revocación en el cliente Android;
- cola offline y resolución asistida de conflictos más allá del rechazo optimista ya implementado.
